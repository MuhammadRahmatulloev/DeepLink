from rest_framework import serializers
from django.contrib.auth.password_validation import validate_password
from .models import User, VideoHistory
from .file_processor import ALLOWED_EXTENSIONS, MAX_FILE_SIZE_MB
import os


class LoginSerializer(serializers.Serializer):
    email = serializers.EmailField()
    password = serializers.CharField(write_only=True)


class RegisterSerializer(serializers.ModelSerializer):
    password = serializers.CharField(write_only=True, validators=[validate_password])
    password2 = serializers.CharField(write_only=True)

    class Meta:
        model = User
        fields = ['email', 'username', 'password', 'password2', 'language']

    def validate(self, attrs):
        if attrs['password'] != attrs['password2']:
            raise serializers.ValidationError({'password': 'Passwords do not match'})
        return attrs

    def create(self, validated_data):
        validated_data.pop('password2')
        user = User.objects.create_user(
            email=validated_data['email'],
            username=validated_data['username'],
            password=validated_data['password'],
            language=validated_data.get('language', 'en'),
            is_active=False
        )
        return user


class UserSerializer(serializers.ModelSerializer):
    class Meta:
        model = User
        fields = ['id', 'email', 'username', 'language', 'avatar', 'is_email_verified', 'created_at']
        read_only_fields = ['id', 'email', 'is_email_verified', 'created_at']


class ChangePasswordSerializer(serializers.Serializer):
    old_password = serializers.CharField(write_only=True)
    new_password = serializers.CharField(write_only=True, validators=[validate_password])

    def validate_old_password(self, value):
        user = self.context['request'].user
        if not user.check_password(value):
            raise serializers.ValidationError('Old password is incorrect')
        return value

    def save(self):
        user = self.context['request'].user
        user.set_password(self.validated_data['new_password'])
        user.save()


class VideoHistorySerializer(serializers.ModelSerializer):
    class Meta:
        model = VideoHistory
        fields = ['id', 'url', 'title', 'transcript', 'explanation', 'language', 'status', 'ai_provider_used', 'created_at']
        read_only_fields = ['id', 'title', 'transcript', 'explanation', 'status', 'ai_provider_used', 'created_at']


class FileUploadSerializer(serializers.Serializer):
    file = serializers.FileField()
    language = serializers.ChoiceField(choices=['en', 'ru', 'tg'], default='en')

    def validate_file(self, value):
        ext = os.path.splitext(value.name)[1].lower()
        if ext not in ALLOWED_EXTENSIONS:
            raise serializers.ValidationError(f'Unsupported file type. Allowed: {", ".join(ALLOWED_EXTENSIONS)}')
        if value.size > MAX_FILE_SIZE_MB * 1024 * 1024:
            raise serializers.ValidationError(f'File too large. Max size: {MAX_FILE_SIZE_MB}MB')
        return value