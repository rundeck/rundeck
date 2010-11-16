set RDECK_HOME=/private/tmp/rdl

set RDECK_BASE=/private/tmp/rdl

set JAVA_HOME=/System/Library/Java/JavaVirtualMachines/1.6.0.jdk/Contents/Home

:: Unsetting JRE_HOME to ensure there is no conflict with JAVA_HOME
(set JRE_HOME=)

set Path=%JAVA_HOME%\bin;%RDECK_HOME%\bin;%Path%