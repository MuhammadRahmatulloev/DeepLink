from django.urls import path, include
from rest_framework.routers import DefaultRouter
from rest_framework_simplejwt.views import TokenRefreshView
from . import views

router = DefaultRouter()
router.register('auth', views.AuthViewSet, basename='auth')
router.register('videos', views.VideoViewSet, basename='videos')
router.register('auth', views.GoogleAuthViewSet, basename='google-auth')

urlpatterns = [
    path('', include(router.urls)),
    path('auth/token/refresh/', TokenRefreshView.as_view(), name='token-refresh'),
]