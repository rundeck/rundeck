:: setup.bat
::
:: installs framework
::
:: $RCSfile$
:: $Revision: 2045 $
::
@ECHO off
setlocal
GOTO:Start

:Start

rem Guess RDECK_BASE if not defined
set "CURRENT_DIR=%cd%"
if not "%RDECK_BASE%" == "" goto gotRdeckBase
set "RDECK_BASE=%CURRENT_DIR%"
if exist "%RDECK_BASE%\etc\profile.bat" goto gotRdeckBase
set "RDECK_BASE="
:gotRdeckBase

IF NOT EXIST "%RDECK_BASE%\etc\profile.bat" (
   ECHO Unable to source %RDECK_BASE%\etc\profile.bat
   GOTO:EOF
) ELSE (
	CALL "%RDECK_BASE%\etc\profile.bat"
)

IF NOT DEFINED RDECK_BASE (
   ECHO RDECK_BASE not set
   GOTO:EOF
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

CALL "%JAVA_HOME%\bin\java" ^
    "-Duser.java_home=%JAVA_HOME%" ^
    %RDECK_CLI_OPTS% ^
    "-Drdeck.base=%RDECK_BASE%" ^
    -Djava.ext.dirs=%RD_LIBDIR% ^
    com.dtolabs.launcher.Setup %*

IF NOT "%ERRORLEVEL%"=="0" GOTO:EXITSetup


echo %* > "%RDECK_BASE%\etc\setup.status"

:EXITSetup

EXIT /B %ERRORLEVEL%



