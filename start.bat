@echo off
start "Django" cmd /k "cd /d C:\Users\Muhammad\Desktop\DeepLink && .venv\Scripts\activate && py manage.py runserver 0.0.0.0:8000"
timeout /t 3
start "Celery" cmd /k "cd /d C:\Users\Muhammad\Desktop\DeepLink && .venv\Scripts\activate && celery -A config worker --loglevel=info --pool=solo"
timeout /t 2
"C:\Users\Muhammad\AppData\Local\Android\Sdk\platform-tools\adb.exe" reverse tcp:8000 tcp:8000
echo Done. Backend + Celery started, adb reverse active.
pause