@echo off
rem Fraud Management System - Part 2 (JavaFX) - B NIKITHA, 25BCE2517
rem Double-click this file to compile and start the JavaFX app.
cd /d "%~dp0"

rem Folder of the JavaFX SDK "lib" folder. Change it if you unzipped the SDK somewhere else,
rem or set JAVAFX_LIB before running this file.
if "%JAVAFX_LIB%"=="" set "JAVAFX_LIB=C:\javafx-sdk-21.0.6\lib"
if not exist "%JAVAFX_LIB%\javafx.controls.jar" (
  echo JavaFX SDK not found in %JAVAFX_LIB%
  echo Download the JavaFX SDK for Windows from https://gluonhq.com/products/javafx/ and fix JAVAFX_LIB in run.bat
  pause & exit /b
)
if not exist lib\mysql-connector-j*.jar (
  echo Put the mysql-connector-j .jar file inside the lib folder first.
  pause & exit /b
)

set /p FRAUD_DB_PASS=Enter MySQL root password: 
set /p LOAD=Load the database from database\guard_schema.sql now? (y/n): 
if /i "%LOAD%"=="y" (
  mysql -u root -p%FRAUD_DB_PASS% < database\guard_schema.sql
  if errorlevel 1 ( echo Could not load the schema. Check the password and that mysql is in PATH. & pause & exit /b )
)

if not exist out mkdir out
javac -d out --module-path "%JAVAFX_LIB%" --add-modules javafx.controls GuardDb.java GuardApp.java
if errorlevel 1 ( echo Compile failed. & pause & exit /b )

java --module-path "%JAVAFX_LIB%" --add-modules javafx.controls -cp "out;lib\*" GuardApp
pause
