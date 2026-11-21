@rem Setup default variables
@set APP_BASE_NAME=%~nx0
@set APP_HOME=%~dp0

@if defined JAVA_HOME goto findJavaFromJavaHome

@set JAVACMD=java.exe
%JAVACMD% -version >NUL 2>&1
@if %ERRORLEVEL% equ 0 goto execute

:findJavaFromJavaHome
@set JAVACMD=%JAVA_HOME%\bin\java.exe

:execute
@set CLASSPATH=%APP_HOME%\gradle\wrapper\gradle-wrapper.jar
"%JAVACMD%" %DEFAULT_JVM_OPTS% -jar "%CLASSPATH%" %*
