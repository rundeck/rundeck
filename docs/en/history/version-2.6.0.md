% Version 2.6.0
% greg
% 10/08/2015

Release 2.6.0
===========

Date: 2015-10-08

Name: <span style="color: chocolate"><span class="glyphicon glyphicon-gift"></span> "cafe bonbon chocolate gift"</span>

## New Features

* SCM Plugin with Git implementation (preliminary release)
* Per-job logging limits, such as max line count or file size, see [Jobs - Log Limit](../manual/jobs.html#log-limit)
* Active/passive execution mode: disable all executions on the server
* Per-Job schedule and execution toggling: disable scheduled or all executions for a single job
* UI updates to Nodes and Commands pages
* Project Archives: includes ACLs and project config
* Per-project ACLs available via API, storable in DB
	* Filesystem ACLs are now used for "global" level access control
	* Project ACLs are limited to manage the specific project, and are stored in the DB
* ACL validation improvements
	* see the `rd-acl` tool [validate command](../man1/rd-acl.html#validate-command)
	* Invalid filesystem ACLs are logged in the service log
	* Invalid project ACLs uploaded via API will be rejected
* Example Orchestrator plugins now bundled
* JSON support added for all API endpoints
* Some API refactoring for v14
* See [API v14](../api/index.html)
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