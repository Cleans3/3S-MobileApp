@rem
@rem Copyright 2015 the original author or authors.
@rem
@if "%DEBUG%" == "" @echo off
set DIRNAME=%~dp0
if "%DIRNAME%" == "" set DIRNAME=.
set APP_BASE_NAME=%~n0
set APP_HOME=%DIRNAME%
set JAVACMD=java
if defined JAVA_HOME goto findJavaFromJavaHome
goto execute
:findJavaFromJavaHome
set JAVACMD=%JAVA_HOME%\bin\java.exe
:execute
"%JAVACMD%" -classpath "%APP_HOME%\gradle\wrapper\gradle-wrapper.jar" org.gradle.wrapper.GradleWrapperMain %*
