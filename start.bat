@echo off
start /min "Django" cmd /k "cd /d C:\Users\Muhammad\Desktop\DeepLink && .venv\Scripts\activate && py manage.py runserver 0.0.0.0:8000"
timeout /t 3 /nobreak >nul
start /min "Celery" cmd /k "cd /d C:\Users\Muhammad\Desktop\DeepLink && .venv\Scripts\activate && celery -A config worker --loglevel=info --pool=solo"
timeout /t 2 /nobreak >nul
"C:\Users\Muhammad\AppData\Local\Android\Sdk\platform-tools\adb.exe" reverse tcp:8000 tcp:8000
exit