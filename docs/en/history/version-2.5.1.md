% Version 2.5.1
% greg
% 05/15/2015

Release 2.5.1
=============

Date: 2015-05-15

Name: <span style="color: seagreen"><span class="glyphicon glyphicon-sunglasses"></span> "cafe au lait seagreen sunglasses"</span>

This release has primarily bugfixes.

## Enhancements

* CLI tools can authenticate to the server using environment variables instead of the credentials stored in the framework.properties file.
	* see [CLI Tool Authentication](../man1/cli-tool-authentication.html)

## Upgrading to 2.5

**Note:** We've attempted to maintain compatibility with previous plugins, but due to changes in the core library,
Rundeck may have issues loading or running some plugins.

Please report issues found to [Github issues](https://github.com/rundeck/rundeck/issues).

See the [Upgrading Guide](../upgrading/index.html)

## Contributors

* Greg Schueler (gschueler)

## Bug Reporters

* Zophar78
* andham
* arthurlogilab
* ctgaff
* danifr
* dspinellis
* gschueler
* harlanbarnes
* hbakkum
* jasonaughton
* michlaustn
* mikeleigh
* rveznaver
* schast
* snebel29
* tiahino
* wcooley

## Issues

* [Job with a negative threadcount causes exception when using Orchestrator](https://github.com/rundeck/rundeck/issues/1239)
* [Failed job runs not shown in 'failed' tab](https://github.com/rundeck/rundeck/issues/1234)
* [Information missing from aclpolicy docs](https://github.com/rundeck/rundeck/issues/1232)
* [Saved Node Filters shown in all projects, will show node results from other projects](https://github.com/rundeck/rundeck/issues/1231)
* [Job copy: Data Binding Failed](https://github.com/rundeck/rundeck/issues/1228)
* [Archive import fails if db still has varchar(7) in actionType column](https://github.com/rundeck/rundeck/issues/1227)
* [running rd-project without -p argument causes NPE](https://github.com/rundeck/rundeck/issues/1226)
* [Change to "If a step fails" selection for workflow isn't persisted](https://github.com/rundeck/rundeck/issues/1224)
* [Email Reporting Error On Success](https://github.com/rundeck/rundeck/issues/1221)
* [edit job step when plugin cannot be loaded causes javascript error](https://github.com/rundeck/rundeck/issues/1220)
* [rd-acl create -i doesn't correctly parse node tags in resource section](https://github.com/rundeck/rundeck/issues/1219)
* [rd-acl test with -t tags doesn't work properly](https://github.com/rundeck/rundeck/issues/1218)
* [Rundeck start fails in Tomcat 7 on Win2012](https://github.com/rundeck/rundeck/issues/1216)
* [Error requesting json on API](https://github.com/rundeck/rundeck/issues/1215)
* [Updating job with orchestrator fails](https://github.com/rundeck/rundeck/issues/1212)
* [Incomplete view in Node list](https://github.com/rundeck/rundeck/issues/1208)
* [Limiting execution output through API with lastlines seems to be off by 2](https://github.com/rundeck/rundeck/issues/1207)
* [Rundeck rpm package should conditionally restart the service on upgrade](https://github.com/rundeck/rundeck/issues/1199)
* [No signature of method: rundeck.services.ProjectService.exportProjectToFileAsync() when trying to export project archive with the DB storage type](https://github.com/rundeck/rundeck/issues/1198)
* [Key Storage - DB Backend Exception - Could not obtain current Hibernate Session](https://github.com/rundeck/rundeck/issues/1197)
* [Unable to save changes to jobs using Local Command node step](https://github.com/rundeck/rundeck/issues/1193)
* [2.5 debian package should require java 7](https://github.com/rundeck/rundeck/issues/1192)
* [Bulk Delete Execution Failing](https://github.com/rundeck/rundeck/issues/1184)
* [JettyCombinedLdapLoginModule -\> case sensitve](https://github.com/rundeck/rundeck/issues/1182)
* [Email Notifications Broken - ERROR NotificationService](https://github.com/rundeck/rundeck/issues/1181)
* [Jobs don't complete: Data truncation: Data too long for column 'action_type' at row 1](https://github.com/rundeck/rundeck/issues/1179)
* [rundeck.gui.login.welcomeHtml option is not rendering HTML](https://github.com/rundeck/rundeck/issues/1177)
* [org.h2.jdbc.JdbcSQLException: Feature not supported: "isWrapperFor"](https://github.com/rundeck/rundeck/issues/1175)
* [documentation: workflow node step plugin java interface shown is wrong](https://github.com/rundeck/rundeck/issues/1174)
* [Documentation: Missing execution data attributes on notification plugin page](https://github.com/rundeck/rundeck/issues/1035)
* [Re-write command line tools to support auth w/o properties file.](https://github.com/rundeck/rundeck/issues/137)