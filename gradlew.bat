@rem Gradle startup script for Windows
@echo off
setlocal

set APP_HOME=%~dp0
if defined JAVA_HOME (
  set JAVA_EXE=%JAVA_HOME%\bin\java.exe
) else (
  set JAVA_EXE=java.exe
)

if not exist "%APP_HOME%gradle\wrapper\gradle-wrapper.jar" (
  echo ERROR: missing %APP_HOME%gradle\wrapper\gradle-wrapper.jar 1>&2
  exit /b 1
)

"%JAVA_EXE%" %JAVA_OPTS% %GRADLE_OPTS% -classpath "%APP_HOME%gradle\wrapper\gradle-wrapper.jar" org.gradle.wrapper.GradleWrapperMain %*
endlocal
