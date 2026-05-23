from rest_framework import status, viewsets, generics
from rest_framework.decorators import action
from rest_framework.views import APIView
from rest_framework.response import Response
from rest_framework.permissions import IsAuthenticated, AllowAny
from rest_framework_simplejwt.tokens import RefreshToken
from django.contrib.auth import authenticate
from django.core.mail import send_mail
from django.conf import settings
from django.core.cache import cache
from celery.result import AsyncResult
from drf_spectacular.utils import extend_schema, OpenApiParameter
from .models import User, EmailVerificationToken, VideoHistory
from .serializers import RegisterSerializer, UserSerializer, ChangePasswordSerializer, VideoHistorySerializer
from .tasks import process_video_task
from .permissions import IsEmailVerified, IsOwner, IsOwnerOrAdmin
from .pagination import StandardPagination
from .tasks import process_file_task
from .serializers import FileUploadSerializer
import os
from django.contrib.auth import login
from social_django.utils import psa


class AuthViewSet(viewsets.ViewSet):

    def get_permissions(self):
        if self.action in ['register', 'login', 'verify_email']:
            return [AllowAny()]
        return [IsAuthenticated()]

    @extend_schema(request=RegisterSerializer, responses={201: {'type': 'object'}})
    @action(detail=False, methods=['post'], url_path='register')
    def register(self, request):
        serializer = RegisterSerializer(data=request.data)
        if not serializer.is_valid():
            return Response(serializer.errors, status=status.HTTP_400_BAD_REQUEST)

        user = serializer.save()
        token = EmailVerificationToken.objects.create(user=user)
        verify_url = f"{request.scheme}://{request.get_host()}/api/auth/verify-email/{token.token}/"

        send_mail(
            subject='Verify your DeepLink account',
            message=f'Click the link to verify your email: {verify_url}',
            from_email=settings.DEFAULT_FROM_EMAIL,
            recipient_list=[user.email],
            fail_silently=False,
        )
        return Response(
            {'message': 'Registration successful. Check your email to verify your account.'},
            status=status.HTTP_201_CREATED
        )

    @extend_schema(responses={200: {'type': 'object'}})
    @action(detail=False, methods=['get'], url_path='verify-email/(?P<token>[^/.]+)')
    def verify_email(self, request, token=None):
        try:
            verification = EmailVerificationToken.objects.get(token=token, is_used=False)
        except EmailVerificationToken.DoesNotExist:
            return Response({'error': 'Invalid or expired token'}, status=status.HTTP_400_BAD_REQUEST)

        verification.user.is_email_verified = True
        verification.user.is_active = True
        verification.user.save()
        verification.is_used = True
        verification.save()
        return Response({'message': 'Email verified successfully. You can now log in.'})

    @extend_schema(
        request={'type': 'object', 'properties': {'email': {'type': 'string'}, 'password': {'type': 'string'}}},
        responses={200: UserSerializer}
    )
    @action(detail=False, methods=['post'], url_path='login')
    def login(self, request):
        email = request.data.get('email')
        password = request.data.get('password')

        user = authenticate(request, username=email, password=password)
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

    @action(detail=False, methods=['post'], url_path='logout')
    def logout(self, request):
        try:
            token = RefreshToken(request.data.get('refresh'))
            token.blacklist()
            return Response({'message': 'Logged out successfully'})
        except Exception:
            return Response({'error': 'Invalid token'}, status=status.HTTP_400_BAD_REQUEST)

    @extend_schema(responses={200: UserSerializer})
    @action(detail=False, methods=['get', 'patch'], url_path='profile')
    def profile(self, request):
        if request.method == 'GET':
            return Response(UserSerializer(request.user).data)

        serializer = UserSerializer(request.user, data=request.data, partial=True)
        if not serializer.is_valid():
            return Response(serializer.errors, status=status.HTTP_400_BAD_REQUEST)
        serializer.save()
        return Response(serializer.data)

    @extend_schema(request=ChangePasswordSerializer, responses={200: {'type': 'object'}})
    @action(detail=False, methods=['post'], url_path='change-password')
    def change_password(self, request):
        serializer = ChangePasswordSerializer(data=request.data, context={'request': request})
        if not serializer.is_valid():
            return Response(serializer.errors, status=status.HTTP_400_BAD_REQUEST)
        serializer.save()
        return Response({'message': 'Password changed successfully'})


class VideoViewSet(viewsets.ViewSet):
    permission_classes = [IsAuthenticated, IsEmailVerified]

    @extend_schema(
        request={'type': 'object', 'properties': {
            'url': {'type': 'string'},
            'language': {'type': 'string', 'enum': ['en', 'ru', 'tg']}
        }},
        responses={202: {'type': 'object'}}
    )
    @action(detail=False, methods=['post'], url_path='process')
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
        responses={200: {'type': 'object'}}
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
    
    def get_permissions(self):
        if self.action in ['retrieve', 'destroy']:
            return [IsAuthenticated(), IsEmailVerified(), IsOwnerOrAdmin()]
        return [IsAuthenticated(), IsEmailVerified()]
    

@extend_schema(request={'multipart/form-data': FileUploadSerializer}, responses={202: {'type': 'object'}})
@action(detail=False, methods=['post'], url_path='process-file')
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


class GoogleAuthViewSet(viewsets.ViewSet):
    permission_classes = [AllowAny]

    @extend_schema(
        request={'type': 'object', 'properties': {'access_token': {'type': 'string'}}},
        responses={200: UserSerializer}
    )
    @action(detail=False, methods=['post'], url_path='google')
    def google_login(self, request):
        access_token = request.data.get('access_token')
        if not access_token:
            return Response({'error': 'access_token is required'}, status=status.HTTP_400_BAD_REQUEST)

        import requests as req
        google_response = req.get(
            'https://www.googleapis.com/oauth2/v3/userinfo',
            headers={'Authorization': f'Bearer {access_token}'}
        )

        if google_response.status_code != 200:
            return Response({'error': 'Invalid Google token'}, status=status.HTTP_401_UNAUTHORIZED)

        google_data = google_response.json()
        email = google_data.get('email')
        name = google_data.get('name', '')

        if not email:
            return Response({'error': 'Email not provided by Google'}, status=status.HTTP_400_BAD_REQUEST)

        user, created = User.objects.get_or_create(
            email=email,
            defaults={
                'username': email.split('@')[0],
                'is_email_verified': True,
                'is_active': True,
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