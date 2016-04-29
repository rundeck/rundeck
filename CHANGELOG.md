Release 2.6.7
===========

Date: 2016-04-29

Name: <span style="color: goldenrod"><span class="glyphicon glyphicon-plane"></span> "cafe bonbon goldenrod plane"</span>

## Notes

Bug fixes.

## Contributors

* Greg Schueler (gschueler)
* sebastianopilla

## Bug Reporters

* ahonor
* ajxb
* alexey-pankratyev
* amitk75
* david-richer-adsk
* gschueler
* jyaworski
* kmusard
* ltamaster
* sebastianopilla

## Issues

* [Resource Model source errors are not properly shown in config page (2.6.6)](https://github.com/rundeck/rundeck/issues/1830)
* [Directory Resource Issue using JSON files](https://github.com/rundeck/rundeck/issues/1828)
* [2.6.6: when adding a new project, the home page may not load the project action buttons properly](https://github.com/rundeck/rundeck/issues/1821)
* [API: /project/X/resources for invalid project results in 500 instead of 404 response](https://github.com/rundeck/rundeck/issues/1820)
* [Place Jetty work directory outside of the installation tree](https://github.com/rundeck/rundeck/pull/1819)
* [CLI tool: "Response content unexpectedly empty" message](https://github.com/rundeck/rundeck/issues/1818)
* [use separate cli-log4j.properties log4j config for cli tools](https://github.com/rundeck/rundeck/pull/1816)
* [Git Export and Git Import in YAML format issue in 2.6.6](https://github.com/rundeck/rundeck/issues/1815)
* [CLI tools should allow relative urls](https://github.com/rundeck/rundeck/issues/1774)
* [Error in "run" command](https://github.com/rundeck/rundeck/issues/1693)
* [Project configured for Git export becomes unavailable if repo is not accessible](https://github.com/rundeck/rundeck/issues/1624)
* [Log file uses system default file encoding, should force UTF-8](https://github.com/rundeck/rundeck/issues/1619)
* [SCM does not change branches once created](https://github.com/rundeck/rundeck/issues/1515)
* [Retry Failed Nodes does not handle nodes with spaces](https://github.com/rundeck/rundeck/issues/1508)
* [Node name with / character causes GUI execution state to be incorrect](https://github.com/rundeck/rundeck/issues/1457)

---

Release 2.6.6
===========

Date: 2016-04-15

Name: <span style="color: fuchsia"><span class="glyphicon glyphicon-phone"></span> "cafe bonbon fuchsia phone"</span>

## Notes

(Release 2.6.5 was missing some changes noted here, so 2.6.6 includes those changes. 2.6.5 release notes are repeated here.)

Primarily bug fixes and performance improvements.

Notably:

* Startup time improved for Rundeck installs with many (thousands) of projects
* Fixes for node sources and asynchronous node loading
* new healthcheck for database latency

## Contributors

* Bharadwaj P (t20)
* Greg Schueler (gschueler)

## Bug Reporters

* ahonor
* david-gregory-inmar
* fiquett
* gschueler
* hyuan-esol
* jippi
* joshuaspence
* ltamaster
* nilroy
* t20

## Issues

* [healthcheck: add database pingtime healthcheck](https://github.com/rundeck/rundeck/issues/1809)
* [API enhancement: takeover schedule for a single job](https://github.com/rundeck/rundeck/issues/1807)
* [Error connecting to OpenSSH 7.2p2. server](https://github.com/rundeck/rundeck/issues/1797)
* [Throw SSHProtocolFailure error when SSH to a remote machine running OpenSSH 7.2p2](https://github.com/rundeck/rundeck/issues/1796)
* [Job execution error in log after schedule takeover](https://github.com/rundeck/rundeck/issues/1795)
* [Request to /scheduler/takeover for all jobs should skip already owned jobs](https://github.com/rundeck/rundeck/issues/1794)
* [Response for /scheduler/takeover indicates prior owner's server uuid](https://github.com/rundeck/rundeck/issues/1793)
* [SSH: when node has blank hostname, it attempts to connect to localhost.](https://github.com/rundeck/rundeck/issues/1790)
* [rd-jobs list export schedule.time.hour wrong](https://github.com/rundeck/rundeck/issues/1773)
* [Slow startup due to incomplete log storage requests](https://github.com/rundeck/rundeck/issues/1771)
* [(2.6.4) Create project fails after second submit](https://github.com/rundeck/rundeck/issues/1770)
* [fix #1744 first node load is synchronous](https://github.com/rundeck/rundeck/pull/1769)
* [fix #1764 slow home page with many projects](https://github.com/rundeck/rundeck/pull/1765)
* [Slow main page with many projects](https://github.com/rundeck/rundeck/issues/1764)
* [Nodes list error when the remoteUrl is defined](https://github.com/rundeck/rundeck/issues/1760)
* [spurious "removeScriptPluginCache: /var/lib/rundeck/libext/cache/..." message](https://github.com/rundeck/rundeck/issues/1749)
* [GUI enhancement: show owner server UUID for scheduled jobs](https://github.com/rundeck/rundeck/issues/1747)
* [API enhancement: cluster mode ability to find server UUID for scheduled jobs](https://github.com/rundeck/rundeck/issues/1746)
* [Initial resource model loading is not asynchronous](https://github.com/rundeck/rundeck/issues/1744)
* [unable to save jobs when notification plugin used](https://github.com/rundeck/rundeck/issues/1740)
* [Upgrade Apache Commons Collections to v3.2.2](https://github.com/rundeck/rundeck/pull/1736)
* [After 2.6.3 upgrade, nodes are not updated](https://github.com/rundeck/rundeck/issues/1725)
* [Incomplete log file storage request should be cancelled after final retry, or retriable via API](https://github.com/rundeck/rundeck/issues/1719)
* [Listing Running Executions API, "Total" error value using wildcard ("*") in the URL](https://github.com/rundeck/rundeck/issues/1711)
* [In the summary page, a failed node reports failure on the wrong step (2.5.3-1)](https://github.com/rundeck/rundeck/issues/1411)
* [MailNotificationPlugin.groovy does not work with rundeck 2.5.2](https://github.com/rundeck/rundeck/issues/1361)

---

Release 2.6.5
===========

Date: 2016-04-15

Name: <span style="color: forestgreen"><span class="glyphicon glyphicon-paperclip"></span> "cafe bonbon forestgreen paperclip"</span>

## Notes

Primarily bug fixes and performance improvements.

Notably:

* Startup time improved for Rundeck installs with many (thousands) of projects
* Fixes for node sources and asynchronous node loading
* new healthcheck for database latency

## Contributors

* Bharadwaj P (t20)
* Greg Schueler (gschueler)

## Bug Reporters

* ahonor
* david-gregory-inmar
* fiquett
* gschueler
* hyuan-esol
* jippi
* joshuaspence
* ltamaster
* nilroy
* t20

## Issues

* [healthcheck: add database pingtime healthcheck](https://github.com/rundeck/rundeck/issues/1809)
* [API enhancement: takeover schedule for a single job](https://github.com/rundeck/rundeck/issues/1807)
* [Error connecting to OpenSSH 7.2p2. server](https://github.com/rundeck/rundeck/issues/1797)
* [Throw SSHProtocolFailure error when SSH to a remote machine running OpenSSH 7.2p2](https://github.com/rundeck/rundeck/issues/1796)
* [Job execution error in log after schedule takeover](https://github.com/rundeck/rundeck/issues/1795)
* [Request to /scheduler/takeover for all jobs should skip already owned jobs](https://github.com/rundeck/rundeck/issues/1794)
* [Response for /scheduler/takeover indicates prior owner's server uuid](https://github.com/rundeck/rundeck/issues/1793)
* [SSH: when node has blank hostname, it attempts to connect to localhost.](https://github.com/rundeck/rundeck/issues/1790)
* [rd-jobs list export schedule.time.hour wrong](https://github.com/rundeck/rundeck/issues/1773)
* [Slow startup due to incomplete log storage requests](https://github.com/rundeck/rundeck/issues/1771)
* [(2.6.4) Create project fails after second submit](https://github.com/rundeck/rundeck/issues/1770)
* [fix #1744 first node load is synchronous](https://github.com/rundeck/rundeck/pull/1769)
* [fix #1764 slow home page with many projects](https://github.com/rundeck/rundeck/pull/1765)
* [Slow main page with many projects](https://github.com/rundeck/rundeck/issues/1764)
* [Nodes list error when the remoteUrl is defined](https://github.com/rundeck/rundeck/issues/1760)
* [spurious "removeScriptPluginCache: /var/lib/rundeck/libext/cache/..." message](https://github.com/rundeck/rundeck/issues/1749)
* [GUI enhancement: show owner server UUID for scheduled jobs](https://github.com/rundeck/rundeck/issues/1747)
* [API enhancement: cluster mode ability to find server UUID for scheduled jobs](https://github.com/rundeck/rundeck/issues/1746)
* [Initial resource model loading is not asynchronous](https://github.com/rundeck/rundeck/issues/1744)
* [unable to save jobs when notification plugin used](https://github.com/rundeck/rundeck/issues/1740)
* [Upgrade Apache Commons Collections to v3.2.2](https://github.com/rundeck/rundeck/pull/1736)
* [After 2.6.3 upgrade, nodes are not updated](https://github.com/rundeck/rundeck/issues/1725)
* [Incomplete log file storage request should be cancelled after final retry, or retriable via API](https://github.com/rundeck/rundeck/issues/1719)
* [Listing Running Executions API, "Total" error value using wildcard ("*") in the URL](https://github.com/rundeck/rundeck/issues/1711)
* [In the summary page, a failed node reports failure on the wrong step (2.5.3-1)](https://github.com/rundeck/rundeck/issues/1411)
* [MailNotificationPlugin.groovy does not work with rundeck 2.5.2](https://github.com/rundeck/rundeck/issues/1361)

---

Release 2.6.4
===========

Date: 2016-03-04

Name: <span style="color: firebrick"><span class="glyphicon glyphicon-music"></span> "cafe bonbon firebrick music"</span>

## Notes

Bug fixes.

## Contributors

* Greg Schueler (gschueler)

## Bug Reporters

* gschueler
* richiereynolds
* schast
* tkald
* wufpack00

## Issues

* [SCM initialization can cause slow startup with many projects](https://github.com/rundeck/rundeck/issues/1721)
* [java.lang.NullPointerException Rundeck 2.6.3](https://github.com/rundeck/rundeck/issues/1717)
* [After 2.6.3 upgrade, node page doesn't display nodes correctly.](https://github.com/rundeck/rundeck/issues/1716)
* [After 2.6.3 upgrade, ACL that does not allow job create/delete always shows Bulk edit checkboxes](https://github.com/rundeck/rundeck/issues/1714)
* [Error after Upgrade to 2.6.3: Failed loading resource model source, java.lang.IllegalStateException: Recursive load](https://github.com/rundeck/rundeck/issues/1713)

---

Release 2.6.3
===========

Date: 2016-02-25

Name: <span style="color: dodgerblue"><span class="glyphicon glyphicon-leaf"></span> "cafe bonbon dodgerblue leaf"</span>

## Notes

Primarily bug fixes, and some enhancements:

* Asynchronous nodes cache. Per-project and global toggle and configurable retention delay.
By default, projects now use an asynchronous method for	retrieving Node data from Resource Model Sources. 
You should see an improvement in page load when using slow model sources (such as a URL for a slow endpoint).
* Performance improvements with many thousands of Nodes.  The Nodes and Commands page, as well as other places
where Nodes are loaded (e.g. Job editor) now should be much more responsive when you have e.g. 20K nodes.
The Nodes page now uses result paging, and some parts of the UI now truncate the result set as well if you have many nodes.


## Contributors

* Alex Honor (ahonor)
* Bryon Williams
* Greg Schueler (gschueler)
* Luis Toledo
* Miguel A. Fuentes Buchholtz (miguelantonio)
* Rophy Tsai (rophy)
* mathieuchateau

## Bug Reporters

* Alicia-Solinea
* SydOps
* ahonor
* ajxb
* arminioa
* bryonwilliams
* gschueler
* kamaradclimber
* kmusard
* ltamaster
* makered
* mathieuchateau
* obrienmorgan
* rasebo
* richiereynolds
* robizz
* rophy
* schast
* snebel29
* ssbarnea

## Issues

* [toggle job execution/schedule can be triggered with GET without synchronizer token](https://github.com/rundeck/rundeck/issues/1709)
* [Add bulk job edit: enable/disable schedule and execution](https://github.com/rundeck/rundeck/issues/1703)
* [ExecutionJob: Failed to update job statistics](https://github.com/rundeck/rundeck/issues/1701)
* [some links are dead](https://github.com/rundeck/rundeck/issues/1688)
* [Workflow step editor: after deleting a script and hitting save, it disappears](https://github.com/rundeck/rundeck/issues/1686)
* [Code example in documentation incorrect](https://github.com/rundeck/rundeck/issues/1678)
* [Documentation Proposed Change log4j](https://github.com/rundeck/rundeck/pull/1677)
* [Missing step plugin causes execution to show no output](https://github.com/rundeck/rundeck/issues/1673)
* [Orchestrator plugin input cannot use job options](https://github.com/rundeck/rundeck/issues/1672)
* [missing API documentation for job execution and schedule enable/disable](https://github.com/rundeck/rundeck/issues/1670)
* [SCM plugins not shown in Plugins listing page](https://github.com/rundeck/rundeck/issues/1668)
* [Add documentation for changing date formats, and general localization](https://github.com/rundeck/rundeck/issues/1667)
* [Script-based plugin setting for merging parent environment variables](https://github.com/rundeck/rundeck/issues/1666)
* [Feature: Asynchronous nodes cache](https://github.com/rundeck/rundeck/pull/1662)
* [Nodes UI page updates issues](https://github.com/rundeck/rundeck/issues/1658)
* [Secure options with defaults in key storage](https://github.com/rundeck/rundeck/issues/1657)
* [Node page UI updated for large node sets](https://github.com/rundeck/rundeck/pull/1651)
* [rd-acl doesn't support project_acl resource type](https://github.com/rundeck/rundeck/issues/1650)
* [lack of quoting tags links in Nodes browse page](https://github.com/rundeck/rundeck/issues/1647)
* [SCM: GitExportPlugin - Could not serialize job: java.lang.IllegalArgumentException: Format not supported: yaml](https://github.com/rundeck/rundeck/issues/1644)
* [SCM: NoSuchFileException](https://github.com/rundeck/rundeck/issues/1642)
* [Passive mode - more explicit for already running job](https://github.com/rundeck/rundeck/pull/1635)
* [Enhance project archive API: select only a set of executions for output](https://github.com/rundeck/rundeck/pull/1621)
* [Added CODE renderingType to allow plugins to render textareas with th…](https://github.com/rundeck/rundeck/pull/1620)
* [execution query api for older than relative date](https://github.com/rundeck/rundeck/pull/1617)
* [fix #1525 project storage file read should not close stream](https://github.com/rundeck/rundeck/pull/1615)
* [fix #1611](https://github.com/rundeck/rundeck/pull/1614)
* [500 http response when execution does not exist](https://github.com/rundeck/rundeck/issues/1611)
* [Invalid username and password message displayed on a successful login which was preceeded by a failed login](https://github.com/rundeck/rundeck/issues/1610)
* [Querying 5k nodes leads to very long queries on the webui](https://github.com/rundeck/rundeck/issues/1597)
* [fix long line wrapping for log output #1558](https://github.com/rundeck/rundeck/pull/1596)
* [Multiple Authentication Modules documentation error](https://github.com/rundeck/rundeck/issues/1593)
* [The size (8192) given to the column 'json_data' exceeds the maximum allowed for any data type (8000)](https://github.com/rundeck/rundeck/issues/1579)
* [Document how to grant via ACL for Per-Job schedule and execution toggling](https://github.com/rundeck/rundeck/issues/1578)
* [Console output is not wrapped making impossible to see long lines](https://github.com/rundeck/rundeck/issues/1558)
* [Export project is not working when using the GUI - 2.6.0-1](https://github.com/rundeck/rundeck/issues/1525)
* [Cancel/Save options on workflow step become unresponsive after error](https://github.com/rundeck/rundeck/issues/1448)
* [Prevents remote option spam in issue 1391 by using onChange event](https://github.com/rundeck/rundeck/pull/1415)
* [Log Output gone mad in 2.5.2](https://github.com/rundeck/rundeck/issues/1368)
* [Simultaneously triggered jobs sometimes hang](https://github.com/rundeck/rundeck/issues/1310)
* [Job timeouts before actual timeout value](https://github.com/rundeck/rundeck/issues/1302)

---

Release 2.6.2
===========

Date: 2015-12-02

Name: <span style="color: crimson"><span class="glyphicon glyphicon-headphones"></span> "cafe bonbon crimson headphones"</span>

## Notes

This release includes bug fixes, and some enhancements.

* Secure Job Options can now use Key Storage for defaults, enabling use via scheduled jobs
* Quartz thread pool info and warnings added to system info
* API added for SCM plugins, API version updated to v15
* Plugins: input property definitions can now be placed in groups, which can be shown collapsed by default

## Contributors

* Greg Schueler (gschueler)

## Bug Reporters

* ahonor
* gschueler
* sea-lmarchal
* stack72

## Issues

* [Feature/multifile storage](https://github.com/rundeck/rundeck/pull/1560)
* [SCM API: push via project-commit action not working](https://github.com/rundeck/rundeck/issues/1553)
* [SCM API: plugin type not checked for enable/disable](https://github.com/rundeck/rundeck/issues/1552)
* [SSH debug logging is missing](https://github.com/rundeck/rundeck/issues/1546)
* [plugin properties can be grouped](https://github.com/rundeck/rundeck/pull/1543)
* [Feature: secure option can use key store for default values](https://github.com/rundeck/rundeck/pull/1537)
* [for #1535 allow rundeck.basedir var in remote option file urls](https://github.com/rundeck/rundeck/pull/1536)
* [Expand ${rdeck.base} for remoteUrl paths for options](https://github.com/rundeck/rundeck/issues/1535)
* [2.6: startup message "Event listener rundeck.services.JobEventsService#jobChanged declared for topic jobChanged and namespace app but no such event is declared, you may never receive it"](https://github.com/rundeck/rundeck/issues/1532)
* [Add Quartz Scheduler threadPool usage to metrics healthcheck, system info](https://github.com/rundeck/rundeck/pull/1530)
* [Startup: ERROR level log: "ScheduledExecutionService - rescheduled job: 335"](https://github.com/rundeck/rundeck/issues/1529)
* [Job succeeds instead of failing when executing a job-ref at the end](https://github.com/rundeck/rundeck/issues/1528)
* [API for SCM plugins](https://github.com/rundeck/rundeck/pull/1526)
* [SCM: add API support for SCM plugins](https://github.com/rundeck/rundeck/issues/1516)
* [Error Deleting a Project](https://github.com/rundeck/rundeck/issues/1436)
---

Release 2.6.1
===========

Date: 2015-10-23

Name: <span style="color: cornflowerblue"><span class="glyphicon glyphicon-globe"></span> "cafe bonbon cornflowerblue globe"</span>

## Fixes

Bugfixes for SCM plugins, and the Job execution/schedule toggle feature.

## Contributors

* Greg Schueler (gschueler)
* Miguel A. Fuentes Buchholtz (miguelantonio)
* robertopaez

## Bug Reporters

* LordMike
* albertmfb
* dustinak
* fseiftsmlb
* gschueler
* jyaworski
* katanafleet
* miguelantonio
* mprasil
* oovoo
* paulpet
* rophy
* tomkregenbild

## Issues

* [SCM plugin does not interpret variables in paths](https://github.com/rundeck/rundeck/issues/1510)
* [CLI: Running rd-jobs list gives an error when user profile is incomplete and SCM is enabled](https://github.com/rundeck/rundeck/issues/1509)
* [Upgrade to 2.6.0: schedule/execution enabled defaults to No](https://github.com/rundeck/rundeck/issues/1502)
* [changes implementation of flipScheduleEnabled and flipExecutionEnable…](https://github.com/rundeck/rundeck/pull/1501)
* [SCM: import while export plugin enabled can cause stacktrace/empty file](https://github.com/rundeck/rundeck/issues/1499)
* [SCM synch rebase: if result is conflicted, it should be aborted](https://github.com/rundeck/rundeck/issues/1497)
* [SCM: yaml whitespace error on re-import](https://github.com/rundeck/rundeck/issues/1496)
* [SCM "Import remote changes"-cancel button leads to SCM configuration](https://github.com/rundeck/rundeck/issues/1494)
* [SCM rebase gives a non-descriptive message](https://github.com/rundeck/rundeck/issues/1493)
* [SCM imports reads as XML even though YAML has been chosen](https://github.com/rundeck/rundeck/issues/1492)
* [SCM: File Path Template Parameter issue ](https://github.com/rundeck/rundeck/issues/1489)
* [SCM: enabling git export and import, then exporting job changes, causes "import needed" status for the job](https://github.com/rundeck/rundeck/issues/1488)
* [SCM: Job description with multiple lines can cause whitespace issues for git-diff](https://github.com/rundeck/rundeck/issues/1487)
* [SCM: Merge result message is verbose](https://github.com/rundeck/rundeck/issues/1486)
* [SCM Plugins: deleting a project should cleanup/remove loaded SCM plugins](https://github.com/rundeck/rundeck/issues/1484)
* [Upgrading from 2.4.x to 2.6.x, all jobs have been set to "Enable Execution: no"](https://github.com/rundeck/rundeck/issues/1483)
* [SCM export plugins: job change events leak to multiple projects](https://github.com/rundeck/rundeck/issues/1479)
* [SCM: setup two projects with same git base dir causes issues](https://github.com/rundeck/rundeck/issues/1478)
* [Upgrade to 2.6.0: Node selection defaults to "user has to explicitly select target nodes"](https://github.com/rundeck/rundeck/issues/1477)
* [When disabling a crontab style schedule and restarting the rundeck service, the schedule is enabled again after service restart.](https://github.com/rundeck/rundeck/issues/1475)
* [scm export commits yaml format in .xml extension](https://github.com/rundeck/rundeck/issues/1471)
* [SCM HTTPS, unknown CA](https://github.com/rundeck/rundeck/issues/1469)
* [Disable/Enable Schedule in Job Actions menu (Rundeck 2.6.0-1) ](https://github.com/rundeck/rundeck/issues/1468)
* [problem closing file descriptors unloading plugins in rundeck](https://github.com/rundeck/rundeck/issues/1440)
* [Deleting execution from api is not always working (random errors)](https://github.com/rundeck/rundeck/issues/1380)
* [documentation needed: using the sudo password and ssh private key passphrase via storage facility for ssh](https://github.com/rundeck/rundeck/issues/1110)
---

Release 2.6.0
===========

Date: 2015-10-08

Name: <span style="color: chocolate"><span class="glyphicon glyphicon-gift"></span> "cafe bonbon chocolate gift"</span>

## New Features

* SCM Plugin with Git implementation (preliminary release)
* Per-job logging limits, such as max line count or file size, see [Jobs - Log Limit](http://rundeck.org/2.6.0/manual/jobs.html#log-limit)
* Active/passive execution mode: disable all executions on the server
* Per-Job schedule and execution toggling: disable scheduled or all executions for a single job
* UI updates to Nodes and Commands pages
* Project Archives: includes ACLs and project config
* Per-project ACLs available via API, storable in DB
	* Filesystem ACLs are now used for "global" level access control
	* Project ACLs are limited to manage the specific project, and are stored in the DB
* ACL validation improvements
	* see the `rd-acl` tool [validate command](http://rundeck.org/2.6.0/man1/rd-acl.html#validate-command)
	* Invalid filesystem ACLs are logged in the service log
	* Invalid project ACLs uploaded via API will be rejected
* Example Orchestrator plugins now bundled
* JSON support added for all API endpoints
* Some API refactoring for v14
* See [API v14](http://rundeck.org/2.6.0/api/index.html)
* Jobs can now disable automatic node selection by default
* Phew

## Upgrading


Upgrading from 2.5 should not cause any issues. Some new database fields were added, and a new database table was added.

## Compatibility

Some changes to Job serialization formats:

* The project name is no longer included in exported Job definitions.
* In YAML: options are now always serialized as a sequence, and will preserve the sequence order on input. The original Map format is still allowed on import.


## Contributors

* Alex Honor (ahonor)
* Francois Travais
* Greg Schueler (gschueler)
* Mathieu Chateau (mathieuchateau)
* Miguel Fuentes (miguelantonio)
* Roberto Paez (robertopaez)
* William Jimenez (wjimenez5271)
* maciejs

## Bug Reporters

* adamhamner
* ahonor
* chadlnc
* ctgaff
* ddzed15
* francois-travais
* gschueler
* hipslu
* ko-christ
* maciejs
* mathieuchateau
* miguelantonio
* snebel29
* sylvainr
* wjimenez5271

## Issues

* [SCM Plugin with Git support](https://github.com/rundeck/rundeck/pull/1465)
* [Log limit](https://github.com/rundeck/rundeck/pull/1453)
* [Add Job Logging Limit options: allow a job to abort/fail or truncate logging if too much output occurs](https://github.com/rundeck/rundeck/issues/1452)
* [GUI improvements for schedule and execution toggle](https://github.com/rundeck/rundeck/pull/1431)
* [XML Export of Jobs with inline scripts fails](https://github.com/rundeck/rundeck/issues/1429)
* [Disable Compatiblity Mode in Internet Explorer: Set X-UA-Compatible](https://github.com/rundeck/rundeck/pull/1423)
* [jobs never complete in 2.5.3 (sudoPassword option is necessary)](https://github.com/rundeck/rundeck/issues/1422)
* [UI Improvments: Nodes page](https://github.com/rundeck/rundeck/pull/1412)
* [project config import: needs to reset project.name](https://github.com/rundeck/rundeck/issues/1400)
* [UI improvements: Commands page](https://github.com/rundeck/rundeck/pull/1398)
* [Authentication Documentation Update](https://github.com/rundeck/rundeck/issues/1389)
* [JS Error: Object doesn't support property or method 'updateError' under IE11](https://github.com/rundeck/rundeck/issues/1383)
* [Add export/import of project config and ACL policy files in project archives](https://github.com/rundeck/rundeck/pull/1381)
* [APIs for ACL policy, project-specific policies, ACLs stored in DB](https://github.com/rundeck/rundeck/pull/1379)
* [Disable schedule and disable job execution: GUI and functionality](https://github.com/rundeck/rundeck/pull/1377)
* [Documentation: MySQL connector download not usually necessary](https://github.com/rundeck/rundeck/pull/1373)
* [2.5.2-1 Project Description isn't removed properly when using "Simple Configuration"](https://github.com/rundeck/rundeck/issues/1366)
* [Need canonical XML serialization for jobs](https://github.com/rundeck/rundeck/issues/1350)
* [Enhance cluster mode schedule takeover API](https://github.com/rundeck/rundeck/pull/1344)
* [Enable multiple config locations](https://github.com/rundeck/rundeck/issues/1339)
* [update to docs for #1333](https://github.com/rundeck/rundeck/pull/1336)
* [Enhance takeover schedule API: all jobs, or by project](https://github.com/rundeck/rundeck/issues/1332)
* [Active/passive execution mode](https://github.com/rundeck/rundeck/issues/1327)
* [Promote example orchestrator plugins to bundled plugins](https://github.com/rundeck/rundeck/issues/1275)
* [add JSON support to API](https://github.com/rundeck/rundeck/pull/1262)
* [add a job option to disable the automatic selection of all nodes](https://github.com/rundeck/rundeck/pull/1245)
* [documentation: update command for creating encrypted/hashed passwords for jetty](https://github.com/rundeck/rundeck/issues/1222)
* [use JSON throughout API](https://github.com/rundeck/rundeck/issues/1109)
* [Api endpoint like /incubator/jobs/takeoverSchedule but for all jobs](https://github.com/rundeck/rundeck/issues/1028)
* [Disabling scheduling without loosing scheduling configuration](https://github.com/rundeck/rundeck/issues/830)
* [Add job option to have nodes default to un-checked](https://github.com/rundeck/rundeck/issues/114)
---

Release 2.5.3
===========

Date: 2015-08-12

Name: <span style="color: slateblue"><span class="glyphicon glyphicon-book"></span> "cafe au lait slateblue book"</span>

* Bug fixes

## Contributors

* Greg Schueler (gschueler)

## Bug Reporters

* ahonor
* ddzed15
* gschueler
* runnnt0
* tomkregenbild

## Issues

* [${option.wimrmPassword}  not passed between jobs in 2.5.2 version. ( was working in 2.5.1)](https://github.com/rundeck/rundeck/issues/1363)
* [Jaas login module using LDAPS can cause exception "Unconnected sockets not implemented"](https://github.com/rundeck/rundeck/issues/1356)
* [version 2.5.2 - Option 'sshKeyPassphrase' is required when running with job reference](https://github.com/rundeck/rundeck/issues/1355)
* [Documentation: Yum install instructions refer to wrong URL](https://github.com/rundeck/rundeck/issues/1345)
* [Node Filter documentation is incorrect](https://github.com/rundeck/rundeck/issues/1338)
* [Incorrect value for server url context variable](https://github.com/rundeck/rundeck/issues/1337)
* [Only required attribute for yaml should be nodename](https://github.com/rundeck/rundeck/issues/1335)
* [Fix #1328](https://github.com/rundeck/rundeck/pull/1329)
* [API: execution output in text format is encoded](https://github.com/rundeck/rundeck/issues/1328)
* [Disallow blank password](https://github.com/rundeck/rundeck/issues/1273)

---

Release 2.5.2
===========

Date: 2015-07-07

Name: <span style="color: sienna"><span class="glyphicon glyphicon-bell"></span> "cafe au lait sienna bell"</span>

* Improved performance on Execution follow page with many nodes (1000+).
* Bug fixes

## Contributors

* Greg Schueler (gschueler)
* Noah Lorang (noahhl)

## Bug Reporters

* andham
* blysik
* corecache
* ctgaff
* fanie-riekert
* gschueler
* jacekkow
* jeffearl
* mathieuchateau
* mgsococo
* noahhl
* richiereynolds
* stack72
* touzoku

## Issues

* [Enable the configuration of excluded cipher suites for SSL](https://github.com/rundeck/rundeck/issues/1321)
* [GUI: Plugins List in Configure page: plugin provider names should be shown](https://github.com/rundeck/rundeck/issues/1320)
* [GUI: Workflow editor: Step plugins list should be sorted](https://github.com/rundeck/rundeck/issues/1319)
* [GUI: workflow plugin text incorrect](https://github.com/rundeck/rundeck/issues/1318)
* [GUI: Execution File Storage Plugin: configuration listing is missing data](https://github.com/rundeck/rundeck/issues/1317)
* [Job Fails after 0s](https://github.com/rundeck/rundeck/issues/1314)
* [GUI: workflow strategy help text is not readable](https://github.com/rundeck/rundeck/issues/1313)
* [2.5.1.1 - hoovering on tooltip, no way to make it disappear](https://github.com/rundeck/rundeck/issues/1307)
* [Documentation: Storage API for creating Password doesn't mention MIME type to use](https://github.com/rundeck/rundeck/issues/1297)
* [Archive Import: can fail for some execution data with missing reports](https://github.com/rundeck/rundeck/issues/1296)
* [orchestrator config not shown for executions](https://github.com/rundeck/rundeck/issues/1295)
* [#2.5.1 #debianpkg rd-acl is missing executable flag ](https://github.com/rundeck/rundeck/issues/1293)
* [Rundeck 2.5.1 doesn't capture/display last line of output if it is not terminated by a newline](https://github.com/rundeck/rundeck/issues/1292)
* [Archive Export should not use default JVM character encoding: can cause error on re-import](https://github.com/rundeck/rundeck/issues/1289)
* [Archive Export: empty project (0 jobs, 0 activity), causes exception](https://github.com/rundeck/rundeck/issues/1288)
* [Archive import: Invalid XML in execution/report.xml file causes import to halt with error](https://github.com/rundeck/rundeck/issues/1287)
* [Key Storage DB backend with empty DB shows warning message](https://github.com/rundeck/rundeck/issues/1286)
* [support secure auth password job option for script-based node executors](https://github.com/rundeck/rundeck/issues/1285)
* [Error importing archive: executions for deleted jobs which use job references cause error at Report import stage](https://github.com/rundeck/rundeck/issues/1284)
* [error: import archive with executions and orchestrator](https://github.com/rundeck/rundeck/issues/1282)
* [email address validation on job import should not restrict domain TLDs](https://github.com/rundeck/rundeck/issues/1281)
* [Fix next execution time on non-scheduled cluster member](https://github.com/rundeck/rundeck/pull/1280)
* [signed debian file build fix](https://github.com/rundeck/rundeck/issues/1279)
* [Add launcher to sonatype maven repo deploy](https://github.com/rundeck/rundeck/issues/1274)
* [Options passed in "Job Reference" step are not being replaced by empty strings](https://github.com/rundeck/rundeck/issues/1272)
* [File extension specified not used in URL script file execution](https://github.com/rundeck/rundeck/issues/1264)
* [Service Unavailable on startup 2.5.1-1.7GA installed from rpm](https://github.com/rundeck/rundeck/issues/1252)
* [Notification validation: require value for email/webhook](https://github.com/rundeck/rundeck/issues/1250)
* ["On start" webhook notification selection persisted incorrectly](https://github.com/rundeck/rundeck/issues/1249)
* [quartz.properties no longer exists](https://github.com/rundeck/rundeck/issues/1210)
* [Blank options passed to sub jobs as literal strings, RD 2.2.3](https://github.com/rundeck/rundeck/issues/992)
* [large rundeck jobs unusable in current UI](https://github.com/rundeck/rundeck/issues/822)

---

Release 2.5.1
=============

Date: 2015-05-15

Name: <span style="color: seagreen"><span class="glyphicon glyphicon-sunglasses"></span> "cafe au lait seagreen sunglasses"</span>

This release has primarily bugfixes.

## Enhancements

* CLI tools can authenticate to the server using environment variables instead of the credentials stored in the framework.properties file.
	* see [CLI Tool Authentication](http://rundeck.org/2.5.1/man1/cli-tool-authentication.html)

## Upgrading to 2.5

**Note:** We've attempted to maintain compatibility with previous plugins, but due to changes in the core library,
Rundeck may have issues loading or running some plugins.

Please report issues found to [Github issues](https://github.com/rundeck/rundeck/issues).

See the [Upgrading Guide](http://rundeck.org/2.5.1/upgrading/index.html)

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
---

Release 2.5.0
=============

Date: 2015-04-15

Name: <span style="color: saddlebrown"><span class="glyphicon glyphicon-grain"></span> "cafe au lait saddlebrown grain"</span>

This release has many new features and bugfixes, and contains some refactoring under the hood.

## New Features

* Upgraded grails framework, which adds support for Java 8 (and removes Java 6)
* Can now store Project definition and configuration in the database (optional), see [Project Definitions](http://rundeck.org/2.5.0/administration/project-setup.html#project-definitions) and information in the [Upgrade Guide](http://rundeck.org/2.5.0/upgrading/index.html).
* Improved GUI for administering a project: modify Project configuration file contents, and readme.md/motd.md contents. Project archiving is also now performed asynchronously.
* New Jasypt encryption plugin can be used for Key Storage and Project Configuration, see [Jasypt Encryption Converter Plugin](http://rundeck.org/2.5.0/plugins-user-guide/storage-plugins.html#jasypt-encryption-converter-plugin)
* Support Key Storage password for SSH sudo and SSH private key passphrase
* New workflow step plugin: Assert state of another Job, optionally halt workflow with custom status string
* New `rd-acl` commandline tool can list test and generate .aclpolicy file contents, see [rd-acl](http://rundeck.org/2.5.0/man1/rd-acl.html)
* New Orchestrator plugin point. Orchestrators can be used to batch and sequence the matched nodes used in node dispatching, e.g.: only allow a certain % to run simultaneously.
	* [Plugins User Guide -  Orchestrator Plugins](http://rundeck.org/2.5.0/plugins-user-guide/orchestrator-plugins.html)
	* [Plugin Developer Guide -  Orchestrator Plugin](http://rundeck.org/2.5.0/developer/orchestrator-plugin.html)
	* [Example code](https://github.com/rundeck/rundeck/tree/development/examples/example-java-orchestrator-plugin)
* Added hybrid LDAP + properties file group authentication for JAAS (`JettyCombinedLdapLoginModule`)
	* [Administration - Authentication - Combining LDAP with other modules](http//rundeck.org/2.5.0/administration/authenticating-users.html#combining-ldap-with-other-modules)

## Upgrading

**Note:** We've attempted to maintain compatibility with previous plugins, but due to changes in the core library,
Rundeck may have issues loading or running some plugins.

Please report issues found to [Github issues](https://github.com/rundeck/rundeck/issues).

See the [Upgrading Guide](http://rundeck.org/2.5.0/upgrading/index.html)

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
---

Release 2.4.1
=============

Date: 2015-01-30

Name: <span style="color: limegreen"><span class="glyphicon glyphicon-bullhorn"></span> "americano limegreen bullhorn"</span>

This release has bug fixes.

## Compatibility notes

See the [release notes for v2.4.0](http://rundeck.org/2.4.0/history/version-2.4.0.html)

## Contributors

* ETAI Opérations
* Greg Schueler (gschueler)
* Mark Bidewell (mbidewell)

## Bug Reporters

* Farzy
* bdmorin
* gschueler
* jedblack
* mbidewell
* mezbiderli
* mgsococo
* mika
* richiereynolds
* russellballestrini
* smithtimamy

## Issues

* [Fix ssh-agent variable names in documentation](https://github.com/rundeck/rundeck/pull/1093)
* [Activity page/tabs: events for deleted jobs appear incorrectly, options for other jobs hidden](https://github.com/rundeck/rundeck/issues/1091)
* [Run again with multi-valued enforced option doesn't select values](https://github.com/rundeck/rundeck/issues/1086)
* [Enter \<cr\> does not work under the activity view for a project ](https://github.com/rundeck/rundeck/issues/1084)
* [SSH plugin appends ^M to scripts, failing jobs to fail](https://github.com/rundeck/rundeck/issues/1082)
* [timeout in job; rundeck doesn't retry](https://github.com/rundeck/rundeck/issues/1079)
* [User domain login field has too restrictive regex](https://github.com/rundeck/rundeck/issues/1078)
* [2.4.0: upload jobs page: after uploading, action menus for new jobs are empty](https://github.com/rundeck/rundeck/issues/1066)
* [2.4.0: job upload form radio buttons in wrong location](https://github.com/rundeck/rundeck/issues/1065)
* [ERROR FrameworkProject - Cannot get nodes from \[DirectoryResourceModelSource](https://github.com/rundeck/rundeck/issues/1064)
* [2.4.0 script plugin support ignoring the output of the file copier provider script](https://github.com/rundeck/rundeck/issues/1060)
* [Script plugin no longer works after upgrading from 2.2.3 to 2.4.0](https://github.com/rundeck/rundeck/issues/1059)
* [As an end user, I would like specific error messages when job imports fail](https://github.com/rundeck/rundeck/issues/1058)
* [Add destfilename expansion which contains the destinitation filename](https://github.com/rundeck/rundeck/pull/1057)
* [Number of nodes wrong on "Run Again" in 2.2.1, even more wrong in 2.2.3!](https://github.com/rundeck/rundeck/issues/991)
* [Cascaded Options Don't Display Value on "Run Again" in v2.2.3, was fine in 2.2.1](https://github.com/rundeck/rundeck/issues/990)
* [/tmp/rundeck has wrong permissions (Debian package)](https://github.com/rundeck/rundeck/issues/937)

---

Release 2.4.0
=============

Date: 2014-12-16

Name: <span style="color: indigo"><span class="glyphicon glyphicon-briefcase"></span> "americano indigo briefcase"</span>

This release has bug fixes and new features, including some GUI improvements.

## New Features

* Job references can override Node Filters. See updates to [XML](../man5/job-xml.html#jobref) and [YAML](../man5/job-yaml.html#job-reference-entry) job definition formats as well.
* Job and Option descriptions can contain markdown and HTML. For Jobs, the first line is the short description, and following lines are interpreted as markdown.
* MSSQL compatibility
* Some GUI tweaks and changes
    * A new Action menu is available next to the Job name in job listing and view pages.  Actions include Edit, Duplicate, Delete, and download XML/YAML definitions.
    * The Delete Job link in the Job Edit page has been removed
    * The Job Edit link in job lists has been replaced with an Action menu
    * Hovering on job name in job lists now triggers the detail popup after a slight delay. Previously it was triggered by hovering on the Edit link.
    * Execution follow page layout has been rearranged
    * Added extended Job descriptions (sanitized Markdown/html)
    * Job Edit/Create page: you are now asked to confirm navigation away from the page if you have made changes to the Job

## Incubator features

* Parallel step execution
    - this can be enabled with `rundeck-config.properties` entry:

            feature.incubator.parallelWorkflowStrategy=true

* ssh-agent forwarding for ssh connections
    - this can be enabled per node, project, or server
        framework.properties:

            framework.local.ssh-agent=<true|false>
            framework.local.ttl-ssh-agent=<time in sec>

        project.properties:

            project.local.ssh-agent=<true|false>
            project.local.ttl-ssh-agent=<time in sec>

        Node properties:

            local-ssh-agent=<true|false>
            local-ttl-ssh-agent=<time in sec>

## Compatibility notes


A bug in API v11 XML responses caused them to sometimes be incorrectly wrapped in a `<result>` element, this has now been corrected.  See the [API Docs](../api/index.html) for information.  The Rundeck API Java Client library has been updated to workaround this issue (for previous versions of Rundeck).

## What is "americano indigo briefcase"?

New versions of Rundeck will have a name based on the version number. The 2.x theme is Coffee, and 2.4.x is "americano". The point release defines a combination of color and icon we can display in the GUI for easier visual differentiation. 2.4.0 is "indigo briefcase".

> Why yes, I'd like an americano, thank you.

## Contributors

* Alex Honor (ahonor)
* Greg Schueler (gschueler)
* Jason (jasonhensler)
* Jonathan Li (onejli)
* Mathieu Payeur Levallois (mathpl)
* Ruslan Lutsenko (lruslan)
* mezbiderli

## Bug Reporters

* Whitepatrick
* adamhamner
* ahonor
* danifr
* davealbert
* foundatron
* gmichels
* gschueler
* jasonhensler
* jcmoraisjr
* katanafleet
* lruslan
* mathpl
* mezbiderli
* new23d
* onejli
* ujfjhz
* zarry

## Issues

* [Cancel editing resource model source doesn't work](https://github.com/rundeck/rundeck/issues/1051)
* [Job run form triggered from Jobs list page incorrectly shows next scheduled time as "never"](https://github.com/rundeck/rundeck/issues/1044)
* [Nodes yaml format: if attribute values are not strings, throws exception](https://github.com/rundeck/rundeck/issues/1039)
* [Rundeck under Tomcat7 dump a lot a of serialization Warning ](https://github.com/rundeck/rundeck/issues/1036)
* [Project Config permission needs Project Create ACL](https://github.com/rundeck/rundeck/issues/1031)
* [ssh-agent forwarding limited to job execution](https://github.com/rundeck/rundeck/pull/1029)
* [Parameterize grails central](https://github.com/rundeck/rundeck/pull/1027)
* [Failure saving project config when empty password field value is used](https://github.com/rundeck/rundeck/issues/1025)
* [Job references can override Node filters](https://github.com/rundeck/rundeck/pull/1024)
* [Allow markup in job and option descriptions](https://github.com/rundeck/rundeck/issues/1020)
* [script-based file copier plugin fails to load project/framework configuration](https://github.com/rundeck/rundeck/issues/1018)
* [Add server uuid element to the /system/info endpoint](https://github.com/rundeck/rundeck/issues/1017)
* [Project config: plugins with same property names will render same values](https://github.com/rundeck/rundeck/issues/1016)
* ["Copy file" step moves file rather than copy ](https://github.com/rundeck/rundeck/issues/1015)
* [Do not look further if nodefilter.dispatch is not set](https://github.com/rundeck/rundeck/pull/1013)
* [rundeck.gui.login.welcome no longer allows html tags like <b></b> to make all or part of the welcome message bold.](https://github.com/rundeck/rundeck/issues/1012)
* [Fix API v11 xml wrapper responses](https://github.com/rundeck/rundeck/pull/1010)
* [Improve validation messages during job import](https://github.com/rundeck/rundeck/issues/1009)
* [APIv11 responses should not include <result> element](https://github.com/rundeck/rundeck/issues/1008)
* [ux: clicking "Top" link when browsing jobs in a group takes you to the rundeck home page](https://github.com/rundeck/rundeck/issues/999)
* [Add MSSQL Support](https://github.com/rundeck/rundeck/pull/972)
* [job.serverUrl not available in reference job.](https://github.com/rundeck/rundeck/issues/965)
* [HMAC request tokens expiring prematurely: "request did not include a valid token"](https://github.com/rundeck/rundeck/issues/960)
* [Issues/927 parallel execution](https://github.com/rundeck/rundeck/pull/929)
* [Delete execution link should not be shown while execution is running](https://github.com/rundeck/rundeck/issues/891)
* [Editing two workflow steps is unsupported](https://github.com/rundeck/rundeck/issues/849)
* [Rundeck using MSSQL datasource](https://github.com/rundeck/rundeck/issues/848)
* [URL encode ${option.[name].value} in Cascading Remote Options](https://github.com/rundeck/rundeck/issues/811)
* [Job editor: don't allow user to lose changes](https://github.com/rundeck/rundeck/issues/254)
* [Cannot use UTF8 in rundeck ](https://github.com/rundeck/rundeck/issues/222)
* [Jobref calls should support overriding node filter params](https://github.com/rundeck/rundeck/issues/131)

---

Release 2.3.2
=============

Date: 2014-11-06

Fix more regressions in executing remote inline script steps.

See [2.3.0 release notes](http://rundeck.org/docs/history/version-2.3.0.html).

## Contributors

* Alex Honor (ahonor)
* Greg Schueler (gschueler)

## Bug Reporters

* ahonor
* gschueler
* ko-christ
* mumblez

## Issues

* [dispatch -s scriptfile -- args fails](https://github.com/rundeck/rundeck/issues/1006)
* [Create email settings page in admin guide](https://github.com/rundeck/rundeck/issues/1004)
* [that ^M aka CRLF line terminators](https://github.com/rundeck/rundeck/issues/1003)
* [node attributes not expanded in inline scripts in 2.3.1](https://github.com/rundeck/rundeck/issues/1001)
---

Release 2.3.1
=============

Date: 2014-10-31

Fix a regression in executing local inline script steps.

See [2.3.0 release notes](http://rundeck.org/docs/history/version-2.3.0.html).

## Contributors

* Greg Schueler (gschueler)

## Bug Reporters

* ko-christ
* richiereynolds

## Issues

* [Options not expanded in inline scripts in 2.3.0](https://github.com/rundeck/rundeck/issues/994)
* [bad interpreter error](https://github.com/rundeck/rundeck/issues/993)
---

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
    [Key Storage](http://rundeck.org/docs/administration/key-storage.html).
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

---

Release 2.2.3
=============

Date: 2014-09-24

Fix several issues found in 2.2.2:

* [2.2.2: Workflow editor drag/drop or step delete doesn't work](https://github.com/rundeck/rundeck/issues/943)
* [Documentation: Sudo password option type incorrect: should specify Secure Remote Authentication option](https://github.com/rundeck/rundeck/issues/940)
* [plugin development: plugin properties using rendering options should allow String values](https://github.com/rundeck/rundeck/issues/939)

Release notes from 2.2.2 follow:

This release fixes a number of bugs and addresses several potential security issues:

1. Require a unique token for each form request from the GUI, which prevents replay and CSRF attacks
2. Updated all pages to prevent unencoded data from being written to the response, preventing XSS style attacks.
3. Prevent access to the /api URLs via the web GUI.
4. Some plugins (Resource model, Node Executor and File Copier) now support using Password fields displayed in the Project config page. The field values once set are never revealed in clear text via the GUI.

Please see the Notes below for some configuration information
related to these changes.

**A big Thank You to one of our clients for sponsoring the work for these enhancements.**

*Security Notes:*

The new form tokens used in all form requests
by default will expire in 30 minutes.
This means that if your session timeout is larger than 30 minutes
and you attempt to e.g. run a job
after your web page has been sitting open for longer than that,
you will see an "Invalid token" error.
If this becomes a problem for you
you can either change the expiration time for these tokens,
or switch to using non-expiring tokens.
See [Administration - Configuration File Reference - Security](http://rundeck.org/2.2.2/administration/configuration-file-reference.html#security).

To add a Password field definition to your plugin, 
see [Plugin Development - Description Properties](http://rundeck.org/2.2.2/developer/plugin-development.html#description-properties). 
(Note that currently using property annotations is not supported 
for the three plugin types that can use Password properties.)

*Upgrade notes:* 

See the [Upgrading Guide](http://rundeck.org/2.2.2/upgrading/index.html).


## Contributors

* Andreas Knifh (knifhen)
* Daniel Serodio (dserodio)
* Greg Schueler (gschueler)

## Bug Reporters

* adolfocorreia
* ahonor
* arjones85
* danpilch
* dennis-benzinger-hybris
* dserodio
* garyhodgson
* gschueler
* jerome83136
* knifhen
* majkinetor
* rfletcher
* schicky

## Issues

* [dynamic node filter string incorrectly includes name: prefix](https://github.com/rundeck/rundeck/issues/934)
* [aclpolicy files are listed in random order in Configure page](https://github.com/rundeck/rundeck/issues/931)
* [Improve "Authenticating Users" docs re. logging](https://github.com/rundeck/rundeck/pull/925)
* [Security: allow plugins to specify password properties that are obscured in project config page](https://github.com/rundeck/rundeck/pull/919)
* [Job Variable Length is too low](https://github.com/rundeck/rundeck/issues/915)
* [Config toggle: Hide error page stacktrace](https://github.com/rundeck/rundeck/pull/910)
* [Security: CSRF prevention](https://github.com/rundeck/rundeck/pull/909)
* [Security: prevent XSS issues](https://github.com/rundeck/rundeck/pull/908)
* [Cannot pass multiple values to multivalued option with enforced values](https://github.com/rundeck/rundeck/issues/907)
* [Rundeck 2.1.1 scheduling bug](https://github.com/rundeck/rundeck/issues/905)
* [Selectively Disable metrics servlets features](https://github.com/rundeck/rundeck/pull/904)
* [Broken Link in Documentation](https://github.com/rundeck/rundeck/issues/903)
* [Machine tag style attributes don't get replaced ](https://github.com/rundeck/rundeck/issues/901)
* [Scheduled job with retry never completes 2.2.1](https://github.com/rundeck/rundeck/issues/900)
* [API docs state latest version is 11, but it is 12](https://github.com/rundeck/rundeck/issues/898)
* [NPE: Cannot get property 'nodeSet' on null object since upgrade to 2.2.1-1](https://github.com/rundeck/rundeck/issues/896)
* [Powershell and script-exec - extension problem](https://github.com/rundeck/rundeck/issues/894)
* [Ldap nestedGroup examples](https://github.com/rundeck/rundeck/pull/892)
* ["Retry failed nodes" does not seem to work, when using dynamic nodes filters](https://github.com/rundeck/rundeck/issues/883)
* [UI job status incorrect](https://github.com/rundeck/rundeck/issues/861)
* [Odd page when not allowing node info access](https://github.com/rundeck/rundeck/issues/844)
---

Release 2.2.2
=============

Date: 2014-09-19

This release fixes a number of bugs and addresses several potential security issues:

1. Require a unique token for each form request from the GUI, which prevents replay and CSRF attacks
2. Updated all pages to prevent unencoded data from being written to the response, preventing XSS style attacks.
3. Prevent access to the /api URLs via the web GUI.
4. Some plugins (Resource model, Node Executor and File Copier) now support using Password fields displayed in the Project config page. The field values once set are never revealed in clear text via the GUI.

Please see the Notes below for some configuration information
related to these changes.

**A big Thank You to one of our clients for sponsoring the work for these enhancements.**

*Security Notes:*

The new form tokens used in all form requests
by default will expire in 30 minutes.
This means that if your session timeout is larger than 30 minutes
and you attempt to e.g. run a job
after your web page has been sitting open for longer than that,
you will see an "Invalid token" error.
If this becomes a problem for you
you can either change the expiration time for these tokens,
or switch to using non-expiring tokens.
See [Administration - Configuration File Reference - Security](http://rundeck.org/2.2.2/administration/configuration-file-reference.html#security).

To add a Password field definition to your plugin, 
see [Plugin Development - Description Properties](http://rundeck.org/2.2.2/developer/plugin-development.html#description-properties). 
(Note that currently using property annotations is not supported 
for the three plugin types that can use Password properties.)

*Upgrade notes:* 

See the [Upgrading Guide](http://rundeck.org/2.2.2/upgrading/index.html).


## Contributors

* Andreas Knifh (knifhen)
* Daniel Serodio (dserodio)
* Greg Schueler (gschueler)

## Bug Reporters

* adolfocorreia
* ahonor
* arjones85
* danpilch
* dennis-benzinger-hybris
* dserodio
* garyhodgson
* gschueler
* jerome83136
* knifhen
* majkinetor
* rfletcher
* schicky

## Issues

* [dynamic node filter string incorrectly includes name: prefix](https://github.com/rundeck/rundeck/issues/934)
* [aclpolicy files are listed in random order in Configure page](https://github.com/rundeck/rundeck/issues/931)
* [Improve "Authenticating Users" docs re. logging](https://github.com/rundeck/rundeck/pull/925)
* [Security: allow plugins to specify password properties that are obscured in project config page](https://github.com/rundeck/rundeck/pull/919)
* [Job Variable Length is too low](https://github.com/rundeck/rundeck/issues/915)
* [Config toggle: Hide error page stacktrace](https://github.com/rundeck/rundeck/pull/910)
* [Security: CSRF prevention](https://github.com/rundeck/rundeck/pull/909)
* [Security: prevent XSS issues](https://github.com/rundeck/rundeck/pull/908)
* [Cannot pass multiple values to multivalued option with enforced values](https://github.com/rundeck/rundeck/issues/907)
* [Rundeck 2.1.1 scheduling bug](https://github.com/rundeck/rundeck/issues/905)
* [Selectively Disable metrics servlets features](https://github.com/rundeck/rundeck/pull/904)
* [Broken Link in Documentation](https://github.com/rundeck/rundeck/issues/903)
* [Machine tag style attributes don't get replaced ](https://github.com/rundeck/rundeck/issues/901)
* [Scheduled job with retry never completes 2.2.1](https://github.com/rundeck/rundeck/issues/900)
* [API docs state latest version is 11, but it is 12](https://github.com/rundeck/rundeck/issues/898)
* [NPE: Cannot get property 'nodeSet' on null object since upgrade to 2.2.1-1](https://github.com/rundeck/rundeck/issues/896)
* [Powershell and script-exec - extension problem](https://github.com/rundeck/rundeck/issues/894)
* [Ldap nestedGroup examples](https://github.com/rundeck/rundeck/pull/892)
* ["Retry failed nodes" does not seem to work, when using dynamic nodes filters](https://github.com/rundeck/rundeck/issues/883)
* [UI job status incorrect](https://github.com/rundeck/rundeck/issues/861)
* [Odd page when not allowing node info access](https://github.com/rundeck/rundeck/issues/844)
---

Release 2.2.1
=============

Date: 2014-07-30

Bugfix release.

*Upgrade notes:* 

See the [Upgrading Guide](http://rundeck.org/2.2.1/upgrading/index.html).

## Contributors

* Greg Schueler (gschueler)

## Issues

* [example node filters in help popover when clicked go to error page](https://github.com/rundeck/rundeck/issues/875)
* [Job references listed in workflow definition are hard to read](https://github.com/rundeck/rundeck/issues/869)
* [Job retry is not documented](https://github.com/rundeck/rundeck/issues/868)
* [file-copy-destination-dir default value for windows nodes is invalid](https://github.com/rundeck/rundeck/issues/867)
* [Rundeck war should include bundled plugins](https://github.com/rundeck/rundeck/issues/865)
* [Run-again screen with multi-select list json provider options duplicates parameters](https://github.com/rundeck/rundeck/issues/864)
* [Links to localhost in the activity tab](https://github.com/rundeck/rundeck/issues/862)
* [stacktrace.log location should be configurable](https://github.com/rundeck/rundeck/issues/860)
* [aclpolicy: syntax error in yaml file should indicate file name](https://github.com/rundeck/rundeck/issues/858)
* [login page "welcome message" needs formatting](https://github.com/rundeck/rundeck/issues/857)
* [Allow multiple default values for "Multi-valued" options](https://github.com/rundeck/rundeck/issues/435)
---

Release 2.2.0
=============

Date: 2014-07-11

New features:

* Job timeout: specify a maximum duration for a job. If the job execution exceeds this duration, it will be halted (as if killed manually). You can use a simple format like "120m" (120 minutes) or "2h" (2 hours).  You can specify the timeout via a job option by setting it to `${option.name}`.  This only affects the job when executed directly, not when run as a job reference.
* Job retry: specify a maximum retry attempt for a job.  If the job fails or is timed out, it will be retried.  The maximum value can be specified via a job option if set to `${option.name}`.
* Delete executions: delete executions individually or in bulk. Requires a 'delete_execution' action allowed via aclpolicy.

Some bug fixes are included, as well as some pull requests to enhance the LDAP login module:

* Support nested LDAP group membership (see [Added support for nested groups in JettyCachingLdapLoginModule.](https://github.com/rundeck/rundeck/pull/829))
* Support a "supplementalRoles" setting, which can help avoid the `!role` issue. See [Login Module Configuration](http://rundeck.org/2.2.0/administration/authenticating-users.html#login-module-configuration).

*Upgrade notes:* 

Several domain fields were added. Mysql and H2 should upgrade in place without issue.

## Contributors

* jdmulloy
* Greg Schueler (gschueler)
* Alex Honor (ahonor)
* new23d
* Bart van der Schans (schans)
* Andreas Knifh (knifhen)

## Issues

* [Server listening port number defined in profile instead of service start script](https://github.com/rundeck/rundeck/pull/845)
* [Install fails when /etc/rundeck is a symlink](https://github.com/rundeck/rundeck/pull/842)
* [Multiple Recepient E-mail Notification](https://github.com/rundeck/rundeck/issues/834)
* [Added support for nested groups in JettyCachingLdapLoginModule.](https://github.com/rundeck/rundeck/pull/829)
* [Add Job retry behavior](https://github.com/rundeck/rundeck/pull/825)
* [Job's references are reported as Killed](https://github.com/rundeck/rundeck/issues/821)
* [Ace editor cursor position bug on mac retina display](https://github.com/rundeck/rundeck/issues/820)
* [add a Job Timeout behavior](https://github.com/rundeck/rundeck/issues/815)
* [Added support for the supplementalRoles option to the LDAP Module](https://github.com/rundeck/rundeck/pull/803)
* [Ansi 256 color support doesn't work](https://github.com/rundeck/rundeck/issues/797)
* [Delete executions](https://github.com/rundeck/rundeck/pull/767)
---

Release 2.1.3
=============

Date: 2014-06-27

Fix several bugs, including a temporary fix for issue #821: [Job references reported as killed](https://github.com/rundeck/rundeck/issues/821)

*Upgrade notes:* 

1. If you are upgrading from 2.0.1 or earlier, see the notes about schema changes in the previous release notes: [2.0.2 Release Notes](http://rundeck.org/docs/history/version-2.0.2.html).
2. See the [Upgrading Guide](http://rundeck.org/2.1.0/upgrading/index.html)

## Contributors

* Greg Schueler (gschueler)

## Issues

* [option order not preserved when using cascading remote options](https://github.com/rundeck/rundeck/issues/841)
* [Multiple Recepient E-mail Notification](https://github.com/rundeck/rundeck/issues/834)
* [Ace editor cursor position bug on mac retina display](https://github.com/rundeck/rundeck/issues/820)
* [Ansi 256 color support doesn't work](https://github.com/rundeck/rundeck/issues/797)

---

Release 2.1.2
=============

Date: 2014-05-23

Bugfix release. Some enhancements:

* ANSI colorized output support
* Updated icons to distinguish command, script and script-file steps

*Upgrade notes:* 

1. If you are upgrading from 2.0.1 or earlier, see the notes about schema changes in the previous release notes: [2.0.2 Release Notes](http://rundeck.org/docs/history/version-2.0.2.html).
2. See the [Upgrading Guide](http://rundeck.org/2.1.0/upgrading/index.html)

## Contributors

* Greg Schueler (gschueler)

## Issues

* [Create job with nodeset: breaks using tagsa+tagb](https://github.com/rundeck/rundeck/issues/795)
* [node filter exclusion of tags doesn't seem to work](https://github.com/rundeck/rundeck/issues/793)
* [Add separate icons for script and scriptfile steps #787](https://github.com/rundeck/rundeck/pull/792)
* [Built in step icons should differ for command, script, url](https://github.com/rundeck/rundeck/issues/787)
* [Ansi color output support](https://github.com/rundeck/rundeck/pull/786)
* [Wrong elapsed time for jobs running more than 24 hours.](https://github.com/rundeck/rundeck/issues/784)
* [Directory node source should merge tags for multiple nodes](https://github.com/rundeck/rundeck/issues/783)
* [Expand variables in ssh-keypath from a node #780](https://github.com/rundeck/rundeck/pull/782)
* [SSH key storage path fails for node executor](https://github.com/rundeck/rundeck/issues/781)
* [SSH keypath for a node should allow embedded context variables](https://github.com/rundeck/rundeck/issues/780)
* [Add loading indicator for nodes in nodes/commands page #759](https://github.com/rundeck/rundeck/pull/779)
* [Add missing context to execution data](https://github.com/rundeck/rundeck/pull/778)
* [Importing a job always sets the month to *](https://github.com/rundeck/rundeck/issues/774)
* [crontab parsing broken on intial import](https://github.com/rundeck/rundeck/issues/773)
* [step description not encoded in form field](https://github.com/rundeck/rundeck/issues/771)
* [Node results table should not uppercase attribute headers](https://github.com/rundeck/rundeck/issues/770)
* [NPE if node step throws exception](https://github.com/rundeck/rundeck/issues/769)
* [Create and Run job button in job create form is redundant](https://github.com/rundeck/rundeck/issues/768)
* [2.1.1 Debug output causes NPE when executing script or scp file](https://github.com/rundeck/rundeck/issues/766)
* [HipChat Plugin Error with Rundeck 2.1.1](https://github.com/rundeck/rundeck/issues/764)
* [Documentation: API tokens access has wrong URL path](https://github.com/rundeck/rundeck/issues/763)
* [Display better status message when project node source is loading](https://github.com/rundeck/rundeck/issues/759)
* [Receiving 400 error after logging out and trying to log back in](https://github.com/rundeck/rundeck/issues/758)
* [allow per-job custom email subject line for #755](https://github.com/rundeck/rundeck/pull/756)
* [Allow per-job custom email notification subject line](https://github.com/rundeck/rundeck/issues/755)
* [Windows/chrome UI: long node list and job args overflow the page](https://github.com/rundeck/rundeck/issues/717)
* [plugin not found: plugin cache dir has wrong permissions](https://github.com/rundeck/rundeck/issues/565)
* [Unable to use multiple LDAP servers](https://github.com/rundeck/rundeck/issues/541)
* [SSL truststore path is misconfigured by default in OS packages](https://github.com/rundeck/rundeck/issues/507)
* [Add rundeck server name and UUID context variables to option model provider url](https://github.com/rundeck/rundeck/issues/500)


---

Release 2.1.1
=============

Date: 2014-05-01

Bugfix release, with a small feature enhancement.

* [Custom Email Templates](http://rundeck.org/2.1.1/administration/configuration-file-reference.html#custom-email-templates):

*Upgrade notes:* 

1. If you are upgrading from 2.0.1 or earlier, see the notes about schema changes in the previous release notes: [2.0.2 Release Notes](http://rundeck.org/2.1.0/history/version-2.0.2.html).
2. See the [Upgrading Guide](http://rundeck.org/2.1.0/upgrading/index.html)

## Contributors

* Greg Schueler (gschueler)
* Jason R. McNeil (jasonrm)

## Issues

* [Simple custom email notification templates](https://github.com/rundeck/rundeck/issues/753)
* [notification plugins are not working](https://github.com/rundeck/rundeck/pull/752)
* [YAML documentation is wrong about dayofmonth ](https://github.com/rundeck/rundeck/issues/751)
* [2.1.0 - Create Project Browse Storage doesn't work](https://github.com/rundeck/rundeck/issues/749)
* [scheduled job shows "never" when viewing running execution](https://github.com/rundeck/rundeck/issues/737)
---

Release 2.1.0
=============

Date: 2014-04-23

This release enhances the Rundeck API and provides a facility for storing Key files that can be used for SSH.

* Project management improvements: 
    - Project Delete action in the Configuration page
    - [Project create/config/delete APIs](http://rundeck.org/2.1.0/api/index.html#changes)
- [Key storage facility](http://rundeck.org/2.1.0/administration/key-storage.html):
    + [Upload public or private keys via API](http://rundeck.org/2.1.0/api/index.html#changes)
    + store the key file data on disk or in the DB
    + Use private keys for SSH
    + Note: no encryption is performed by default, but you can use plugins to encrypt the data

*Upgrade notes:* 

1. If you are upgrading from 2.0.1 or earlier, see the notes about schema changes in the previous release notes: [2.0.2 Release Notes](http://rundeck.org/2.1.0/history/version-2.0.2.html).
2. See the [Upgrading Guide](http://rundeck.org/2.1.0/upgrading/index.html)

## Contributors

* Diomidis Spinellis (dspinellis)
* Greg Schueler (gschueler)

## Issues

* [add link about project.properties in project setup guide](https://github.com/rundeck/rundeck/issues/745)
* [After update to 2.0.4-1.15.GA ssh login fails with "bad ownership or modes for directory"](https://github.com/rundeck/rundeck/issues/743)
* [Key storage and management](https://github.com/rundeck/rundeck/issues/726)
* [deb requires java 6](https://github.com/rundeck/rundeck/issues/722)
* [Feature/project api](https://github.com/rundeck/rundeck/pull/693)
* [Allow script based plugins (-plugin.zip) to have just .zip extension instead](https://github.com/rundeck/rundeck/issues/556)
* [Allow user configuration of authentication methods available to built-in SSH provider](https://github.com/rundeck/rundeck/issues/551)
* [Update API to better manage rundeck administration](https://github.com/rundeck/rundeck/issues/492)
* [API to get a project configuration](https://github.com/rundeck/rundeck/issues/491)
* [API for api-key administration (list,create,remove)](https://github.com/rundeck/rundeck/issues/488)
* [API for project remove](https://github.com/rundeck/rundeck/issues/487)
* [API for project create ](https://github.com/rundeck/rundeck/issues/486)
* [API for project export and import](https://github.com/rundeck/rundeck/issues/485)
---

Release 2.0.4
=============

Date: 2014-04-18

This is a bugfix release (see Issues.)

*Important Upgrade Note*: This bug [Issue 661](https://github.com/rundeck/rundeck/issues/661)
is caused by the VARCHAR length being too small for a database column.  Unfortunately grails won't
automatically update the VARCHAR size when you upgrade.

* If you are using a different database (mysql, etc.), you will have
to update your schema manually.
* If you are using H2, you can use this
script to update your database:

    1. Shutdown rundeck
    2. Run [rundeck-2.0.2-h2-upgrade.sh](https://gist.github.com/gschueler/9534814#file-rundeck-2-0-2-h2-upgrade-sh)
    3. Start rundeck

## Contributors

* Greg Schueler (gschueler)

## Issues

* [Required option value icon shown even when value is set](https://github.com/rundeck/rundeck/issues/740)
* [[2.0.3] Documentation: job 'create' access not shown in admin example ACL policy](https://github.com/rundeck/rundeck/issues/738)
* [debian package should not include jetty6 libs](https://github.com/rundeck/rundeck/issues/735)
* [Job Run again after changing nodes doesn't keep the same nodes](https://github.com/rundeck/rundeck/issues/734)
* [Allow serving content with an SSL terminated proxy](https://github.com/rundeck/rundeck/issues/732)
* [ressources.xml handling questions](https://github.com/rundeck/rundeck/issues/730)
* [Default Node File Copier -> Script Execution -> Missing variable](https://github.com/rundeck/rundeck/issues/725)
* [V2.0.2 : Job report's page is broken when Job's parameters are too Long](https://github.com/rundeck/rundeck/issues/721)
* [Job fails with "No matched nodes" unless target nodes explicitly selected](https://github.com/rundeck/rundeck/issues/719)
* [Some UI issues in V2.X but not critic](https://github.com/rundeck/rundeck/issues/717)
* ["run -f" in follow mode provides incorrect return code to shell](https://github.com/rundeck/rundeck/issues/714)
* [Rundeck 2.0.2 - Project Configuration error when cancelling](https://github.com/rundeck/rundeck/issues/708)
* [Deep link to UI not honoured is user authenticates (2.0.x)](https://github.com/rundeck/rundeck/issues/703)
* [Large execution output takes a long time to load](https://github.com/rundeck/rundeck/issues/655)
* [Job option names are not modifiable in 1.6.1+](https://github.com/rundeck/rundeck/issues/559)
---

Release 2.0.3
=============

Date: 2014-03-24

This is a bugfix release (see Issues.)

*Important Upgrade Note*: This bug [Issue 661](https://github.com/rundeck/rundeck/issues/661)
is caused by the VARCHAR length being too small for a database column.  Unfortunately grails won't
automatically update the VARCHAR size when you upgrade.

* If you are using a different database (mysql, etc.), you will have
to update your schema manually.
* If you are using H2, you can use this
script to update your database:

    1. Shutdown rundeck
    2. Run [rundeck-2.0.2-h2-upgrade.sh](https://gist.github.com/gschueler/9534814#file-rundeck-2-0-2-h2-upgrade-sh)
    3. Start rundeck

## Contributors

* David Petzel
* Greg Schueler (gschueler)
* Alex Honor (ahonor)

## Issues

* [Create from execution is broken [2.0.2]](https://github.com/rundeck/rundeck/issues/707)
* [Service fails to start on RHEL5](https://github.com/rundeck/rundeck/issues/682)

---

Release 2.0.2
=============

Date: 2014-03-13

This is a bugfix release.

*Important Upgrade Note*: This bug [Issue 661](https://github.com/rundeck/rundeck/issues/661)
is caused by the VARCHAR length being too small for a database column.  Unfortunately grails won't
automatically update the VARCHAR size when you upgrade.

* If you are using a different database (mysql, etc.), you will have
to update your schema manually.
* If you are using H2, you can use this
script to update your database:

    1. Shutdown rundeck
    2. Run [rundeck-2.0.2-h2-upgrade.sh](https://gist.github.com/gschueler/9534814#file-rundeck-2-0-2-h2-upgrade-sh)
    3. Start rundeck


## Contributors

* David Wittman
* Greg Schueler
* Alex Honor

## Issues

* [Grammar fix in Job editor view](https://github.com/rundeck/rundeck/pull/702)
* [Import of an Exported Rundeck Project Results in "No such property: year for class: java.lang.String"](https://github.com/rundeck/rundeck/issues/698)
* [Plugin development guide broken into per plugin pages](https://github.com/rundeck/rundeck/issues/695)
* [Custom Attributes unusable in dynamic filters](https://github.com/rundeck/rundeck/issues/691)
* [ Required options are not visible when the job page is refreshed](https://github.com/rundeck/rundeck/issues/690)
* [[2.0.1] Option field is split when use Run Again... button](https://github.com/rundeck/rundeck/issues/684)
* [Rundeck 2.0.1 - cron settings reset in ui](https://github.com/rundeck/rundeck/issues/678)
* [Rundeck 2.0.1 - Improve look of unauthorized access pages](https://github.com/rundeck/rundeck/issues/675)
* [Activity page: can show activity for wrong project](https://github.com/rundeck/rundeck/issues/674)
* [Project import: failure if execution workflow has nodeStep jobref](https://github.com/rundeck/rundeck/issues/673)
* [Rundeck 2.0.1 - Can not hide 'Command' / adhoc section](https://github.com/rundeck/rundeck/issues/672)
* [Update upstart init script to allow service stop command](https://github.com/rundeck/rundeck/pull/670)
* [Rundeck can't be stopped/restarted on Ubuntu](https://github.com/rundeck/rundeck/issues/669)
* [Show all tags button doesn't work correctly on Job run page](https://github.com/rundeck/rundeck/issues/668)
* [rundeck 2.0.1 - change target nodes - sql error when run is activated ](https://github.com/rundeck/rundeck/issues/661)
* [Rundeck 2.0.1 - remove UUID doesn't work when uploading job definition ](https://github.com/rundeck/rundeck/issues/658)
* [Rundeck 2.0.1 - 2013 copyright in footer](https://github.com/rundeck/rundeck/issues/657)
* [Export archive failed with NullPointerException in Rundeck 2.0.0](https://github.com/rundeck/rundeck/issues/656)


---

Release 2.0.1
===========

Date: 2014-02-11

This is a bugfix release.

Contributors:

* Alex Honor
* Diomidis Spinellis
* Greg Schueler
* Mark LaPerriere

Issues:

* [Option values select boxes only fill the first option](https://github.com/rundeck/rundeck/issues/654)
* [Update doc screenshots and improve organization](https://github.com/rundeck/rundeck/issues/652)
* [`dispatch` command uses old node filter syntax](https://github.com/rundeck/rundeck/issues/651)
* [`dispatch` command node filtering broken](https://github.com/rundeck/rundeck/issues/647)
* [2.0: Copy file doesn't rename the file](https://github.com/rundeck/rundeck/issues/646)
* [Rundeck 2.0 - Job fails if workflow execute another job 'for each node'](https://github.com/rundeck/rundeck/issues/641)
* [Rundeck 2.0 - "Unable to create URL for mapping" on home page when rundeck.gui.startpage=jobs](https://github.com/rundeck/rundeck/issues/639)
* [Configure Project should highlight the "Configure" tab (not Nodes)](https://github.com/rundeck/rundeck/issues/638)
* [Rundeck 2.0 - Can not send email notifications](https://github.com/rundeck/rundeck/issues/637)
* [Fix typo](https://github.com/rundeck/rundeck/pull/635)
* [Rundeck 2.0.0: Job Add or Edit > Node Filter > Set Filter button broken](https://github.com/rundeck/rundeck/issues/634)
* [Ensure rundeck/.ssh directory is created](https://github.com/rundeck/rundeck/pull/633)
* [Run ssh-keygen after home directory is installed](https://github.com/rundeck/rundeck/pull/632)
* [Rundeck RPM pre-install script fails to create home directory](https://github.com/rundeck/rundeck/issues/631)
* [Running make outside bash fails to pickup the package version](https://github.com/rundeck/rundeck/issues/624)
* [API Run Script Documentation Misleading](https://github.com/rundeck/rundeck/issues/596)
* [Expanding inline script in definition view](https://github.com/rundeck/rundeck/issues/593)

---

Release 2.0.0
===========

Date: 2014-01-31

Rundeck 2.0.0 introduces a large number of major changes. We have revamped the
entire UI and overhauled the underpinnings. Our goals were to improve the user
experience and cut down some of the technical debt which had accrued.

Note that the documentation is still being updated. We will update it as it improves at <http://rundeck.org/2.0.0>

Before you upgrade to Rundeck 2.0, please be sure to read the Upgrading Guide
located in the documentation.

Many thanks to everyone who was able to contribute ideas, feedback, code, time
or money in helping us to improve Rundeck.

## Notable Changes

* New feature: live execution state view
    * Live view of job executions to see what step is running on which node.
    * Instantly drill in to view the output for the step and node that failed
    * View node metrics and collated output.
    * **Please give a big thank-you to (an anonymous) "sponsored development client" for funding the work for this feature!**
* New Projects home page displays readme and message of the day files that can be customized with markdown to display notices to users
* Improved Node filter
    * Now supports all custom node attributes
    * New filter expression syntax, simple examples: 
        - `tags: a+b` filters by tags
        - `environment: (prod|qa)` Regular expression filter on an attribute called `environment`
    * New simpler UI
* Improved Nodes page to better navigate the data about the nodes in your infrastructure
    * Navigate nodes through attribute links
    * Run a job or execute a command from filter sets
* New Commands page dedicated to ad hoc command execution.
    * Controls over thread count and error handling
    * Reference saved defined filters or express your own.
* Step descriptions for workflow steps. Give your step a brief description, which will be displayed during execution.
* Improved Activity views with tabbed views for common queries
    * Tabs for Now running, recent, errors and executions by you.
* Box score metrics for executions. Use Rundeck as an information radiator.
    * Percent completed and Success/Failure metrics displayed for each execution 

## Enhancements

* New coat of paint: new logo, new GUI style using Bootstrap 3 and Flatly theme
* Caching and error catching for resource model source plugins
* Execution model API and json representation stored with log output
* Optimized internals to reduce service loading time
* Cruft removal (legacy formats and syntaxes), upgraded frameworks
* Copy file step plugin copies files from rundeck server to remote nodes.
* API
    * Better REST-ful behavior
    * removed use of 302 redirects between requests and some responses
* JDK7 support

## Related projects

The Rundeck organization on github is the new location for the Rundeck application source code as well as other associated projects:

* [Rundeck source](https://github.com/rundeck/rundeck)
* [Rundeck api-java-client library](https://github.com/rundeck/rundeck-api-java-client) 
    - New version 9.3 recently released.

Additionally, the Rundeck-plugin for Jenkins is now maintained by the core Rundeck project maintainers.

* [Rundeck-plugin for Jenkins](https://github.com/jenkinsci/rundeck-plugin)
    - New version 3.0 recently released

(Special thanks to Vincent Behar who originally created both the rundeck-api-java-client and rundeck-plugin projects.)

## Get in touch

Please let us know about any issues you find, or just if you like the new look:

* Github Issues: <https://github.com/dtolabs/rundeck/issues>
* Mailing list: <rundeck-discuss@googlegroups.com>
* IRC: #rundeck on freenode.net ([webchat link](http://webchat.freenode.net/?nick=rundeckuser.&channels=rundeck&prompt=1))
* Twitter: [@rundeck](https://twitter.com/rundeck)

## Acknowledgements

* Alex Honor
* Greg Schueler
* Damon Edwards
* John Burbridge
* Moto Ohno
* Kim Ho
* Matt Wise at Nextdoor.com
* Etienne Grignon at Disney
* Srinivas Peri and Aya Ivtan at Adobe
* Mark Maun and Eddie Wizelman at Ticketmaster
* Vincent Behar
* As well as *(anonymous) Sponsored Development Clients* - thank you!

## Issues

* [Update docs for Upgrading to 2.0](https://github.com/rundeck/rundeck/issues/629)
* [Multiple node sources should merge the attributes for a node](https://github.com/rundeck/rundeck/issues/628)
* [Running Rundeck in Tomcat and integrating with Jenkins ](https://github.com/rundeck/rundeck/issues/626)
* [[2.0-beta1] Execution log could not be found after renaming the job](https://github.com/rundeck/rundeck/issues/622)
* [2.0-beta1: LDAP authentication is broken for RPM install](https://github.com/rundeck/rundeck/issues/621)
* ["Change the Target Nodes" option not work in Rundeck 2.0beta1](https://github.com/rundeck/rundeck/issues/619)
* [NPE parsing YAML with empty tag](https://github.com/rundeck/rundeck/issues/613)
* [named steps](https://github.com/rundeck/rundeck/issues/567)
* [Emit execution status logs via Log4j](https://github.com/rundeck/rundeck/issues/553)
* [SSH authentication in a workflow node step plugin](https://github.com/rundeck/rundeck/issues/527)
* [update rundeck page URLs to include project context](https://github.com/rundeck/rundeck/issues/149)

## Fixed in beta1

* [Rundeck should catch errors and cache node data from Resource Model Source providers](https://github.com/dtolabs/rundeck/issues/609)
* [MS IE / Rundeck Nodes Page: "Enter a shell command" caption not visible](https://github.com/dtolabs/rundeck/issues/607)
* [Refactor some execution finalization code for #511](https://github.com/dtolabs/rundeck/pull/604)
* [Node attributes with ":" character breaks XML serialization.](https://github.com/dtolabs/rundeck/issues/603)
* [Remove rpm java dependency](https://github.com/dtolabs/rundeck/issues/601)
* [rundeck does not output spaces/tabs properly](https://github.com/dtolabs/rundeck/issues/600)
* [edit job and duplicate to a new job buttons not-clickable in 2.0.0-1-alpha1](https://github.com/dtolabs/rundeck/issues/598)
* [Send Notification not saved](https://github.com/dtolabs/rundeck/issues/594)
* [Delete job](https://github.com/dtolabs/rundeck/issues/592)
* [Missing username causes failure with "Execution failed: X: null", even if project.ssh.user is set](https://github.com/dtolabs/rundeck/issues/589)
* [Default Option values are ignored when a jobs is referenced from another job..](https://github.com/dtolabs/rundeck/issues/577)
* [Remove dead/unused keys from framework.properties](https://github.com/dtolabs/rundeck/issues/575)
* [Remove auto-project creation from Setup](https://github.com/dtolabs/rundeck/issues/574)
* [The quick 'Run' page should allow for thread count adjustment as well as 'on failure' behavior changes.](https://github.com/dtolabs/rundeck/issues/510)
* [obsolete RDECK_HOME and rdeck.home](https://github.com/dtolabs/rundeck/issues/508)
* ['group' and 'user' field should be wildcard-able in the aclpolicy files](https://github.com/dtolabs/rundeck/issues/359)
* [upgrade grails to 2.x](https://github.com/dtolabs/rundeck/issues/219)

---

Release 1.6.2
===========

Date: 2013-09-19

Notable Changes:

* Bug fixes:
    * Job references could not be edited after upgrading from 1.6.0 to 1.6.1
    * using node rank attribute with the same value on two nodes would skip nodes
    * error running jobs with no options defined
    * LDAPS certificate validation fixes
    * Secure option data should not be echoed in DEBUG logs

Many thanks to Kim Ho for his contributions for this release!

Contributors:

* Greg Schueler
* Kim Ho

Issues:

* [LDAP SSL CN validation checks fail to validate wildcard SSL cert names](https://github.com/dtolabs/rundeck/issues/547)
* [No stacktraces in logs when plugins throw exceptions](https://github.com/dtolabs/rundeck/pull/545)
* [Improve execution page info when option variables are used in node filters](https://github.com/dtolabs/rundeck/issues/543)
* [Rundeck prints secure option data in DEBUG into execution logs](https://github.com/dtolabs/rundeck/pull/542)
* [Java notification plugin Configuration map does not contain all values](https://github.com/dtolabs/rundeck/issues/540)
* [Cannot edit job references if upgrading from 1.6.0 to 1.6.1](https://github.com/dtolabs/rundeck/issues/539)
* [Can't change current project if not authorized for last viewed project](https://github.com/dtolabs/rundeck/issues/537)
* [using node rank attribute when dispatching will skip nodes if rank values are the same](https://github.com/dtolabs/rundeck/issues/535)
* [ Failed request: runJobInline . Result: Internal Server Error ](https://github.com/dtolabs/rundeck/issues/534)
* [rundeck does not remove remote dispatch files](https://github.com/dtolabs/rundeck/issues/531)
* [ldaps authentication CN validation fails when using alias providerUrl](https://github.com/dtolabs/rundeck/pull/482)

---

Release 1.6.1
===========

Date: 2013-08-24

Notable Changes:

* Bug fixes and updates to documentation to reflect changes in 1.6
* Added a unix PAM login module
* Added a feature to allow static definition of API auth tokens in a file
* Restored ability to use Job references as Node steps in a workflow (i.e. execute the job reference for each matched node in the parent job.)
* Fixed issue using the Jenkins rundeck-plugin when authenticating to Rundeck

Contributors:

* Alex Honor
* Greg Schueler
* Martin Strigl

Issues:

* [Static authentication tokens defined in configuration file](https://github.com/dtolabs/rundeck/issues/524)
* [Jenkins rundeck-plugin fails to authenticate to rundeck](https://github.com/dtolabs/rundeck/issues/523)
* [* add the possibility to use ${session.user} for variable expansion in r...](https://github.com/dtolabs/rundeck/issues/521)
* [Add a unix PAM JAAS login module](https://github.com/dtolabs/rundeck/issues/518)
* [Restore node-step functionality for Job References](https://github.com/dtolabs/rundeck/issues/517)
* [API for now running executions over all projects](https://github.com/dtolabs/rundeck/issues/515)
* [Potential hanging job after execution completes](https://github.com/dtolabs/rundeck/issues/514)
* [Project archive import fails to copy log files](https://github.com/dtolabs/rundeck/issues/513)
* [Project import fails if execution has blank threadcount in xml](https://github.com/dtolabs/rundeck/issues/512)
* [Add a "Scaling Rundeck" chapter](https://github.com/dtolabs/rundeck/issues/504)
* [IE Javascript issue: can't enter option name](https://github.com/dtolabs/rundeck/issues/503)
* [GUI Java Script ReferenceError: ExecutionOptions is not defined](https://github.com/dtolabs/rundeck/issues/501)
* [Log API and Web requests](https://github.com/dtolabs/rundeck/issues/499)
* [feature request: add the variable ${job.user.name} to the Option model provider](https://github.com/dtolabs/rundeck/issues/494)
* [copy icon missing in 1.6](https://github.com/dtolabs/rundeck/issues/493)
* [Update docs with 1.6 screenshots and navigation changes](https://github.com/dtolabs/rundeck/issues/479)
* [Job scheduling issue with rundeck 1.5](https://github.com/dtolabs/rundeck/issues/411)
* [Working around Local Command automatic escaping](https://github.com/dtolabs/rundeck/issues/395)
* [Invalid option name can be defined in xml](https://github.com/dtolabs/rundeck/issues/366)
* [Add Option to strip UUID for imported jobs](https://github.com/dtolabs/rundeck/issues/249)
* [scp: ambiguous target  -  for Job with script referencing a node with a space](https://github.com/dtolabs/rundeck/issues/168)

---

Release 1.6.0
===========

Date: 2013-08-02

Rundeck 1.6.0 introduces two major changes and a number of bug fixes and enhancements:

* Logging system plugins.
    * New facility allows integration with other systems, like Logstash, and improves Rundeck's behavior in a clustered cloud environment by allowing you to have log files synched to a shared storage more easily.
* Refreshed GUI
    * Did you know it hasn't really been changed since Rundeck 1.0?!  We fixed some of the usability complaints about the old GUI, and our goal was to address usability without having to change functionality, but some of our future GUI enhancement ideas snuck in.

Notable issues:

* [Allow scripts to be run as sudo](https://github.com/dtolabs/rundeck/issues/343) - popular request!
* [Use defaults for optional parameter values, and expand to blank value when missing](https://github.com/dtolabs/rundeck/issues/352) - More intuitive and less annoying!

Many thanks for the helpful contributions from the community!

Contributors:

* Alex Honor
* Greg Schueler
* Jonathan Li
* Kim Ho
* UnsignedLong

Issues:

* [Email subject line for aborted job says "KILLING" should say "KILLED"](https://github.com/dtolabs/rundeck/issues/477)
* [onstart email notification doesn't get saved](https://github.com/dtolabs/rundeck/issues/476)
* [Optional project element for job definitions](https://github.com/dtolabs/rundeck/issues/474)
* [Unprivileged users can access execution api actions](https://github.com/dtolabs/rundeck/issues/472)
* [Improve IE8/9 support](https://github.com/dtolabs/rundeck/issues/471)
* [Rename Admin link to Configure](https://github.com/dtolabs/rundeck/issues/470)
* [Re-running an adhoc command should return to Nodes/Command page](https://github.com/dtolabs/rundeck/issues/469)
* [Save job by default](https://github.com/dtolabs/rundeck/issues/468)
* [Cascading option does not display useful text if no choices available](https://github.com/dtolabs/rundeck/issues/467)
* [RSS feed content for job execution contains inline script](https://github.com/dtolabs/rundeck/issues/466)
* [Job "Created by" information is not accurate](https://github.com/dtolabs/rundeck/issues/465)
* [Cascading options can fail to load remote options, if an option has only a single allowed value](https://github.com/dtolabs/rundeck/issues/464)
* [RSS title should include options & date/time](https://github.com/dtolabs/rundeck/issues/463)
* [RSS feed for jobs does not reflect history view](https://github.com/dtolabs/rundeck/issues/462)
* [Import Job definition: existing UUID will move a job to the current project](https://github.com/dtolabs/rundeck/issues/461)
* [Potential division by zero in email notification](https://github.com/dtolabs/rundeck/issues/460)
* [Remove /api/1/reports/create endpoint](https://github.com/dtolabs/rundeck/issues/457)
* [Remove --noqueue flag from dispatch tool](https://github.com/dtolabs/rundeck/issues/456)
* [LDAP login module does not perform CN validation on certificate when ldaps is specified](https://github.com/dtolabs/rundeck/issues/455)
* [GUI updates for Rundeck 1.6](https://github.com/dtolabs/rundeck/issues/454)
* [project archive/import should allow preserving job UUIDs](https://github.com/dtolabs/rundeck/issues/452)
* [Cluster mode servers should not kill currently running jobs on other server UUIDs](https://github.com/dtolabs/rundeck/issues/451)
* [No Project Access: should display user groups](https://github.com/dtolabs/rundeck/issues/450)
* [aclpolicy example doesn't include application scope in example](https://github.com/dtolabs/rundeck/issues/449)
* [RSS feed should default to not be enabled](https://github.com/dtolabs/rundeck/issues/448)
* [Update Notification plugins: support other property scopes](https://github.com/dtolabs/rundeck/issues/443)
* [Timeout trying to lock table: remove h2 pessimistic locks](https://github.com/dtolabs/rundeck/issues/441)
* [Remove Project dropdown in Job form](https://github.com/dtolabs/rundeck/issues/434)
* [Project archive with incomplete execution causes error on import](https://github.com/dtolabs/rundeck/issues/430)
* [Job execution context should include the server URL, execution URL, and server UUID](https://github.com/dtolabs/rundeck/issues/428)
* [Feature/log storage - adds plugin system for streaming logs and storing log files](https://github.com/dtolabs/rundeck/issues/426)
* [Some log lines are truncated in GUI](https://github.com/dtolabs/rundeck/issues/425)
* [rpm install: new project doesn't automatically create resources.xml (regression)](https://github.com/dtolabs/rundeck/issues/424)
* [Remove cruft libs](https://github.com/dtolabs/rundeck/issues/422)
* [Jar plugins: add ability to resolve classes from embedded libs first](https://github.com/dtolabs/rundeck/issues/419)
* [asUser feature stops working after scheduling a job](https://github.com/dtolabs/rundeck/issues/418)
* ["num parameter is required" error when adding a workflow step to a job](https://github.com/dtolabs/rundeck/issues/416)
* [Feature/log storage - adds plugin system for streaming logs and storing log files](https://github.com/dtolabs/rundeck/issues/414)
* [example ssh-script plugin: disable hostkey verification](https://github.com/dtolabs/rundeck/issues/413)
* [update footers with simplifyops links](https://github.com/dtolabs/rundeck/issues/410)
* [Missing osFamily attribute in Node definition causes command to fail](https://github.com/dtolabs/rundeck/issues/406)
* [Use defaults for optional parameter values, and expand to blank value when missing](https://github.com/dtolabs/rundeck/issues/352)
* [Rundeck execution follow UI splits single steps into multiple sections](https://github.com/dtolabs/rundeck/issues/347)
* [Allow scripts to be run as sudo](https://github.com/dtolabs/rundeck/issues/343)
* [changing project via dropdown does not update job pane.](https://github.com/dtolabs/rundeck/issues/306)

---


Release 1.5.3
===========

Date: 2013-05-30

Notable Changes:

* bug fixes
* plugins can define input properties that get displayed as textareas in the gui

Incubator changes:

* cluster mode, which allows scheduled (cron) jobs to only run on a single node when multiple Rundeck servers share a single DB. (Note: "incubator" features are experimental and likely to change)

Contributors:

* Alex Honor
* Greg Schueler
* Kim Ho

Issues:

* [Unable to create scheduled jobs if user has too many authorization roles](https://github.com/dtolabs/rundeck/issues/407)
* [Add customizable text in the login box.](https://github.com/dtolabs/rundeck/issues/405)
* [Authentication Page in manual lists wrong port for LDAPS](https://github.com/dtolabs/rundeck/issues/404)
* [deb packaging: rd-queue script has wrong perms](https://github.com/dtolabs/rundeck/issues/403)
* [JarPluginProviderLoader NPE if the pluginJar cache dir is not readable or does not exist](https://github.com/dtolabs/rundeck/issues/402)
* [deb/rpm packaging, cli tools use wrong rdeck.base value](https://github.com/dtolabs/rundeck/issues/401)
* [Secure option default values are not used by scheduled jobs](https://github.com/dtolabs/rundeck/issues/399)
* [Feature/cluster mode unique server for job schedules](https://github.com/dtolabs/rundeck/issues/396)
* [JSch authentication retries until server maxes out allowed attempts](https://github.com/dtolabs/rundeck/issues/393)
* [Add support for textarea rendering option for String property type](https://github.com/dtolabs/rundeck/issues/390)
* [deb/rpm packaging rundeck-config doesn't set grails.serverURL](https://github.com/dtolabs/rundeck/issues/387)

---


Release 1.5.2
===========

Date: 2013-05-06

**Note: the 1.5.2 release fixes a packaging issue with rpm/debian installations.  1.5.1 release notes follow.**

Notable Changes:

* Bug fixes
* Job Notification Plugins
    * Jobs can now trigger notification plugins in addition to the built-in email and webhook
    * plugins can be developed in Java or with a new Groovy DSL
    * See the [Notification Plugin Development](../developer/notification-plugin-development.html) Chapter.
    * Example code at [dtolabs/rundeck/examples](https://github.com/dtolabs/rundeck/tree/development/examples)
* Notification 'onstart' trigger
    * Jobs can now cause a notification trigger when started via the 'onstart' trigger
* Notifications can now use the email address of the user who executed a job, as `${job.user.email}`. (You can set an email for your account in the Profile page.)

Contributors:

* Alex Honor
* Charles Duffy
* Greg Schueler
* John Burbridge
* Jonathan Li
* Kim Ho

Issues:

* [Add static ordering to job options instead of always sorted by name](https://github.com/dtolabs/rundeck/issues/361)
* [Add job notification plugins](https://github.com/dtolabs/rundeck/issues/360)
* [Big number as option default causes exception on load job from xml](https://github.com/dtolabs/rundeck/issues/356)
* [Job XML parse fails if "false" value in an command step](https://github.com/dtolabs/rundeck/issues/353)
* [Local error handling command lost on export / import](https://github.com/dtolabs/rundeck/issues/351)
* [error handler has exception when attempting to handle No Matched Nodes failure for a job reference](https://github.com/dtolabs/rundeck/issues/350)
* [Multi-value options not pre-populated when doing a "run again"](https://github.com/dtolabs/rundeck/issues/346)
* [API: add "asUser" feature for running/killing jobs and executions](https://github.com/dtolabs/rundeck/issues/341)
* [init script doesn't work on CentOS (one-line fix provided)](https://github.com/dtolabs/rundeck/issues/338)
* [job group changes before authorization check in 1.5](https://github.com/dtolabs/rundeck/issues/330)
* [Many ACL policy files can slow down the Project drop down menu](https://github.com/dtolabs/rundeck/issues/328)
* [Unable to generate an api-token on rundeck 1.5](https://github.com/dtolabs/rundeck/issues/327)
* [Cannot send notification email using 1.5](https://github.com/dtolabs/rundeck/issues/325)
* [Long description for Option causes Job import failure in 1.5 for jobs exported from 1.4](https://github.com/dtolabs/rundeck/issues/320)
* [1.5: GUI regression: Success rate % in job popup always shows 0%](https://github.com/dtolabs/rundeck/issues/316)
* [1.5: GUI regression: Job execution follow page: progress meter stuck](https://github.com/dtolabs/rundeck/issues/315)
* [Invalid XML char in log output breaks CLI output follow mode.](https://github.com/dtolabs/rundeck/issues/313)
* [1.5: html and ajax errors about "Timeout trying to lock table"](https://github.com/dtolabs/rundeck/issues/312)
* [sudo responder hangs](https://github.com/dtolabs/rundeck/issues/311)
* [CLIUtils.generateArgline does not quote IFS characters other than space](https://github.com/dtolabs/rundeck/issues/298)
* [documentation: upgrade guide "admin" aclpolicy doesn't allow all permissions](https://github.com/dtolabs/rundeck/issues/280)
* [Add Job Notification/webhook on job start](https://github.com/dtolabs/rundeck/issues/250)
* [projects folder under version control - .svn folder treated as project](https://github.com/dtolabs/rundeck/issues/209)
* [incorrect aclpolicy yaml structure can cause NPE](https://github.com/dtolabs/rundeck/issues/206)
* [Options are not passed correctly to programs](https://github.com/dtolabs/rundeck/issues/201)
* [Allow Email Address substitution in notification recipients list](https://github.com/dtolabs/rundeck/issues/165)

---


Release 1.5.1
===========

Date: 2013-04-30

Notable Changes:

* Bug fixes
* Job Notification Plugins
    * Jobs can now trigger notification plugins in addition to the built-in email and webhook
    * plugins can be developed in Java or with a new Groovy DSL
    * See the [Notification Plugin Development](../developer/notification-plugin-development.html) Chapter.
    * Example code at [dtolabs/rundeck/examples](https://github.com/dtolabs/rundeck/tree/development/examples)
* Notification 'onstart' trigger
    * Jobs can now cause a notification trigger when started via the 'onstart' trigger
* Notifications can now use the email address of the user who executed a job, as `${job.user.email}`. (You can set an email for your account in the Profile page.)

Contributors:

* Alex Honor
* Charles Duffy
* Greg Schueler
* John Burbridge
* Jonathan Li
* Kim Ho

Issues:

* [Add static ordering to job options instead of always sorted by name](https://github.com/dtolabs/rundeck/issues/361)
* [Add job notification plugins](https://github.com/dtolabs/rundeck/issues/360)
* [Big number as option default causes exception on load job from xml](https://github.com/dtolabs/rundeck/issues/356)
* [Job XML parse fails if "false" value in an command step](https://github.com/dtolabs/rundeck/issues/353)
* [Local error handling command lost on export / import](https://github.com/dtolabs/rundeck/issues/351)
* [error handler has exception when attempting to handle No Matched Nodes failure for a job reference](https://github.com/dtolabs/rundeck/issues/350)
* [Multi-value options not pre-populated when doing a "run again"](https://github.com/dtolabs/rundeck/issues/346)
* [API: add "asUser" feature for running/killing jobs and executions](https://github.com/dtolabs/rundeck/issues/341)
* [init script doesn't work on CentOS (one-line fix provided)](https://github.com/dtolabs/rundeck/issues/338)
* [job group changes before authorization check in 1.5](https://github.com/dtolabs/rundeck/issues/330)
* [Many ACL policy files can slow down the Project drop down menu](https://github.com/dtolabs/rundeck/issues/328)
* [Unable to generate an api-token on rundeck 1.5](https://github.com/dtolabs/rundeck/issues/327)
* [Cannot send notification email using 1.5](https://github.com/dtolabs/rundeck/issues/325)
* [Long description for Option causes Job import failure in 1.5 for jobs exported from 1.4](https://github.com/dtolabs/rundeck/issues/320)
* [1.5: GUI regression: Success rate % in job popup always shows 0%](https://github.com/dtolabs/rundeck/issues/316)
* [1.5: GUI regression: Job execution follow page: progress meter stuck](https://github.com/dtolabs/rundeck/issues/315)
* [Invalid XML char in log output breaks CLI output follow mode.](https://github.com/dtolabs/rundeck/issues/313)
* [1.5: html and ajax errors about "Timeout trying to lock table"](https://github.com/dtolabs/rundeck/issues/312)
* [sudo responder hangs](https://github.com/dtolabs/rundeck/issues/311)
* [CLIUtils.generateArgline does not quote IFS characters other than space](https://github.com/dtolabs/rundeck/issues/298)
* [documentation: upgrade guide "admin" aclpolicy doesn't allow all permissions](https://github.com/dtolabs/rundeck/issues/280)
* [Add Job Notification/webhook on job start](https://github.com/dtolabs/rundeck/issues/250)
* [projects folder under version control - .svn folder treated as project](https://github.com/dtolabs/rundeck/issues/209)
* [incorrect aclpolicy yaml structure can cause NPE](https://github.com/dtolabs/rundeck/issues/206)
* [Options are not passed correctly to programs](https://github.com/dtolabs/rundeck/issues/201)
* [Allow Email Address substitution in notification recipients list](https://github.com/dtolabs/rundeck/issues/165)

---


Release 1.5
===========

Date: 2/21/2013

This release introduces a few new features and some bug fixes. The new features
required  some schema changes to the database, so direct upgrading from 1.4 to
1.5 is not possible.   Please read the Upgrading document for upgrade
information.

There is now a new type of plugin, the Workflow Step Plugin, which should allow
rundeck  workflows to integrate with more systems in a more direct fashion.
These plugins can be  either "Workflow Steps" (run once per workflow) or "Node
Steps" (run once per node in a workflow.)

Steps can now have Error Handlers which run if the step fails. They will be
provided with context data about the reason the step failed.

Notable Changes:

* Step Plugins - Plugins can now be created and used for workflow or node steps.
* Error Handlers - Each step in a workflow can now have an error handler that
* will be run if the step fails.  bug fix: Job references in a workflow will now
* only run once, and not operate as a node-step.

Plugin developers:

You will need to update your plugins to work in Rundeck 1.5. Refer to the
Developer Guide for more information.

Issues:

* [User profile page broken](https://github.com/dtolabs/rundeck/issues/308)
* [Update docs for 1.5 release](https://github.com/dtolabs/rundeck/issues/307)
* [Update sample scripts in plugin docs for best practices conformance](https://github.com/dtolabs/rundeck/issues/297)
* [allow optional build parameter to rundeckapp to specify use of a local copy of the grails zip](https://github.com/dtolabs/rundeck/issues/296)
* [expose loglevel for executions](https://github.com/dtolabs/rundeck/issues/293)
* [script based plugin caching issue](https://github.com/dtolabs/rundeck/issues/290)
* [rpm/deb rundeck-config needs to be udpated](https://github.com/dtolabs/rundeck/issues/289)
* [Node dispatch threadcount can be set to blank](https://github.com/dtolabs/rundeck/issues/282)
* [job import: threadcount does not get set](https://github.com/dtolabs/rundeck/issues/281)
* [Now running and History views don't use Job view filter](https://github.com/dtolabs/rundeck/issues/273)
* [History views default to recentFilter=1d, should be all events](https://github.com/dtolabs/rundeck/issues/272)
* [History project filter is not exact](https://github.com/dtolabs/rundeck/issues/271)
* [Allow group path in URL of jobs page to filter groups](https://github.com/dtolabs/rundeck/issues/270)
* [Node dispatch threadcount can be set to blank. export+import fails.](https://github.com/dtolabs/rundeck/issues/269)
* [Bulk delete of jobs via GUI](https://github.com/dtolabs/rundeck/issues/268)
* [add release notes to generated docs](https://github.com/dtolabs/rundeck/issues/264)
* [more compatible rpm dependency for java](https://github.com/dtolabs/rundeck/issues/263)
* [remove unneeded "rdbsupport" config property for 1.5](https://github.com/dtolabs/rundeck/issues/262)
* [API: Now running execution project filter is not exact](https://github.com/dtolabs/rundeck/issues/261)
* [execution output api: xml content problems](https://github.com/dtolabs/rundeck/issues/259)
* [don't combine spaces for scripts output](https://github.com/dtolabs/rundeck/issues/258)
* [Job reference picker has incorrect behavior when clicking a group name](https://github.com/dtolabs/rundeck/issues/255)
* [Error handler failure reason as context data](https://github.com/dtolabs/rundeck/issues/248)
* [Workflow step plugins](https://github.com/dtolabs/rundeck/issues/246)
* [Job folder/group display still buggy](https://github.com/dtolabs/rundeck/issues/241)
* [dispatch -s scriptfile is broken](https://github.com/dtolabs/rundeck/issues/228)
* [can't delete job option](https://github.com/dtolabs/rundeck/issues/227)
* [dispatch with url option don't work](https://github.com/dtolabs/rundeck/issues/225)
* [Job references should run only once within a workflow](https://github.com/dtolabs/rundeck/issues/224)
* [divide by zero error on system info page](https://github.com/dtolabs/rundeck/issues/221)
* [workflow step failure handlers](https://github.com/dtolabs/rundeck/issues/218)
* [make H2 the default rundeck database backend](https://github.com/dtolabs/rundeck/issues/183)
* [UUID permits spaces](https://github.com/dtolabs/rundeck/issues/171)

---

Release 1.4.5
===========

Date: 1/10/2013

This release is a minor bugfix update, with some bonus features.

Notable Changes:

* bug fixes 
    * dispatch -s, some API project filtering was wrong, node dispatch threadcount can be set blank
* Bulk delete jobs via GUI
* Job page group filters now apply to Now Running and History areas
* History views don't use 1 day as a filter by default anymore

Issues: 

* [maint-1.4.5: dispatch -s scriptfile is broken](https://github.com/dtolabs/rundeck/issues/266)
* [maint-1.4.5: API: Now running execution project filter is not exact](https://github.com/dtolabs/rundeck/issues/265)
* [Bulk delete of jobs via GUI](https://github.com/dtolabs/rundeck/issues/245)
* [Node dispatch threadcount can be set to blank. export+import fails.](https://github.com/dtolabs/rundeck/issues/244)
* [Allow group path in URL of jobs page to filter groups](https://github.com/dtolabs/rundeck/issues/243)
* [History project filter is not exact](https://github.com/dtolabs/rundeck/issues/242)
* [History views default to recentFilter=1d, should be all events](https://github.com/dtolabs/rundeck/issues/240)
* [Now running and History views don't use Job view filter](https://github.com/dtolabs/rundeck/issues/239)

---

Release 1.4.4
===========

Date: 10/26/2012

This release marks the end of the 1.4 development cycle, and includes bug fixes and a few new features.

We are planning to make some changes in the DB schema for the next release (1.5.x) that may not be backwards 
compatible so have included a feature to export a Rundeck project into an archive file.  This will allow us
to change the schema yet still allow users to migrate their projects.

Notable Changes:

* bug fixes (scheduled jobs, mail notifications, rd-jobs yaml output, jenkins plugin + parallel jobs)
* Project archive/import - download an archive of Jobs, Executions and History that can be imported into a different project
* Added a second level of sudo password support
* Add a 'purge' action to rd-jobs tool to delete jobs
* Better support for Tomcat war deployment
* View all nodes button in Run page
* Cascading option values from remote URLs
* CLI tools can follow execution output from the server (rd-queue, run, dispatch)
* API enhancements:
    * query for executions and history reports
    * retrieve execution output

Issues: 

* [remote options URL failure allows text field input even if option is restricted](https://github.com/dtolabs/rundeck/issues/215) (bug)
* [project archive/import](https://github.com/dtolabs/rundeck/issues/212) (enhancement)
* [multiple sudo authentication support](https://github.com/dtolabs/rundeck/issues/211) (enhancement)
* [Document syntax of arguments passed to the run command](https://github.com/dtolabs/rundeck/issues/208) (enhancement, documentation)
* [add purge option to rd-jobs tool](https://github.com/dtolabs/rundeck/issues/207) (enhancement, cli)
* [Add query API for executions](https://github.com/dtolabs/rundeck/issues/205) (enhancement, api)
* [CLI tools can't authenticate to a tomcat war deployment of rundeck](https://github.com/dtolabs/rundeck/issues/204) (bug)
* [Allow history API to query for list of job names](https://github.com/dtolabs/rundeck/issues/203) (enhancement, api)
* [javascript problem: Can't change nodes when trying to run a saved job](https://github.com/dtolabs/rundeck/issues/194) (bug, ux)
* [Rundeck jobs fail to execute sometimes ](https://github.com/dtolabs/rundeck/issues/193) (bug)
* [Rundeck war should not contain servlet api libraries](https://github.com/dtolabs/rundeck/issues/192) (enhancement)
* [deb dependency requires GUI libraries](https://github.com/dtolabs/rundeck/issues/191) (enhancement, packaging)
* [Enable property expansion in framework level default ssh user ](https://github.com/dtolabs/rundeck/issues/189) (enhancement, configuration, ssh)
* [Mail notifications are broken in 1.4.3](https://github.com/dtolabs/rundeck/issues/186) (bug)
* [resource model source URL basic auth support is broken](https://github.com/dtolabs/rundeck/issues/184) (bug)
* [Update wiki/documentation for remote option provider](https://github.com/dtolabs/rundeck/issues/182) (documentation)
* [Parallel/Concurrent jobs fail](https://github.com/dtolabs/rundeck/issues/180) (bug)
* [cli tool rd-jobs format yaml does not generate any content in file for 1.4.3](https://github.com/dtolabs/rundeck/issues/179) (bug, cli)
* [Scheduled RunDeck jobs no longer work with RunDeck 1.4.3](https://github.com/dtolabs/rundeck/issues/178) (bug, scheduler, jobs)
* [Allow disabling of hover popups](https://github.com/dtolabs/rundeck/issues/174) (enhancement)
* [Add a button to view all nodes in nodes filter view](https://github.com/dtolabs/rundeck/issues/172) (enhancement, ux, filters)
* [need REST interface to retrieve execution ouput](https://github.com/dtolabs/rundeck/issues/145) (enhancement, api)
* [dispatcher needs option to queue job but also observe log](https://github.com/dtolabs/rundeck/issues/142) (enhancement)
* [Ability to change the default number of lines to display for the TAIL output in the rundeck job execution history](https://github.com/dtolabs/rundeck/issues/109) (enhancement)
* [feature for cascading select list from options provider](https://github.com/dtolabs/rundeck/issues/80) (enhancement)

---

Release 1.4.3
===========

Date: 6/21/2012

Notable Changes:

* Security fix for issue [#555 - vulnerability with ldap authentication](http://rundeck.lighthouseapp.com/projects/59277/tickets/555)
* Scripts can be executed from URLs
* Secure options now have two types (authentication or normal)
* Easily run a job with the same arguments as a previous execution
* Bugfixes

See the [Upgrading Guide](http://rundeck.org/1.4.3/upgrading/) if you are upgrading from Rundeck 1.3.

Tickets: 

* [#538 - Can't change nodes when trying to run a saved job](http://rundeck.lighthouseapp.com/projects/59277/tickets/538)
* [#555 - vulnerability with ldap authentication](http://rundeck.lighthouseapp.com/projects/59277/tickets/555)
* [#554 - in-place upgrade using the launcher leaves old jars in place](http://rundeck.lighthouseapp.com/projects/59277/tickets/554)
* [#563 - multipleExecutions Error](http://rundeck.lighthouseapp.com/projects/59277/tickets/563)
* [#287 - Force the home directory to be /home/rundeck (or perhaps /var/lib/rundeck/)](http://rundeck.lighthouseapp.com/projects/59277/tickets/287)
* [#517 - default ssh key for projects doesn't match rpm install's rundeck user ssh key](http://rundeck.lighthouseapp.com/projects/59277/tickets/517)
* [#552 - allow sudo auth configuration at project/framework level](http://rundeck.lighthouseapp.com/projects/59277/tickets/552)
* [#526 - adhoc execution page shows kill button when it is not authorized](http://rundeck.lighthouseapp.com/projects/59277/tickets/526)
* [#572 - export more vars for script plugins (contents basedir, var dir, etc)](http://rundeck.lighthouseapp.com/projects/59277/tickets/572)
* [#571 - ScriptResourceModel plugin can't use script-args and script-interpreter](http://rundeck.lighthouseapp.com/projects/59277/tickets/571)
* [#551 - Secure option values cannot be used in scripts/commands](http://rundeck.lighthouseapp.com/projects/59277/tickets/551)
* [#537 - Temp files not being removed from /tmp when using a script as a resource model source](http://rundeck.lighthouseapp.com/projects/59277/tickets/537)
* [#523 - passwordless sudo shouldn't fail after the timeout value](http://rundeck.lighthouseapp.com/projects/59277/tickets/523)
* [#518 - undocumented gui default startpage configuration](http://rundeck.lighthouseapp.com/projects/59277/tickets/518)
* [#524 - Secure option values for sudo/ssh do not get passed to sub-jobs](http://rundeck.lighthouseapp.com/projects/59277/tickets/524)
* [#230 - Allow URL values for scriptfiles](http://rundeck.lighthouseapp.com/projects/59277/tickets/230)
* [#528 - authorization for api call to system/info is not checked](http://rundeck.lighthouseapp.com/projects/59277/tickets/528)
* [#550 - warning message after upgrade to 1.4.2](http://rundeck.lighthouseapp.com/projects/59277/tickets/550)
* [#558 - Prevent job names containing slashes (/)](http://rundeck.lighthouseapp.com/projects/59277/tickets/558)
* [#553 - dispatch yields NullPointerException and fails](http://rundeck.lighthouseapp.com/projects/59277/tickets/553)
* [#560 - Re-run a job with the same arguments](http://rundeck.lighthouseapp.com/projects/59277/tickets/560)
* [#567 - Execution page: Collapse view checkbox is set incorrectly](http://rundeck.lighthouseapp.com/projects/59277/tickets/567)
* [#570 - Add "execution id" to job-context data in running jobs](http://rundeck.lighthouseapp.com/projects/59277/tickets/570)
* [#527 - NPE on node view if a node has no description defined](http://rundeck.lighthouseapp.com/projects/59277/tickets/527)
* [#529 - default apitoken aclpolicy differs for rpm/deb and launcher install](http://rundeck.lighthouseapp.com/projects/59277/tickets/529)
* [#545 - rundeck option cannot take integer value](http://rundeck.lighthouseapp.com/projects/59277/tickets/545)
* [#519 - Dispatch to one node only shows "1 node ok" even if job fails](http://rundeck.lighthouseapp.com/projects/59277/tickets/519)
* [#564 - upgrade commons-codec dependency](http://rundeck.lighthouseapp.com/projects/59277/tickets/564)
* [#530 - add faq/documentation about mysql autoreconnect flag](http://rundeck.lighthouseapp.com/projects/59277/tickets/530)
* [#544 - The CronExpression link in docs and Web GUI to http://www.quartz-scheduler.org returns 404](http://rundeck.lighthouseapp.com/projects/59277/tickets/544)
* [#522 - documentation typo/truncation on plugin dev guide](http://rundeck.lighthouseapp.com/projects/59277/tickets/522)

