from django.test import TestCase
from django.urls import reverse
from rest_framework.test import APIClient
from rest_framework import status
from .models import User, VideoHistory


class AuthAPITest(TestCase):

    def setUp(self):
        self.client = APIClient()
        self.user = User.objects.create_user(
            email='test@example.com',
            username='testuser',
            password='TestPass123!',
            is_email_verified=True,
            is_active=True,
        )

    def test_register_returns_201(self):
        response = self.client.post('/api/auth/register/', {
            'email': 'new@example.com',
            'username': 'newuser',
            'password': 'NewPass123!',
            'password2': 'NewPass123!',
            'language': 'en'
        })
        self.assertEqual(response.status_code, 201)

    def test_login_returns_200(self):
        response = self.client.post('/api/auth/login/', {
            'email': 'test@example.com',
            'password': 'TestPass123!'
        })
        self.assertEqual(response.status_code, 200)
        self.assertIn('access', response.data)
        self.assertIn('refresh', response.data)

    def test_login_wrong_password_returns_401(self):
        response = self.client.post('/api/auth/login/', {
            'email': 'test@example.com',
            'password': 'wrongpassword'
        })
        self.assertEqual(response.status_code, 401)

    def test_profile_authenticated_returns_200(self):
        self.client.force_authenticate(user=self.user)
        response = self.client.get('/api/auth/profile/')
        self.assertEqual(response.status_code, 200)
        self.assertEqual(response.data['email'], 'test@example.com')

    def test_profile_unauthenticated_returns_401(self):
        response = self.client.get('/api/auth/profile/')
        self.assertEqual(response.status_code, 401)

    def test_logout_returns_200(self):
        login_response = self.client.post('/api/auth/login/', {
            'email': 'test@example.com',
            'password': 'TestPass123!'
        })
        self.client.force_authenticate(user=self.user)
        response = self.client.post('/api/auth/logout/', {
            'refresh': login_response.data['refresh']
        })
        self.assertEqual(response.status_code, 200)

    def test_change_password_returns_200(self):
        self.client.force_authenticate(user=self.user)
        response = self.client.post('/api/auth/change-password/', {
            'old_password': 'TestPass123!',
            'new_password': 'NewPass456!'
        })
        self.assertEqual(response.status_code, 200)

    def test_token_refresh_returns_200(self):
        login_response = self.client.post('/api/auth/login/', {
            'email': 'test@example.com',
            'password': 'TestPass123!'
        })
        response = self.client.post('/api/auth/token/refresh/', {
            'refresh': login_response.data['refresh']
        })
        self.assertEqual(response.status_code, 200)


class VideoAPITest(TestCase):

    def setUp(self):
        self.client = APIClient()
        self.user = User.objects.create_user(
            email='test@example.com',
            username='testuser',
            password='TestPass123!',
            is_email_verified=True,
            is_active=True,
        )
        self.client.force_authenticate(user=self.user)

    def test_video_history_returns_200(self):
        response = self.client.get('/api/videos/history/')
        self.assertEqual(response.status_code, 200)

    def test_video_process_no_url_returns_400(self):
        response = self.client.post('/api/videos/process/', {})
        self.assertEqual(response.status_code, 400)

    def test_video_process_with_url_returns_202(self):
        response = self.client.post('/api/videos/process/', {
            'url': 'https://www.youtube.com/watch?v=dQw4w9WgXcQ',
            'language': 'en'
        })
        self.assertEqual(response.status_code, 202)

    def test_video_detail_not_found_returns_404(self):
        response = self.client.get('/api/videos/999/')
        self.assertEqual(response.status_code, 404)

    def test_video_history_unauthenticated_returns_401(self):
        self.client.force_authenticate(user=None)
        response = self.client.get('/api/videos/history/')
        self.assertEqual(response.status_code, 401)