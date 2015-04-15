% Version 2.5.0
% greg
% 04/15/2015

Release 2.5.0
=============

Date: 2015-04-15

Name: <span style="color: saddlebrown"><span class="glyphicon glyphicon-grain"></span> "cafe au lait saddlebrown grain"</span>

This release has many new features and bugfixes, and contains some refactoring under the hood.

## New Features

* Upgraded grails framework, which adds support for Java 8 (and removes Java 6)
* Can now store Project definition and configuration in the database (optional), see [Project Definitions](../administration/project-setup.html#project-definitions) and information in the [Upgrade Guide](http://rundeck.org/2.5.0/upgrading/index.html).
* Improved GUI for administering a project: modify Project configuration file contents, and readme.md/motd.md contents. Project archiving is also now performed asynchronously.
* New Jasypt encryption plugin can be used for Key Storage and Project Configuration, see [Jasypt Encryption Converter Plugin](../plugins-user-guide/storage-plugins.html#jasypt-encryption-converter-plugin)
* Support Key Storage password for SSH sudo and SSH private key passphrase
* New workflow step plugin: Assert state of another Job, optionally halt workflow with custom status string
* New `rd-acl` commandline tool can list test and generate .aclpolicy file contents, see [rd-acl](../man1/rd-acl.html)
* New Orchestrator plugin point. Orchestrators can be used to batch and sequence the matched nodes used in node dispatching, e.g.: only allow a certain % to run simultaneously.
	* [Plugins User Guide -  Orchestrator Plugins](../plugins-user-guide/orchestrator-plugins.html)
	* [Plugin Developer Guide -  Orchestrator Plugin](../developer/orchestrator-plugin.html)
	* [Example code](https://github.com/rundeck/rundeck/tree/development/examples/example-java-orchestrator-plugin)
* Added hybrid LDAP + properties file group authentication for JAAS (`JettyCombinedLdapLoginModule`)
	* [Administration - Authentication - Combining LDAP with other modules](http//rundeck.org/2.5.0/administration/authenticating-users.html#combining-ldap-with-other-modules)

## Upgrading

**Note:** We've attempted to maintain compatibility with previous plugins, but due to changes in the core library,
Rundeck may have issues loading or running some plugins.

Please report issues found to [Github issues](https://github.com/rundeck/rundeck/issues).

See the [Upgrading Guide](../upgrading/index.html)

## Contributors

* Andreas Knifh
* Greg Schueler (gschueler)
* Mark Bidewell (mbidewell)
* Mayank Asthana (mayankasthana)
* Puru Tuladhar (tuladhar)
* Thomas Mueller (vinzent)
* Yagi (yagince)
* ashley-taylor
* new23d

## Bug Reporters

* MartinMulder
* MartyNeal
* Zophar78
* adamhamner
* ahonor
* ashley-taylor
* brismuth
* danifr
* dbeckham
* ffk23
* gschueler
* hirsts
* hjdr4
* knifhen
* mayankasthana
* mbidewell
* mgherman
* mikagika
* new23d
* reigner-yrastorza
* travisgroth
* tuladhar
* vinzent
* yagince

## Issues

* [Project config page: plugin properties of wrong scope displayed](https://github.com/rundeck/rundeck/issues/1164)
* [Security: Script plugins: password rendering option plugin property files not properly obscured](https://github.com/rundeck/rundeck/issues/1163)
* [Display of active jobs maxes out at 20](https://github.com/rundeck/rundeck/issues/1161)
* [dispatch command does not work when project config stored in RDB](https://github.com/rundeck/rundeck/issues/1158)
* [Schedules don't launch](https://github.com/rundeck/rundeck/issues/1150)
* [Fix LDAP nested groups for Active Directory](https://github.com/rundeck/rundeck/pull/1149)
* [Documentation: Database docs link to 404 Not Found](https://github.com/rundeck/rundeck/issues/1148)
* [Fixed typo in command in docs and grammar](https://github.com/rundeck/rundeck/pull/1143)
* [Project definitions and configuration stored in DB](https://github.com/rundeck/rundeck/pull/1135)
* [Refactor authorization into components, add preauth attribute role](https://github.com/rundeck/rundeck/pull/1134)
* [Support ServerAliveInterval and ServerAliveCountMax](https://github.com/rundeck/rundeck/pull/1133)
* [Rundeck v2.4.0-1 rundeck not deleting dispatch files](https://github.com/rundeck/rundeck/issues/1131)
* [Dispatched inline script has race condition with crontab](https://github.com/rundeck/rundeck/issues/1129)
* [Storage provider/converter config info on Plugins page is wrong](https://github.com/rundeck/rundeck/issues/1128)
* [Add default storage encryption plugin](https://github.com/rundeck/rundeck/pull/1127)
* [JettyPamLoginModule: supplementalRoles split regex requires whitespace](https://github.com/rundeck/rundeck/pull/1124)
* [Using SSH stored password for sudo](https://github.com/rundeck/rundeck/issues/1122)
* [Jobs initiated from the crontab do not respect the timeout value given for that job](https://github.com/rundeck/rundeck/issues/1121)
* [Script plugin non-instance configuration fails](https://github.com/rundeck/rundeck/issues/1120)
* [Add "server uuid" element to the Execution info](https://github.com/rundeck/rundeck/issues/1119)
* [API doc: Getting Project Info response xml has bad formatting](https://github.com/rundeck/rundeck/issues/1118)
* [${result.resultCode} is not available in the error handler for a Local Command Plugin step.](https://github.com/rundeck/rundeck/issues/1114)
* [job state conditional plugin](https://github.com/rundeck/rundeck/pull/1105)
* [Add workflow step plugin: Assert state of another Job, optionally halt workflow with custom status string](https://github.com/rundeck/rundeck/issues/1104)
* [GUI: 2.4.x: Custom property input fields for node-step plugins style issues](https://github.com/rundeck/rundeck/issues/1095)
* [Export archive does not work](https://github.com/rundeck/rundeck/issues/1094)
* [LDAP auth requests have no timeout](https://github.com/rundeck/rundeck/issues/1092)
* [upload jobs page: after uploading, Delete action from action menu should be available](https://github.com/rundeck/rundeck/issues/1090)
* [List Plugins admin page should show Resource Model Source providers](https://github.com/rundeck/rundeck/issues/1089)
* [Scheduled jobs without a default value for required options fail to run](https://github.com/rundeck/rundeck/issues/1088)
* ['Algorithm negotiation fail'  JSCH](https://github.com/rundeck/rundeck/issues/1087)
* [Upgrade grails to 2.4.4](https://github.com/rundeck/rundeck/pull/1075)
* [Launcher: fix "nohup: redirecting stderr to stdout" warning](https://github.com/rundeck/rundeck/pull/1074)
* [Inline scripts for jobs do not honor File Copy Settings](https://github.com/rundeck/rundeck/issues/1056)
* [Add text/plain MIME type for YAML files](https://github.com/rundeck/rundeck/pull/1043)
* [Support project-specific email template overrides](https://github.com/rundeck/rundeck/pull/1026)
* [Add support for Java 8](https://github.com/rundeck/rundeck/issues/920)
* [aclpolicy validation/builder tool](https://github.com/rundeck/rundeck/issues/859)
* [adding orchestrator](https://github.com/rundeck/rundeck/pull/826)
* [The run shell tool can clobber plugin cache](https://github.com/rundeck/rundeck/issues/808)
* [Allow hybrid LDAP + properties file group authentication](https://github.com/rundeck/rundeck/issues/608)
* [add configurable timeout for remote option URLs](https://github.com/rundeck/rundeck/issues/232)