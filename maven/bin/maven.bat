@echo off

REM ----------------------------------------------------------------------------
REM Maven Windows Batch script
REM ----------------------------------------------------------------------------

if "%JAVA_HOME%"  == "" goto error_java

REM -- Set up command line args according to the OS present

if not "%OS%"=="Windows_NT" goto win9x_start
:winNT_start
@setlocal

REM -- Need to check if we are using the 4NT shell...
if "%eval[2+2]" == "4" goto setup_4NT

REM -- On NT/2K grab all arguments at once
set MAVEN_CMD_LINE_ARGS=%*
If Not Exist "%MAVEN_HOME%" (
	echo MAVEN_HOME environment variable not found. Defaulting to "%ProgramFiles%\Maven"
    SET MAVEN_HOME=%ProgramFiles%\Maven
)
goto done_start

:setup_4NT
set MAVEN_CMD_LINE_ARGS=%$
goto done_start

:win9x_start
REM -- Slurp the command line arguments.  This loop allows for an unlimited
REM -- number of agruments (up to the command line limit, anyway).

set MAVEN_CMD_LINE_ARGS=

:setup_args
if %1a==a goto done_start
set MAVEN_CMD_LINE_ARGS=%MAVEN_CMD_LINE_ARGS% %1
shift
goto setup_args

:done_start
REM -- This label provides a place for the argument list loop to break out 
REM -- and for NT handling to skip to.

REM -- Look for MAVEN_HOME, and make sure it exists
if "%MAVEN_HOME%" == "" goto error_maven
if not EXIST "%MAVEN_HOME%" goto error_maven_not_found

SET MAVEN_OPTS=-Xmx128m
REM -- The following values are quoted as MAVEN_HOME might contain spaces
SET JAVA="%JAVA_HOME%\bin\java"
SET MAVEN_OPTS=%MAVEN_OPTS% "-Dmaven.home=%MAVEN_HOME%"
SET MAVEN_OPTS=%MAVEN_OPTS% "-Dtools.jar=%JAVA_HOME%\lib\tools.jar"
SET MAVEN_OPTS=%MAVEN_OPTS% "-Dforehead.conf.file=%MAVEN_HOME%\bin\forehead.conf"
SET MAVEN_CLASSPATH="%MAVEN_HOME%\lib\forehead-1.0-beta-4.jar"
SET MAVEN_MAIN=com.werken.forehead.Forehead

%JAVA% %MAVEN_OPTS% -classpath %MAVEN_CLASSPATH% %MAVEN_MAIN%  %MAVEN_CMD_LINE_ARGS%
goto end

:error_maven

echo ERROR: MAVEN_HOME not found in your environment.
echo Please, set the MAVEN_HOME variable in your environment to match the
echo location of the Maven installation

goto main_end

:error_maven_not_found
echo ERROR: File (%MAVEN_HOME%) MAVEN_HOME not found.
echo Please, set the MAVEN_HOME variable in your environment to match the
echo location of the Maven installation
goto main_end

:error_java

echo ERROR: JAVA_HOME not found in your environment.
echo Please, set the JAVA_HOME variable in your environment to match the
echo location of the Java installation

goto main_end

:end

set MAVEN_CMD_LINE_ARGS=
if not "%OS%"=="Windows_NT" goto main_end
@endlocal

:main_end
