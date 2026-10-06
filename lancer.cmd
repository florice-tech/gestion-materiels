@echo off
REM Lance l application (serveur + pages) : http://localhost:8080
REM Pour les telephones, definir l adresse du PC avant : set APP_URL=http://192.168.1.20:8080
cd /d "%~dp0backend"
call mvnw.cmd spring-boot:run %*
