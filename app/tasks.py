import os
import httpx
from celery import shared_task
from django.conf import settings
from django.core.cache import cache
from .models import VideoHistory


AI_PROVIDERS = [
    {
        'name': 'groq',
        'url': 'https://api.groq.com/openai/v1/chat/completions',
        'key_setting': 'GROQ_API_KEY',
        'model': 'llama-3.3-70b-versatile'
    },
    {
        'name': 'groq-gemma',
        'url': 'https://api.groq.com/openai/v1/chat/completions',
        'key_setting': 'GROQ_API_KEY',
        'model': 'gemma2-9b-it'
    },
    {
        'name': 'deepseek',
        'url': 'https://api.deepseek.com/v1/chat/completions',
        'key_setting': 'DEEPSEEK_API_KEY',
        'model': 'deepseek-chat'
    },
    {
        'name': 'openrouter',
        'url': 'https://openrouter.ai/api/v1/chat/completions',
        'key_setting': 'OPENROUTER_API_KEY',
        'model': 'mistralai/mistral-7b-instruct'
    },
]

LANGUAGE_NAMES = {
    'en': 'English',
    'ru': 'Russian',
    'tj': 'Tajik',
}


def ask_ai(text: str, language: str) -> tuple[str, str]:
    lang_name = LANGUAGE_NAMES.get(language, 'English')
    prompt = f"""You are an assistant that explains video content in simple language.
User language: {lang_name}

Video transcript:
{text}

Tasks:
1. Translate content to {lang_name} if needed
2. Explain the main topic in simple words
3. List 3-5 key points
4. Write a short summary

Reply ONLY in {lang_name}."""

    for provider in AI_PROVIDERS:
        api_key = getattr(settings, provider['key_setting'], None)
        if not api_key:
            continue
        try:
            response = httpx.post(
                provider['url'],
                headers={
                    'Authorization': f'Bearer {api_key}',
                    'Content-Type': 'application/json'
                },
                json={
                    'model': provider['model'],
                    'messages': [{'role': 'user', 'content': prompt}],
                    'max_tokens': 2000
                },
                timeout=30.0
            )
            if response.status_code == 200:
                return response.json()['choices'][0]['message']['content'], provider['name']
        except Exception:
            continue

    return 'All AI providers are currently unavailable. Please try again later.', 'none'


def transcribe_audio(audio_path: str) -> str:
    from faster_whisper import WhisperModel
    model = WhisperModel('small', device='cpu', compute_type='int8')
    segments, _ = model.transcribe(audio_path, language=None)
    return ' '.join([seg.text for seg in segments]).strip()


def download_audio(url: str, output_path: str) -> str:
    import yt_dlp
    ydl_opts = {
        'format': 'bestaudio/best',
        'outtmpl': output_path,
        'postprocessors': [{'key': 'FFmpegExtractAudio', 'preferredcodec': 'mp3'}],
        'quiet': True,
        'cookiefile': 'cookies.txt',
    }
    with yt_dlp.YoutubeDL(ydl_opts) as ydl:
        info = ydl.extract_info(url, download=True)
        return info.get('title', 'Unknown title')


@shared_task(bind=True)
def process_video_task(self, video_id: int):
    try:
        video = VideoHistory.objects.get(id=video_id)
        video.status = 'processing'
        video.save()

        audio_path = f'/tmp/deeplink_{video_id}'

        self.update_state(state='PROGRESS', meta={'step': 'Downloading audio...'})
        title = download_audio(video.url, audio_path)
        video.title = title
        video.save()

        self.update_state(state='PROGRESS', meta={'step': 'Transcribing audio...'})
        transcript = transcribe_audio(f'{audio_path}.mp3')
        video.transcript = transcript
        video.save()

        self.update_state(state='PROGRESS', meta={'step': 'Generating AI explanation...'})
        explanation, provider = ask_ai(transcript, video.language)
        video.explanation = explanation
        video.ai_provider_used = provider
        video.status = 'done'
        video.save()
        cache.delete(f'video_history_{video.user_id}')

        if os.path.exists(f'{audio_path}.mp3'):
            os.remove(f'{audio_path}.mp3')

        return {'status': 'done', 'video_id': video_id}

    except Exception as e:
        VideoHistory.objects.filter(id=video_id).update(status='failed')
        raise self.retry(exc=e, countdown=5, max_retries=2)


@shared_task(bind=True)
def process_file_task(self, video_id: int, file_path: str, file_extension: str):
    from .file_processor import extract_text
    try:
        video = VideoHistory.objects.get(id=video_id)
        video.status = 'processing'
        video.save()

        self.update_state(state='PROGRESS', meta={'step': 'Extracting text from file...'})
        transcript = extract_text(file_path, file_extension)

        if not transcript:
            video.status = 'failed'
            video.save()
            return {'status': 'failed', 'error': 'No text found in file'}

        video.transcript = transcript
        video.save()

        self.update_state(state='PROGRESS', meta={'step': 'Generating AI explanation...'})
        explanation, provider = ask_ai(transcript, video.language)
        video.explanation = explanation
        video.ai_provider_used = provider
        video.status = 'done'
        video.save()
        cache.delete(f'video_history_{video.user_id}')

        if os.path.exists(file_path):
            os.remove(file_path)

        return {'status': 'done', 'video_id': video_id}

    except Exception as e:
        VideoHistory.objects.filter(id=video_id).update(status='failed')
        if os.path.exists(file_path):
            os.remove(file_path)
        raise self.retry(exc=e, countdown=5, max_retries=2)