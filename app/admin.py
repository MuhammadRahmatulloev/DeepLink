from django.contrib import admin
from django.contrib.auth.admin import UserAdmin as BaseUserAdmin
from .models import User, EmailVerificationToken, VideoHistory


@admin.register(User)
class UserAdmin(BaseUserAdmin):
    list_display = ['email', 'username', 'language', 'is_email_verified', 'is_staff', 'created_at']
    list_filter = ['is_email_verified', 'is_staff', 'language']
    search_fields = ['email', 'username']
    ordering = ['-created_at']

    fieldsets = BaseUserAdmin.fieldsets + (
        ('DeepLink Info', {
            'fields': ('language', 'avatar', 'is_email_verified')
        }),
    )


@admin.register(EmailVerificationToken)
class EmailVerificationTokenAdmin(admin.ModelAdmin):
    list_display = ['user', 'code', 'is_used', 'created_at']
    list_filter = ['is_used']
    search_fields = ['user__email']
    readonly_fields = ['code', 'created_at']


@admin.register(VideoHistory)
class VideoHistoryAdmin(admin.ModelAdmin):
    list_display = ['user', 'title', 'status', 'language', 'ai_provider_used', 'created_at']
    list_filter = ['status', 'language', 'ai_provider_used']
    search_fields = ['user__email', 'title', 'url']
    readonly_fields = ['transcript', 'explanation', 'created_at']
    ordering = ['-created_at']