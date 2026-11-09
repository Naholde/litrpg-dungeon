@echo off
rem Запуск игры с модом из исходников и вход на сервер подземелья.
rem   play.bat            — ник Naholde, сервер 95.182.99.124
rem   play.bat Ник        — другой ник
rem Окно можно свернуть; игра работает сама по себе, Claude для неё не нужен.
set NICK=%1
if "%NICK%"=="" set NICK=Naholde
set JAVA_HOME=C:\Users\arsen\.litrpg-tools\jdk-21.0.13+9
cd /d "%~dp0..\.."
call gradlew.bat runClient -Pjoin=95.182.99.124:25565 -Pplayer=%NICK% --no-daemon
