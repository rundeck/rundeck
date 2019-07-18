Release 3.1.0-rc2
===========

Date: 2019-07-17

Name: <span style="color: peru"><span class="glyphicon glyphicon-piggy-bank"></span> "mozzarella stick peru piggy-bank"</span>

## Notes

### Enhancements

* More UI Improvements
* API: scheduler takeover endpoint can specify multiple job IDs
* (Enterprise) License key can be shared among cluster members using 
* (Enterprise) Execution cleanup process can clean up executions from dead cluster members
 
### Bug Fixes

* Project List page correctly loads project controls
* Remote Option values using allowed values was not validating correctly
* JAAS/property file login module: username should not be added as a role
* Admin view of user Profile page with lots of API tokens will load correctly
* (Enterprise) Cluster manager: can select all orphan jobs to reassign scheduler owner
* 

### Security

* updated dependencies flagged due to CVEs

### Plugins

* Added "Attribute Match Node Enhancer" plugin: customize node icons, or inject new node attributes based on other attributes
* (Enterprise) Ruleset workflow strategy can use variables on both sides of conditional comparisons
* (Enterprise) SQL Runner can use inline SQL script
* (Enterprise) File Transfer source should allow wildcards

### Docker

* (Enterprise) remote policy

## Contributors

* Alberto Hormazabal
* Alex Honor (ahonor)
* Diego Queiroz (DiegoQueiroz)
* Evan Farrell (moosilauke18)
* Greg Schueler (gschueler)
* Greg Zapp (ProTip)
* Jaime Tobar (jtobard)
* Jesse Marple (jessemarple)
* Luis Toledo (ltamaster)
* Stephen Joyner (sjrd218)
* carlos (carlosrfranco)

## Bug Reporters

* DiegoQueiroz
* ahormazabal
* boudekerk
* carlosrfranco
* cbo0485
* cukal
* gschueler
* jessemarple
* jtobard
* ltamaster
* marcbejerano
* menathor
* moosilauke18
* nmamn
* sebastianbello
* sjrd218
* tintranvan

## Issues

