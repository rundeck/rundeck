:: depot-setup.bat
::
:: $Revision: 1084 $
::
@ECHO off
setlocal
GOTO:Start

:Start

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

IF NOT DEFINED ANT_HOME (
	SET ANT_HOME=%RDECK_BASE%\pkgs\apache-ant-1.8.1
)

IF "%JAVA_HOME%" =="" (
   echo JAVA_HOME not set
   GOTO:EOF
)
IF NOT EXIST "%JAVA_HOME%\bin\java.exe" (
   ECHO JAVA_HOME not set or set incorrectly
   GOTO:EOF
)


set Path=%JAVA_HOME%\bin:%ANT_HOME%\bin:%Path%

CALL %JAVA_HOME%\bin\java "-Dant.home=%ANT_HOME%" ^
                          "-Drdeck.base=%RDECK_BASE%" ^
                          "-Duser.name=%USER_NAME%" ^
                           %RDECK_SSL_OPTS% ^
                          -cp "%RDECK_BASE%\classes;%ANT_HOME%\lib\xerces-2.6.0.jar;%ANT_HOME%\lib\xml-apis.jar" ^
                          com.dtolabs.rundeck.core.cli.project.ProjectTool %*

IF NOT "%ERRORLEVEL%"=="0" GOTO:EXITSetup



:EXITSetup

EXIT /B %ERRORLEVEL%



