from rest_framework import status, viewsets
from rest_framework.decorators import action
from rest_framework.response import Response
from rest_framework.permissions import IsAuthenticated, AllowAny
from rest_framework.parsers import JSONParser, MultiPartParser, FormParser
from rest_framework_simplejwt.tokens import RefreshToken
from django.contrib.auth import authenticate
from django.core.mail import send_mail
from django.conf import settings
from django.core.cache import cache
from celery.result import AsyncResult
from drf_spectacular.utils import extend_schema, OpenApiParameter
import os
import requests as req

from .models import User, EmailVerificationToken, VideoHistory
from .serializers import (
    RegisterSerializer, UserSerializer, ChangePasswordSerializer,
    VideoHistorySerializer, LoginSerializer, FileUploadSerializer
)
from .tasks import process_video_task, process_file_task
from .permissions import IsEmailVerified, IsOwnerOrAdmin
from .pagination import StandardPagination


class AuthViewSet(viewsets.ViewSet):

    def get_permissions(self):
        if self.action in ['register', 'login', 'verify_email', 'google_login']:
            return [AllowAny()]
        return [IsAuthenticated()]

    @extend_schema(
        request=RegisterSerializer,
        responses={201: {'type': 'object', 'properties': {'message': {'type': 'string'}}}}
    )
    @action(detail=False, methods=['post'], url_path='register', parser_classes=[JSONParser])
    def register(self, request):
        serializer = RegisterSerializer(data=request.data)
        if not serializer.is_valid():
            return Response(serializer.errors, status=status.HTTP_400_BAD_REQUEST)
        user = serializer.save()
        token = EmailVerificationToken.objects.create(user=user)
        send_mail(
            subject='Your DeepLink verification code',
            message=f'Your verification code: {token.code}',
            from_email=settings.DEFAULT_FROM_EMAIL,
            recipient_list=[user.email],
            fail_silently=False,
        )
        return Response({'message': 'Registration successful. Check your email for the 6-digit code.'}, status=status.HTTP_201_CREATED)

    @extend_schema(
        request={'application/json': {
            'type': 'object',
            'properties': {
                'email': {'type': 'string'},
                'code': {'type': 'string', 'example': '123456'}
            },
            'required': ['email', 'code']
        }},
        responses={200: {'type': 'object', 'properties': {'message': {'type': 'string'}}}}
    )
    @action(detail=False, methods=['post'], url_path='verify-email', parser_classes=[JSONParser])
    def verify_email(self, request):
        email = request.data.get('email')
        code = request.data.get('code')
        if not email or not code:
            return Response({'error': 'email and code are required'}, status=status.HTTP_400_BAD_REQUEST)
        try:
            user = User.objects.get(email=email)
            verification = EmailVerificationToken.objects.filter(
                user=user, code=code, is_used=False
            ).latest('created_at')
        except (User.DoesNotExist, EmailVerificationToken.DoesNotExist):
            return Response({'error': 'Invalid code or email'}, status=status.HTTP_400_BAD_REQUEST)
        verification.user.is_email_verified = True
        verification.user.is_active = True
        verification.user.save()
        verification.is_used = True
        verification.save()
        return Response({'message': 'Email verified successfully. You can now log in.'})

    @extend_schema(
        request=LoginSerializer,
        responses={200: UserSerializer}
    )
    @action(detail=False, methods=['post'], url_path='login', parser_classes=[JSONParser])
    def login(self, request):
        serializer = LoginSerializer(data=request.data)
        if not serializer.is_valid():
            return Response(serializer.errors, status=status.HTTP_400_BAD_REQUEST)
        user = authenticate(
            request,
            username=serializer.validated_data['email'],
            password=serializer.validated_data['password']
        )
        if not user:
            return Response({'error': 'Invalid credentials'}, status=status.HTTP_401_UNAUTHORIZED)
        if not user.is_email_verified:
            return Response({'error': 'Email not verified'}, status=status.HTTP_403_FORBIDDEN)
        refresh = RefreshToken.for_user(user)
        return Response({
            'access': str(refresh.access_token),
            'refresh': str(refresh),
            'user': UserSerializer(user).data
        })

    @extend_schema(
        request={'application/json': {
            'type': 'object',
            'properties': {'refresh': {'type': 'string'}},
            'required': ['refresh']
        }},
        responses={200: {'type': 'object', 'properties': {'message': {'type': 'string'}}}}
    )
    @action(detail=False, methods=['post'], url_path='logout', parser_classes=[JSONParser])
    def logout(self, request):
        try:
            token = RefreshToken(request.data.get('refresh'))
            token.blacklist()
            return Response({'message': 'Logged out successfully'})
        except Exception:
            return Response({'error': 'Invalid token'}, status=status.HTTP_400_BAD_REQUEST)

    @extend_schema(responses={200: UserSerializer})
    @action(detail=False, methods=['get', 'patch'], url_path='profile', parser_classes=[JSONParser])
    def profile(self, request):
        if request.method == 'GET':
            return Response(UserSerializer(request.user).data)
        serializer = UserSerializer(request.user, data=request.data, partial=True)
        if not serializer.is_valid():
            return Response(serializer.errors, status=status.HTTP_400_BAD_REQUEST)
        serializer.save()
        return Response(serializer.data)

    @extend_schema(
        request=ChangePasswordSerializer,
        responses={200: {'type': 'object', 'properties': {'message': {'type': 'string'}}}}
    )
    @action(detail=False, methods=['post'], url_path='change-password', parser_classes=[JSONParser])
    def change_password(self, request):
        serializer = ChangePasswordSerializer(data=request.data, context={'request': request})
        if not serializer.is_valid():
            return Response(serializer.errors, status=status.HTTP_400_BAD_REQUEST)
        serializer.save()
        return Response({'message': 'Password changed successfully'})

    @extend_schema(
        request={'application/json': {
            'type': 'object',
            'properties': {'access_token': {'type': 'string', 'example': 'google_oauth_access_token'}},
            'required': ['access_token']
        }},
        responses={200: UserSerializer}
    )
    @action(detail=False, methods=['post'], url_path='google', parser_classes=[JSONParser])
    def google_login(self, request):
        access_token = request.data.get('access_token')
        if not access_token:
            return Response({'error': 'access_token is required'}, status=status.HTTP_400_BAD_REQUEST)
        google_response = req.get(
            'https://www.googleapis.com/oauth2/v3/userinfo',
            headers={'Authorization': f'Bearer {access_token}'}
        )
        if google_response.status_code != 200:
            return Response({'error': 'Invalid Google token'}, status=status.HTTP_401_UNAUTHORIZED)
        google_data = google_response.json()
        email = google_data.get('email')
        if not email:
            return Response({'error': 'Email not provided by Google'}, status=status.HTTP_400_BAD_REQUEST)
        user, created = User.objects.get_or_create(
            email=email,
            defaults={
                'username': email.split('@')[0],
                'is_email_verified': True,
                'is_active': True
            }
        )
        if created:
            user.set_unusable_password()
            user.save()
        refresh = RefreshToken.for_user(user)
        return Response({
            'access': str(refresh.access_token),
            'refresh': str(refresh),
            'user': UserSerializer(user).data,
            'created': created
        })


