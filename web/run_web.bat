@echo off
rem Web version (Vaadin, pure Java). Needs JDK 21, Maven 3.9 and MySQL with ..\database\guard_schema.sql loaded.
rem The first build downloads the Vaadin and Jetty libraries from Maven Central (internet needed once).
cd /d "%~dp0"
if "%FRAUD_DB_PASS%"=="" set /p FRAUD_DB_PASS=MySQL password for root: 
call mvn -q package -DskipTests || goto :eof
echo Open http://localhost:8080/fraud in the browser (Ctrl+C here to stop)
java -cp "target\fraud-web.jar;target\lib\*" guard.Main
