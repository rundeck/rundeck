@echo off 

rem Script to create the RunDeck MSI installer.

setlocal 

set RD_VERSION=%1
set CWD=%CD%
set TARGET=%CWD%\target
set TARGETSRCS=%TARGET%\SOURCES

@rem check Wix tools are in the PATH:
where candle.exe >NUL: 2>NUL:
if ERRORLEVEL 1 (
  echo [ERROR]: Please put WIX-35 tools in your path.
  goto EOF
)

@rem create target directory and sub working directory
if not exist %TARGETSRCS% (
  echo [INFO]: Creating directory %TARGETSRCS%
  mkdir %TARGETSRCS%
)

@rem Install using the rundeck-launcher --installonly option:
if not exist %TARGETSRCS%\server (
  echo [INFO]: Copying and installing RunDeck using '--installonly' option
  pushd %TARGETSRCS%
  echo [INFO]:   Copying rundeck-launcher-%RD_VERSION%.jar
  copy %CWD%\..\rundeck-launcher\launcher\target\rundeck-launcher-%RD_VERSION%.jar > NUL:
  echo [INFO]:   Installing RunDeck using --installonly option
  java -jar rundeck-launcher-%RD_VERSION%.jar --installonly
  del rundeck-launcher-%RD_VERSION%.jar
  popd
)

@rem Test if install using the rundeck-launcher --installonly option completed:
if not exist %TARGETSRCS%\server (
  echo [ERROR]: Installing RunDeck using '--installonly' option failed
  goto EOF
)

@rem Truncate all installable files to 0-bytes and create rundeck.cmd batch file.
if not exist %TARGETSRCS%\rundeck.cmd (
  echo [INFO]: Creating "rundeck.cmd" command script to be used as shortcut
  echo set RDECK_BASE=%%~dp0> %TARGETSRCS%\rundeck.cmd
  echo cd %%RDECK_BASE%%>> %TARGETSRCS%\rundeck.cmd
  echo java -cp server/lib/rundeck-jetty-server-1.4.2.jar;server/lib/jetty-6.1.21.jar\;server/lib/jetty-naming-6.1.21.jar;server/lib/jetty-plus-6.1.21.jar;server/lib/jetty-util-6.1.21.jar;server/lib/servlet-api-2.5-20081211.jar  com.dtolabs.rundeck.RunServer .>> %TARGETSRCS%\rundeck.cmd  
)

@rem msi:
if not exist %TARGET%\rundeck.msi (
  echo [INFO]: Creating "rundeck.msi..."
  pushd %TARGETSRCS%
    echo [INFO]:   Running "heat.exe"
    heat.exe dir "." -nologo -var var.SOURCES -gg -scom -sreg -sfrag -srd -dr INSTALLDIR -cg RunDeckComponentGroup -out ../rundeck-files.wxs > %TARGET%\heatOut.log
    echo [INFO]:   Running "candle.exe"
    candle.exe -nologo -dSOURCES=. -dVERSION=%RD_VERSION% -out ../ ../../rundeck.wxs ../../rundeck-dialogs.wxs ../rundeck-files.wxs > %TARGET%\heatOut.log
    echo [INFO]:   Running "light.exe"
    light.exe -nologo -dSOURCES=. -dVERSION=1.4.0 -out ../rundeck.msi -ext WixUIExtension -cultures:en-us ../rundeck.wixobj ../rundeck-dialogs.wixobj ../rundeck-files.wixobj > %TARGET%\heatOut.log
  popd
)

@rem Test if .msi installer was successfully created
if exist %TARGET%\rundeck.msi (
  echo [INFO]: Creation "rundeck.msi" succeeded
) else (
  echo [ERROR]: Creation "rundeck.msi" failed
)

:EOF
endlocal