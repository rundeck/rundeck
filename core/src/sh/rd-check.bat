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

IF NOT DEFINED RDECK_HOME (
   ECHO RDECK_HOME not set
   GOTO:EOF
)

IF NOT DEFINED ANT_HOME (
	SET ANT_HOME=%RDECK_HOME%\pkgs\apache-ant-1.8.1
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
                          "-Duser.java_home=%JAVA_HOME%" ^
                          "-Drdeck.home=%RDECK_HOME%" ^
                          "-Drdeck.base=%RDECK_BASE%" ^
                          -cp "%RDECK_HOME%\classes;%ANT_HOME%\lib\ant.jar;%ANT_HOME%\lib\ant-launcher.jar;%ANT_HOME%\lib\regexp-1.5.jar;%ANT_HOME%\lib\ant-apache-regexp.jar" ^
                          com.dtolabs.launcher.Check %*

IF NOT "%ERRORLEVEL%"=="0" GOTO:EXITSetup


:EXITSetup

EXIT /B %ERRORLEVEL%



