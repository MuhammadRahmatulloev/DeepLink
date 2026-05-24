from django.contrib.auth.models import AbstractUser
from django.db import models
import uuid
import random


class User(AbstractUser):
    email = models.EmailField(unique=True)
    is_email_verified = models.BooleanField(default=False)
    avatar = models.ImageField(upload_to='avatars/', null=True, blank=True)
    language = models.CharField(
        max_length=5,
        choices=[('en', 'English'), ('ru', 'Russian'), ('tj', 'Tajik')],
        default='en'
    )
    created_at = models.DateTimeField(auto_now_add=True)

    USERNAME_FIELD = 'email'
    REQUIRED_FIELDS = ['username']

    def __str__(self):
        return self.email


class EmailVerificationToken(models.Model):
    user = models.ForeignKey(User, on_delete=models.CASCADE, related_name='verification_tokens')
    code = models.CharField(max_length=6)
    created_at = models.DateTimeField(auto_now_add=True)
    is_used = models.BooleanField(default=False)

    def save(self, *args, **kwargs):
        if not self.code:
            self.code = str(random.randint(100000, 999999))
        super().save(*args, **kwargs)

    def __str__(self):
        return f"{self.user.email} - {self.code}"


class VideoHistory(models.Model):
    STATUS_CHOICES = [
        ('pending', 'Pending'),
        ('processing', 'Processing'),
        ('done', 'Done'),
        ('failed', 'Failed'),
    ]

    user = models.ForeignKey(User, on_delete=models.CASCADE, related_name='videos')
    url = models.URLField()
    title = models.CharField(max_length=500, blank=True)
    transcript = models.TextField(blank=True)
    explanation = models.TextField(blank=True)
    language = models.CharField(max_length=5, default='en')
    status = models.CharField(max_length=20, choices=STATUS_CHOICES, default='pending')
    ai_provider_used = models.CharField(max_length=50, blank=True)
    created_at = models.DateTimeField(auto_now_add=True)

    def __str__(self):
        return f"{self.user.email} - {self.title}"