:: dispatch.bat
::
:: $Revision: 1022 $
::
@ECHO off
setlocal

IF DEFINED USER (
   set USER_NAME=%USER%
) ELSE (
   set USER_NAME=%USERNAME%
)

IF NOT DEFINED RDECK_BASE (
   ECHO RDECK_BASE not set
   GOTO:EOF
)

IF NOT EXIST "%RDECK_BASE%\etc\profile.bat" (
   ECHO Unable to source %RDECK_BASE%\etc\profile.bat
   GOTO:EOF
) ELSE (
	CALL "%RDECK_BASE%\etc\profile.bat"
)

IF NOT EXIST "%ANT_HOME%\bin\ant.bat" (
    ECHO ANT_HOME not configured or non-existent.
   GOTO:EOF
)


::echo Classpath is %cp%
SET cp=%RDECK_HOME%\classes;%ANT_HOME%\lib\xerces-2.6.0.jar;%ANT_HOME%\lib\xml-apis.jar



::
:: run dispatch main class
::
call "%JAVA_HOME%\bin\java.exe" ^
    -Xms64m -Xmx128m ^
	-classpath "%cp%" ^
    -Drdeck.base="%RDECK_BASE%" ^
	-Drdeck.home="%RDECK_HOME%" ^
	-Dant.home="%ANT_HOME%" ^
    %RDECK_SSL_OPTS% ^
    -Drdeck.traceExceptions="%RUNDECK_TRACE_EXCEPTIONS%" ^
    -Drdeck.cli.terse="%RUNDECK_CLI_TERSE%" ^
	com.dtolabs.rundeck.core.cli.ExecTool %*
