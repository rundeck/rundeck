% Version 2.3.0
% greg
% 10/28/2014

Release 2.3.0
=============

Date: 2014-10-28

* Improved support for use of Windows, both as a Rundeck server 
    and a remote node.
    * Fixed outstanding issues with CLI .bat scripts
    * Support powershell scripts by allowing configuration
        of file extension to be used in workflow script steps. 
        E.g use ".ps1" because powershell will not execute a script 
        that doesn't end in .ps1.
    * Other fixes for issues with script-based plugins and Windows paths.
* Added support for storing Passwords in the Key Storage facility.  
    The built-in SSH execution and SCP file copy 
    both now support using stored passwords.
    Note: the Key Storage facility is not encrypted by default, see
    [Key Storage](../administration/key-storage.html).
* Added a new GUI for uploading Passwords and public/private keys
    to the Key Storage facility
* Bug fixes
* Disable SSLv3 by default

## Contributors

* Greg Schueler (gschueler)
* JayKim (c-jason-kim)

## Bug Reporters

* aparsons
* c-jason-kim
* csciarri
* dennis-benzinger-hybris
* desaim
* gschueler
* jdmulloy
* jefffiser
* jippi
* lmayorga1980
* pwhack
* stagrlee

## Issues

* [Disable SSL Undesired Versions](https://github.com/rundeck/rundeck/issues/987)
* [add MaxPermSize to default JVM args](https://github.com/rundeck/rundeck/issues/985)
* [script-based file-copier plugin: always pass a destination path](https://github.com/rundeck/rundeck/issues/981)
* [windows launcher: first run causes a '${framework.var.dir}' directory to be created](https://github.com/rundeck/rundeck/issues/980)
* [node exec script plugins: allow custom plugin config properties](https://github.com/rundeck/rundeck/issues/979)
* [2.2: windows: file storage shows incorrect paths](https://github.com/rundeck/rundeck/issues/978)
* [windows script-plugins: first invocation might fail because extracted file stream is not closed](https://github.com/rundeck/rundeck/issues/977)
* [Broken Status Command in Rundeck Launcher Init Script ](https://github.com/rundeck/rundeck/issues/973)
* [Execution of reference job (by Parent Job) not working in v2.2.3-1](https://github.com/rundeck/rundeck/issues/968)
* [java.lang.NullPointerException thrown when starting a job](https://github.com/rundeck/rundeck/issues/964)
* [Can't save script step with a long script](https://github.com/rundeck/rundeck/issues/963)
* [parallel execution continues after a node failure even if keepgoing is false](https://github.com/rundeck/rundeck/pull/962)
* [Can't delete jobs using ACL Policy in documentation](https://github.com/rundeck/rundeck/issues/961)
* [SSH key upload via GUI](https://github.com/rundeck/rundeck/issues/957)
* [Broken link: "Option model provider" link to guide when adding job option](https://github.com/rundeck/rundeck/issues/945)
* [Allow custom file extension for script temp files](https://github.com/rundeck/rundeck/issues/933)
* [Documentation: SSL on Debian/Ubuntu Installs](https://github.com/rundeck/rundeck/issues/914)
* [Issue with run.bat file on windows hosted rundeck instance](https://github.com/rundeck/rundeck/issues/843)
* [Add a MaxPermSize to recommended launcher commandline](https://github.com/rundeck/rundeck/issues/687)
* [profile.bat needs RDECK_JVM](https://github.com/rundeck/rundeck/issues/570)
* [unix format on windows for profile.bat](https://github.com/rundeck/rundeck/issues/569)
* [Add support for password storage to SSH plugins](https://github.com/rundeck/rundeck/issues/989)