class VideoViewSet(viewsets.ViewSet):

    def get_permissions(self):
        if self.action in ['retrieve', 'destroy']:
            return [IsAuthenticated(), IsEmailVerified(), IsOwnerOrAdmin()]
        return [IsAuthenticated(), IsEmailVerified()]

    @extend_schema(
        request=FileUploadSerializer,
        responses={202: {'type': 'object', 'properties': {
            'task_id': {'type': 'string'},
            'video_id': {'type': 'integer'},
            'message': {'type': 'string'}
        }}}
    )
    @action(detail=False, methods=['post'], url_path='process-file', parser_classes=[MultiPartParser, FormParser])
    def process_file(self, request):
        serializer = FileUploadSerializer(data=request.data)
        if not serializer.is_valid():
            return Response(serializer.errors, status=status.HTTP_400_BAD_REQUEST)
        file = serializer.validated_data['file']
        language = serializer.validated_data['language']
        ext = os.path.splitext(file.name)[1].lower()
        video = VideoHistory.objects.create(
            user=request.user,
            url=f'file://{file.name}',
            title=file.name,
            language=language,
            status='pending'
        )
        file_path = f'/tmp/deeplink_file_{video.id}{ext}'
        with open(file_path, 'wb') as f:
            for chunk in file.chunks():
                f.write(chunk)
        task = process_file_task.delay(video.id, file_path, ext)
        return Response({
            'task_id': task.id,
            'video_id': video.id,
            'message': 'File is being processed'
        }, status=status.HTTP_202_ACCEPTED)

    @extend_schema(
        request={'application/json': {
            'type': 'object',
            'properties': {
                'url': {'type': 'string', 'example': 'https://www.youtube.com/watch?v=dQw4w9WgXcQ'},
                'language': {'type': 'string', 'enum': ['en', 'ru', 'tg'], 'example': 'en'}
            },
            'required': ['url']
        }},
        responses={202: {'type': 'object', 'properties': {
            'task_id': {'type': 'string'},
            'video_id': {'type': 'integer'},
            'message': {'type': 'string'}
        }}}
    )
    @action(detail=False, methods=['post'], url_path='process', parser_classes=[JSONParser])
    def process(self, request):
        url = request.data.get('url')
        language = request.data.get('language', request.user.language)
        if not url:
            return Response({'error': 'URL is required'}, status=status.HTTP_400_BAD_REQUEST)
        video = VideoHistory.objects.create(
            user=request.user,
            url=url,
            language=language,
            status='pending'
        )
        task = process_video_task.delay(video.id)
        return Response({
            'task_id': task.id,
            'video_id': video.id,
            'message': 'Video is being processed'
        }, status=status.HTTP_202_ACCEPTED)

    @extend_schema(
        parameters=[OpenApiParameter('task_id', str, OpenApiParameter.PATH)],
        responses={200: {'type': 'object', 'properties': {
            'status': {'type': 'string'},
            'step': {'type': 'string'}
        }}}
    )
    @action(detail=False, methods=['get'], url_path='task/(?P<task_id>[^/.]+)')
    def task_status(self, request, task_id=None):
        task = AsyncResult(task_id)
        if task.state == 'PROGRESS':
            return Response({'status': 'processing', 'step': task.info.get('step', '')})
        elif task.state == 'SUCCESS':
            return Response({'status': 'done', 'result': task.result})
        elif task.state == 'FAILURE':
            return Response({'status': 'failed', 'error': str(task.info)}, status=status.HTTP_500_INTERNAL_SERVER_ERROR)
        return Response({'status': task.state.lower()})

    @extend_schema(responses={200: VideoHistorySerializer(many=True)})
    @action(detail=False, methods=['get'], url_path='history')
    def history(self, request):
        cache_key = f'video_history_{request.user.id}'
        cached = cache.get(cache_key)
        if cached is not None:
            return Response(cached)
        queryset = VideoHistory.objects.filter(user=request.user).order_by('-created_at')
        paginator = StandardPagination()
        page = paginator.paginate_queryset(queryset, request)
        data = VideoHistorySerializer(page, many=True).data
        cache.set(cache_key, data, timeout=60 * 5)
        return paginator.get_paginated_response(data)

    @extend_schema(responses={200: VideoHistorySerializer})
    def retrieve(self, request, pk=None):
        try:
            video = VideoHistory.objects.get(id=pk)
            self.check_object_permissions(request, video)
            return Response(VideoHistorySerializer(video).data)
        except VideoHistory.DoesNotExist:
            return Response({'error': 'Not found'}, status=status.HTTP_404_NOT_FOUND)

    def destroy(self, request, pk=None):
        try:
            video = VideoHistory.objects.get(id=pk)
            self.check_object_permissions(request, video)
            video.delete()
            cache.delete(f'video_history_{request.user.id}')
            return Response(status=status.HTTP_204_NO_CONTENT)
        except VideoHistory.DoesNotExist:
            return Response({'error': 'Not found'}, status=status.HTTP_404_NOT_FOUND)
        
    @extend_schema(
        request={'multipart/form-data': {
            'type': 'object',
            'properties': {
                'image': {'type': 'string', 'format': 'binary'},
                'language': {'type': 'string', 'enum': ['en', 'ru', 'tj']}
            },
            'required': ['image']
        }},
        responses={200: {'type': 'object', 'properties': {
            'text': {'type': 'string'},
            'char_count': {'type': 'integer'}
        }}}
    )
    @action(detail=False, methods=['post'], url_path='extract-image', parser_classes=[MultiPartParser, FormParser])
    def extract_image(self, request):
        image = request.FILES.get('image')
        if not image:
            return Response({'error': 'image is required'}, status=status.HTTP_400_BAD_REQUEST)
        allowed = ['.jpg', '.jpeg', '.png', '.bmp', '.tiff']
        ext = os.path.splitext(image.name)[1].lower()
        if ext not in allowed:
            return Response({'error': f'Allowed formats: {", ".join(allowed)}'}, status=status.HTTP_400_BAD_REQUEST)
        if image.size > 10 * 1024 * 1024:
            return Response({'error': 'Max image size: 10MB'}, status=status.HTTP_400_BAD_REQUEST)
        tmp_path = f'/tmp/deeplink_img_{request.user.id}{ext}'
        with open(tmp_path, 'wb') as f:
            for chunk in image.chunks():
                f.write(chunk)
        try:
            from .file_processor import extract_text_from_image
            text = extract_text_from_image(tmp_path)
            if not text:
                return Response({'error': 'No text found in image'}, status=status.HTTP_422_UNPROCESSABLE_ENTITY)
            return Response({'text': text, 'char_count': len(text)})
        except Exception as e:
            return Response({'error': str(e)}, status=status.HTTP_500_INTERNAL_SERVER_ERROR)
        finally:
            if os.path.exists(tmp_path):
                os.remove(tmp_path)