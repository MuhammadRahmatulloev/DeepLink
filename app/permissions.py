from rest_framework.permissions import BasePermission


class IsEmailVerified(BasePermission):
    message = 'Email address is not verified.'

    def has_permission(self, request, view):
        return bool(
            request.user and
            request.user.is_authenticated and
            request.user.is_email_verified
        )


class IsOwner(BasePermission):
    message = 'You do not have permission to access this object.'

    def has_object_permission(self, request, view, obj):
        return obj.user == request.user


class IsAdminUser(BasePermission):
    message = 'Admin access required.'

    def has_permission(self, request, view):
        return bool(
            request.user and
            request.user.is_authenticated and
            request.user.is_staff
        )


class IsOwnerOrAdmin(BasePermission):
    message = 'You do not have permission to access this object.'

    def has_object_permission(self, request, view, obj):
        return obj.user == request.user or request.user.is_staff