[Milestone 3.1.0-RC2](https://github.com/rundeck/rundeck/milestone/109)

* [Execution cleanup on a cluster](https://github.com/rundeck/rundeck/pull/5058)
* [Remove unused project param in plugin validation method ](https://github.com/rundeck/rundeck/pull/5054)
* [Capability to compare between variables using ruleset. fixes #5050 ](https://github.com/rundeck/rundeck/pull/5051)
* [Capability to compare variables using ruleset](https://github.com/rundeck/rundeck/issues/5050)
* [Fixes #1654 to make cleanup on records in Workflow, workflow_step and workflow_workflow_step when executions are deleted](https://github.com/rundeck/rundeck/pull/5049)
* [Issues #5002, #4979, #4463, #4464, #4465, #4466 - Update several library dependencies to address reported CVEs.](https://github.com/rundeck/rundeck/pull/5047)
* [oficial docke image: adding custom templates for repository feature.](https://github.com/rundeck/rundeck/pull/5040)
* [reverting changes to allow schedule jobs to respect the remote policy](https://github.com/rundeck/rundeck/pull/5033)
* [Add link to Docs in App Header](https://github.com/rundeck/rundeck/issues/5032)
* [Fixes #5030 - This changes make validations if the options from URL is a json object or a simple array list of options…](https://github.com/rundeck/rundeck/pull/5031)
* [Problem with remote URL options validation](https://github.com/rundeck/rundeck/issues/5030)
* [Fix #5023 race condition bug in wf engine](https://github.com/rundeck/rundeck/pull/5029)
* [Option to Enable/Disable notifications for Referenced Jobs #4182](https://github.com/rundeck/rundeck/pull/5026)
* [Upgrades jackson to 2.9.9 to address vulnerabilities](https://github.com/rundeck/rundeck/pull/5019)
* [JNDI info in the configuration page #5010](https://github.com/rundeck/rundeck/pull/5011)
* [Fix: job delete "delete all executions" checkbox, and some form control labels don't work](https://github.com/rundeck/rundeck/pull/5008)
* [more optimized use of pluginControlService](https://github.com/rundeck/rundeck/pull/5007)
* [GUI: job workflow editor has visual problems](https://github.com/rundeck/rundeck/issues/5006)
* [fix #5004 EnhancedNodeService bean was not loaded](https://github.com/rundeck/rundeck/pull/5005)
* [ Node Enhancer feature is not working](https://github.com/rundeck/rundeck/issues/5004)
* [GUI: When editing a job, errors are not clearly shown.](https://github.com/rundeck/rundeck/issues/5000)
* [GUI: "Cancel" option not present when editing Node Sources](https://github.com/rundeck/rundeck/issues/4999)
* [Sync Rundeck profile from LDAP user attributes for official docker image](https://github.com/rundeck/rundeck/pull/4995)
* [adding bind address to jsch](https://github.com/rundeck/rundeck/pull/4994)
* [Fix Issue #4831 - Adds pagination of tokens on user profile page.](https://github.com/rundeck/rundeck/pull/4990)
* [Small improvement to the API forecast](https://github.com/rundeck/rundeck/pull/4986)
* [3.1: misc ui tweaks](https://github.com/rundeck/rundeck/pull/4984)
* [Improve performance when copying/editing a job](https://github.com/rundeck/rundeck/pull/4980)
* [Security Scan library updates](https://github.com/rundeck/rundeck/issues/4979)
* [Fix home page project ui not loading #4149](https://github.com/rundeck/rundeck/pull/4977)
* [Update runbook marker parsing fixes #4973 ](https://github.com/rundeck/rundeck/pull/4976)
* [Task/3.1.0 ui review](https://github.com/rundeck/rundeck/pull/4975)
* [Add UI plugins to the new installed plugin list.](https://github.com/rundeck/rundeck/pull/4974)
* [Runbook not shown in job page](https://github.com/rundeck/rundeck/issues/4973)
* [dateformat and locale update](https://github.com/rundeck/rundeck/pull/4972)
* [Added test and extra if conditional to fix #4959](https://github.com/rundeck/rundeck/pull/4967)
* [upgrade python winrm 2.0.3. Adding support for kerberos authentication](https://github.com/rundeck/rundeck/pull/4966)
* [Execution mode status API with failing code when status is passive.](https://github.com/rundeck/rundeck/pull/4965)
* [Cleanup/prototype scriptaculous](https://github.com/rundeck/rundeck/pull/4963)
* [Fix: potential npe caused by race](https://github.com/rundeck/rundeck/pull/4962)
* [Fix empty error when node exec validation fails](https://github.com/rundeck/rundeck/pull/4961)
* [Schedule DayOfWeek via terraform/API fails.](https://github.com/rundeck/rundeck/issues/4959)
* [RD3.1 New Configure Project Form doesn't work as expected](https://github.com/rundeck/rundeck/issues/4958)
* [3.1: motd updates](https://github.com/rundeck/rundeck/pull/4957)
* [Fix: run job with no options causes error](https://github.com/rundeck/rundeck/pull/4956)
* [Add multiple jobs ids to scheduler/takeover endpoint](https://github.com/rundeck/rundeck/pull/4955)
* [3.1: job page shows running executions](https://github.com/rundeck/rundeck/pull/4952)
* [3.1: Updates: node filters in job run page](https://github.com/rundeck/rundeck/pull/4950)
* [3.1: Nodes updates](https://github.com/rundeck/rundeck/pull/4947)
* [3.1: event access update](https://github.com/rundeck/rundeck/pull/4946)
* [UI: Job Show/Run page updates](https://github.com/rundeck/rundeck/pull/4945)
* [3.1: UI: project config form tabs](https://github.com/rundeck/rundeck/pull/4935)
* [Fix #4778. Normalize JAAS debugging log messages.](https://github.com/rundeck/rundeck/pull/4933)
* [Ability to specify -b variable for SSH](https://github.com/rundeck/rundeck/issues/4932)
* [Fix #4930 for ReloadablePropertyFileLoginModule.](https://github.com/rundeck/rundeck/pull/4931)
* [Jaas login using PropertyFileLoginModule adds username as a role](https://github.com/rundeck/rundeck/issues/4930)
* [enabling option plugins by default](https://github.com/rundeck/rundeck/pull/4929)
* [Fix #4894 invalid sourcemappingurl causes 404](https://github.com/rundeck/rundeck/pull/4923)
* [RDK Cluster : scheduled job on node A killed on node B : still launched](https://github.com/rundeck/rundeck/issues/4916)
* [Remote URL Triggered too often/quickly](https://github.com/rundeck/rundeck/issues/4887)
* [Add validationQuery parameter](https://github.com/rundeck/rundeck/pull/4872)
* [UI progress bar : display issue](https://github.com/rundeck/rundeck/issues/4625)
* [Timezone in execution log output](https://github.com/rundeck/rundeck/issues/4486)
* [Option to Enable/Disable notifications for Referenced Jobs](https://github.com/rundeck/rundeck/issues/4182)
* [RD3 GUI: When displaying overview page with a big amount of projects ( over 15 ) the last ones may not load "configure" and  "create job" buttons correctly](https://github.com/rundeck/rundeck/issues/4149)
* [Scrolling output log](https://github.com/rundeck/rundeck/issues/4047)
* [Tooltip always shows the wrong time (unless timezone is equal to Zulu/UTC)](https://github.com/rundeck/rundeck/issues/3518)
