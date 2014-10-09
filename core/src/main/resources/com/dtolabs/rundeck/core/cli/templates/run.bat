:: Copyright 2010 DTO Labs, Inc. (http://dtolabs.com)
::
:: Licensed under the Apache License, Version 2.0 (the "License");
:: you may not use this file except in compliance with the License.
:: You may obtain a copy of the License at
::
::     http://www.apache.org/licenses/LICENSE-2.0
::
:: Unless required by applicable law or agreed to in writing, software
:: distributed under the License is distributed on an "AS IS" BASIS,
:: WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
:: See the License for the specific language governing permissions and
:: limitations under the License.
:: $RCSfile$
::
:: $Revision: 1084 $
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
    %RDECK_SSL_OPTS% ^
    -Djava.ext.dirs=%RD_LIBDIR% ^
    com.dtolabs.rundeck.core.cli.run.RunTool %*

IF NOT "%ERRORLEVEL%"=="0" GOTO:EXITSetup



:EXITSetup

EXIT /B %ERRORLEVEL%



