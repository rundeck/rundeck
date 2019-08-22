Release 3.1.0
===========

Date: 2019-07-31

Name: <span style="color: peru"><span class="glyphicon glyphicon-piggy-bank"></span> "mozzarella stick peru piggy-bank"</span>

## Upgrading
See the upgrade documentation [here](https://docs.rundeck.com/3.1.0-rc2/upgrading/upgrade-to-rundeck-3.1.html).

## Enhancements

### Rundeck Plugin Repository
It is now easier than ever to discover and install plugins with the new plugin management system!
This is enabled by default on new installations, however you may need to update the config for
`deb` and `rpm` installations. Documentation can be found [here](https://docs.rundeck.com/3.1.0/administration/configuration/repository.html).

![Taste the rainbow!](https://docs.rundeck.com/assets/releases/3_1_0/feature_repo_rainbow_small.gif "Taste the Rainbow!")

### Execution Cleaner
Make manual execution cleanup a thing of the past! Configure the execution cleaner to your retention policy and let
it handle the rest.

![](https://docs.rundeck.com/assets/releases/3_1_0/execution_cleanup_small.gif)


### User Interface Enhancements
Rundeck 3.1 has received numerous UI enhancements to streamline resource creation workflows and
get more of the important information you need on the screen at the same time. Here
are a few highlights:  

**Execution view**  
![](https://docs.rundeck.com/assets/releases/3_1_0/execution_view.png)

**Activity view**  
![](https://docs.rundeck.com/assets/releases/3_1_0/activity_view.png)

**Job Editor**  
![](https://docs.rundeck.com/assets/releases/3_1_0/create_job.png)

**Create Project**  
![](https://docs.rundeck.com/assets/releases/3_1_0/create_project.png)

**Project List**  
![](https://docs.rundeck.com/assets/releases/3_1_0/project_list.png)

**Job list**  
![](https://docs.rundeck.com/assets/releases/3_1_0/job_list.png)


### Misc

* Options [#4599](https://github.com/rundeck/rundeck/pull/4599): 
  * Preserve allowed value list order (sort is now optional)
  * Specify multi-value delimiter
* Authentication and authorization:
  * Ability to require a role for Rundeck access [#4820](https://github.com/rundeck/rundeck/pull/4820).
  * ACLs can use "notBy" instead of "by" to deny access to non matching groups/users [#4769](https://github.com/rundeck/rundeck/pull/4769)
* Jobs:
  * Add default Log filters at the project/framework level. For example, use the Mask Passwords plugin for all jobs in a project. [#4806](https://github.com/rundeck/rundeck/pull/4806)
  * Configuration to allow unsantized html in log output. [#4784](https://github.com/rundeck/rundeck/pull/4784)
* API: Scheduler takeover endpoint can specify multiple job IDs
* Added option to Enable/Disable notifications for Referenced Jobs [#5026](https://github.com/rundeck/rundeck/pull/5026)
* (Enterprise) License key can be shared among cluster members using database storage
* (Enterprise) Execution cleanup process can clean up executions from dead cluster members
 
## Bug Fixes

* Project List page correctly loads project controls
* Remote Option values using allowed values was not validating correctly
* Fixed node enhancer plugins not working [#5005](https://github.com/rundeck/rundeck/pull/5005)
* JAAS/property file login module: username should not be added as a role
* Admin view of user Profile page with lots of API tokens will load correctly
* (Enterprise) Cluster manager: can select all orphan jobs to reassign scheduler owner

## Security

* Updated dependencies flagged due to CVEs [#5047](https://github.com/rundeck/rundeck/pull/5047)

## Plugins

* Added "Attribute Match Node Enhancer" plugin for customizing node icons and injecting new node attributes based on other attributes
* (Enterprise) Ruleset workflow strategy can use variables on both sides of conditional comparisons
* (Enterprise) SQL Runner can use inline SQL script
* (Enterprise) File Transfer source allows wildcards

## Docker
* Images are OpenShift compatible [#4826](https://github.com/rundeck/rundeck/pull/4826)
* Sync Rundeck profile from LDAP user attributes config option [#4995](https://github.com/rundeck/rundeck/pull/4995)
* Template added for plugin repository feature [#5040](https://github.com/rundeck/rundeck/pull/5040)
* (Enterprise) Updated cluster config defaults so that clustering works
more naturally OOTB

## Issues

[Milestone 3.1.0](https://github.com/rundeck/rundeck/milestone/113)

* [Fix: empty project list shows white box on home page](https://github.com/rundeck/rundeck/pull/5112)
* [Updating transitive dependencies used in plugins](https://github.com/rundeck/rundeck/pull/5111)
* [Address library vulnerabilities](https://github.com/rundeck/rundeck/pull/5110)
* [Issue #5077 - Dependency updates for CVEs: bouncycastle, spring, c3p0, moment.js](https://github.com/rundeck/rundeck/pull/5106)
* [Restores Previous and Next in Execution page #5102](https://github.com/rundeck/rundeck/pull/5103)
* [missing "previous" and "next" execution buttons in rd3rc2](https://github.com/rundeck/rundeck/issues/5102)
* [Fix #5080 by sending the chosen file copier plugin type even if it has no config properties.](https://github.com/rundeck/rundeck/pull/5098)
* [Convert projectStorageType key to lowercase](https://github.com/rundeck/rundeck/pull/5095)
* [Support quartz.threadPool.threadCount parameter in Docker image](https://github.com/rundeck/rundeck/pull/5091)
* [on cluster environment , the job scheduled should respect the remote exec policies](https://github.com/rundeck/rundeck/pull/5089)
* [Referenced Job resolve variables on timeout field #5046](https://github.com/rundeck/rundeck/pull/5084)
* [Error 500 "provider name was null for Service: FileCopier" at the moment of define "stub" on file copier while creating a new Project](https://github.com/rundeck/rundeck/issues/5080)
* [Dependency updates for CVEs: bouncycastle, spring, c3p0, moment.js](https://github.com/rundeck/rundeck/issues/5077)
* [Job Timeout using an Option Value is not honored when a Job is Referenced](https://github.com/rundeck/rundeck/issues/5046)
* [fix #4781 Options from URL not filled when job is scheduled](https://github.com/rundeck/rundeck/pull/5041)

[Milestone 3.1.0-RC2](https://github.com/rundeck/rundeck/milestone/109)

* [Change in the async call for SCM gui #4998](https://github.com/rundeck/rundeck/pull/5076)
* [Uncaught TypeError on jobs page using SCM. #5074](https://github.com/rundeck/rundeck/pull/5075)
* [Uncaught TypeError on jobs page using SCM](https://github.com/rundeck/rundeck/issues/5074)
* [Takeover endpoint with multiple ids correctly using api v32 #5069](https://github.com/rundeck/rundeck/pull/5070)
* [Execution cleanup on a cluster](https://github.com/rundeck/rundeck/pull/5058)
* [Remove unused project param in plugin validation method ](https://github.com/rundeck/rundeck/pull/5054)
* [Capability to compare between variables using ruleset. fixes #5050 ](https://github.com/rundeck/rundeck/pull/5051)
* [Capability to compare variables using ruleset](https://github.com/rundeck/rundeck/issues/5050)
* [Fixes #1654 to make cleanup on records in Workflow, workflow_step and workflow_workflow_step when executions are deleted](https://github.com/rundeck/rundeck/pull/5049)
* [Issues #5002, #4979, #4463, #4464, #4465, #4466 - Update several library dependencies to address reported CVEs.](https://github.com/rundeck/rundeck/pull/5047)
* [oficial docke image: adding custom templates for repository feature.](https://github.com/rundeck/rundeck/pull/5040)
* [reverting changes to allow schedule jobs to respect the remote policy](https://github.com/rundeck/rundeck/pull/5033)
* [Fixes #5030 - This changes make validations if the options from URL is a json object or a simple array list of options…](https://github.com/rundeck/rundeck/pull/5031)
* [Problem with remote URL options validation](https://github.com/rundeck/rundeck/issues/5030)
* [Fix #5023 race condition bug in wf engine](https://github.com/rundeck/rundeck/pull/5029)
* [Option to Enable/Disable notifications for Referenced Jobs #4182](https://github.com/rundeck/rundeck/pull/5026)
* [Upgrades jackson to 2.9.9 to address vulnerabilities](https://github.com/rundeck/rundeck/pull/5019)
* [Adding attribute-match-node-enhancer plugin to the rundeck oss core](https://github.com/rundeck/rundeck/pull/5017)
* [Fix #5000 highlight workflow editor unsaved changes issues](https://github.com/rundeck/rundeck/pull/5014)
* [JNDI info in the configuration page #5010](https://github.com/rundeck/rundeck/pull/5011)
* [Fix: job delete "delete all executions" checkbox, and some form control labels don't work](https://github.com/rundeck/rundeck/pull/5008)
* [more optimized use of pluginControlService](https://github.com/rundeck/rundeck/pull/5007)
* [GUI: job workflow editor has visual problems](https://github.com/rundeck/rundeck/issues/5006)
* [Fix EnhancedNodeService bean was not loaded #5004](https://github.com/rundeck/rundeck/pull/5005)
* [ Node Enhancer feature is not working](https://github.com/rundeck/rundeck/issues/5004)
* [Adds "Cancel" button to UI](https://github.com/rundeck/rundeck/pull/5003)
* [GUI: When editing a job, errors are not clearly shown.](https://github.com/rundeck/rundeck/issues/5000)
* [GUI: "Cancel" option not present when editing Node Sources](https://github.com/rundeck/rundeck/issues/4999)
* [Unable to import SCM changes - URI is too large \>8192](https://github.com/rundeck/rundeck/issues/4998)
* [Sync Rundeck profile from LDAP user attributes for official docker image](https://github.com/rundeck/rundeck/pull/4995)
* [adding bind address to jsch](https://github.com/rundeck/rundeck/pull/4994)
* [Fix Issue #4831 - Adds pagination of tokens on user profile page.](https://github.com/rundeck/rundeck/pull/4990)
* [Small improvement to the API forecast](https://github.com/rundeck/rundeck/pull/4986)
* [3.1: misc ui tweaks](https://github.com/rundeck/rundeck/pull/4984)
* [Remove unreliable test on ReloadablePropertyFileLoginModule.](https://github.com/rundeck/rundeck/pull/4982)
* [Improve performance when copying/editing a job](https://github.com/rundeck/rundeck/pull/4980)
* [Security Scan library updates](https://github.com/rundeck/rundeck/issues/4979)
* [Fix home page project ui not loading #4149](https://github.com/rundeck/rundeck/pull/4977)
* [Update runbook marker parsing fixes #4973 ](https://github.com/rundeck/rundeck/pull/4976)
* [Task/3.1.0 ui review](https://github.com/rundeck/rundeck/pull/4975)
* [Add UI plugins to the new installed plugin list.](https://github.com/rundeck/rundeck/pull/4974)
* [Runbook not shown in job page](https://github.com/rundeck/rundeck/issues/4973)
* [dateformat and locale update](https://github.com/rundeck/rundeck/pull/4972)
* [Changes to fix #4847 - Option enforced with allowed values from remote url was not validating](https://github.com/rundeck/rundeck/pull/4970)
* [Added test and extra if conditional to fix #4959](https://github.com/rundeck/rundeck/pull/4967)
* [upgrade python winrm 2.0.3. Adding support for kerberos authentication](https://github.com/rundeck/rundeck/pull/4966)
* [Execution mode status API with failing code when status is passive.](https://github.com/rundeck/rundeck/pull/4965)
* [Cleanup/prototype scriptaculous](https://github.com/rundeck/rundeck/pull/4963)
* [Fix: potential npe caused by race](https://github.com/rundeck/rundeck/pull/4962)
* [Fix empty error when node exec validation fails](https://github.com/rundeck/rundeck/pull/4961)
* [fix #4958 file copier form always show defaults](https://github.com/rundeck/rundeck/pull/4960)
* [Schedule DayOfWeek via terraform/API fails.](https://github.com/rundeck/rundeck/issues/4959)
* [RD3.1 New Configure Project Form doesn't work as expected](https://github.com/rundeck/rundeck/issues/4958)
* [3.1: motd updates](https://github.com/rundeck/rundeck/pull/4957)
* [Fix: run job with no options causes error](https://github.com/rundeck/rundeck/pull/4956)
* [Add multiple jobs ids to scheduler/takeover endpoint](https://github.com/rundeck/rundeck/pull/4955)
* [3.1: job page shows running executions](https://github.com/rundeck/rundeck/pull/4952)
* [3.1: Updates: node filters in job run page](https://github.com/rundeck/rundeck/pull/4950)
* [fix: don't log full stacktrace for "already being executed" conflict](https://github.com/rundeck/rundeck/pull/4949)
* [3.1: Nodes updates](https://github.com/rundeck/rundeck/pull/4947)
* [3.1: event access update](https://github.com/rundeck/rundeck/pull/4946)
* [UI: Job Show/Run page updates](https://github.com/rundeck/rundeck/pull/4945)
* [Update repository dependency.](https://github.com/rundeck/rundeck/pull/4942)
* [UI fixes, minor css and html changes, flattened](https://github.com/rundeck/rundeck/pull/4937)
* [3.1: UI: project config form tabs](https://github.com/rundeck/rundeck/pull/4935)
* [Fix #4778. Normalize JAAS debugging log messages.](https://github.com/rundeck/rundeck/pull/4933)
* [Ability to specify -b variable for SSH](https://github.com/rundeck/rundeck/issues/4932)
* [Fix #4930 for ReloadablePropertyFileLoginModule.](https://github.com/rundeck/rundeck/pull/4931)
* [Jaas login using PropertyFileLoginModule adds username as a role](https://github.com/rundeck/rundeck/issues/4930)
* [enabling option plugins by default](https://github.com/rundeck/rundeck/pull/4929)
* [Fix #4894 invalid sourcemappingurl causes 404](https://github.com/rundeck/rundeck/pull/4923)
* [Add validationQuery parameter](https://github.com/rundeck/rundeck/pull/4872)
* [Option enforced with allowd values from remote url not validating](https://github.com/rundeck/rundeck/issues/4847)
* [UI progress bar : display issue](https://github.com/rundeck/rundeck/issues/4625)
* [Timezone in execution log output](https://github.com/rundeck/rundeck/issues/4486)
* [Option to Enable/Disable notifications for Referenced Jobs](https://github.com/rundeck/rundeck/issues/4182)
* [RD3 GUI: When displaying overview page with a big amount of projects ( over 15 ) the last ones may not load "configure" and  "create job" buttons correctly](https://github.com/rundeck/rundeck/issues/4149)
* [Tooltip always shows the wrong time (unless timezone is equal to Zulu/UTC)](https://github.com/rundeck/rundeck/issues/3518)

[Milestone 3.1.0-RC1](https://github.com/rundeck/rundeck/milestone/80)
* [Update Docker images to be compatible with OpenShift](https://github.com/rundeck/rundeck/pull/4826)
* [Making the translation of messages to Brazilian Portuguese](https://github.com/rundeck/rundeck/pull/4523)
* [Misc nodes page UI updates](https://github.com/rundeck/rundeck/pull/4521)
* [removed UUID validation of jobRef](https://github.com/rundeck/rundeck/pull/4516)
* [Improvement for audit.log file size](https://github.com/rundeck/rundeck/pull/4515)
* [Add secondary node filter for jobs: Exclude filter](https://github.com/rundeck/rundeck/pull/4509)
* [Node status UI attributes](https://github.com/rundeck/rundeck/pull/4508)
* [Update copyright](https://github.com/rundeck/rundeck/issues/4504)
* [Fix #4488. Help Grails write output stream correctly on Tomcat 7.](https://github.com/rundeck/rundeck/pull/4503)
* [Render the SSO login button in a more sensible way.](https://github.com/rundeck/rundeck/pull/4500)
* [Update repository version.](https://github.com/rundeck/rundeck/pull/4495)
* [API job import fails on WriteListener error](https://github.com/rundeck/rundeck/issues/4488)
* [Feature/community news component](https://github.com/rundeck/rundeck/pull/4485)
* [New scheduled execution stats table ](https://github.com/rundeck/rundeck/pull/4482)
* [Fix project/framework prop resolution](https://github.com/rundeck/rundeck/pull/4476)
* [Some framework/project plugin property resolution is not correct](https://github.com/rundeck/rundeck/issues/4475)
* [Notification email template with log output: use blank when not included](https://github.com/rundeck/rundeck/pull/4474)
* [Job options hidden1](https://github.com/rundeck/rundeck/pull/4472)
* [UUID validation of jobRef breaks bulk import of jobs in clean instance of RunDeck](https://github.com/rundeck/rundeck/issues/4471)
* [Disable JobStats because it produces a deadlock on mssql.](https://github.com/rundeck/rundeck/pull/4468)
* [Updates the Copyright date in the footer and licenses page](https://github.com/rundeck/rundeck/pull/4461)
* [Feature/version notification](https://github.com/rundeck/rundeck/pull/4460)
* [Cleanup: orchestrator node processor logging](https://github.com/rundeck/rundeck/pull/4457)
* [Fix #4454 resume correct step context after handler](https://github.com/rundeck/rundeck/pull/4455)
* [Wrong step id logged in rdlog when error handler executed](https://github.com/rundeck/rundeck/issues/4454)
* [Fix race condition/workflow state bug](https://github.com/rundeck/rundeck/pull/4453)
* [Remove extraneous login module config](https://github.com/rundeck/rundeck/pull/4452)
* [Docker - Fix key for project storage type](https://github.com/rundeck/rundeck/pull/4446)
* [Docker - Unable to set project storage type](https://github.com/rundeck/rundeck/issues/4445)
* [Fix rd-acl tool in Docker image](https://github.com/rundeck/rundeck/pull/4444)
* [Notification email template with log output: use blank when not included](https://github.com/rundeck/rundeck/issues/4443)
* [Add API endpoint that allows a user to list their roles](https://github.com/rundeck/rundeck/pull/4441)
* [rd-acl not running on docker version: /home/greg/.sdkman/candidates/java/8.0.172-zulu/jre/bin/java: No such file or directory](https://github.com/rundeck/rundeck/issues/4436)
* [Very large Rundeck.audit.log](https://github.com/rundeck/rundeck/issues/4435)
* [Add tomcat api tests](https://github.com/rundeck/rundeck/pull/4433)
* [Fix #4179 kill job reference thread when parent is killed](https://github.com/rundeck/rundeck/pull/4432)
* [Job detail in execution xml for log storage](https://github.com/rundeck/rundeck/pull/4431)
* [loginmodule unnecessary overwritten in Docker instance ignoring file](https://github.com/rundeck/rundeck/issues/4430)
* [Docker CSP config: Ensure newlines are added when options are rendered](https://github.com/rundeck/rundeck/pull/4424)
* [Docker - CSP config overrides on same line](https://github.com/rundeck/rundeck/issues/4423)
* [Job detail in execution xml for log storage](https://github.com/rundeck/rundeck/issues/4414)
* [Adds Plugin provider metadata](https://github.com/rundeck/rundeck/pull/4393)
* [Add option values plugin type. Fixes issue 77](https://github.com/rundeck/rundeck/pull/4344)
* [Killing parent job doesn't kill running child jobs ( in 3.0.x , not able to kill parent job )](https://github.com/rundeck/rundeck/issues/4179)
* [Feature request : hide unmodifyable job options, such as secrets](https://github.com/rundeck/rundeck/issues/4135)
* [Job step skipped for no apparent reason](https://github.com/rundeck/rundeck/issues/3443)
* [Local script option provider](https://github.com/rundeck/rundeck/issues/77)

## Contributors

* Alberto Hormazabal (ahormazabal)
* Alex Honor (ahonor)
* Antoine Leroyer (aleroyer)
* Can Hanhan (finarfin)
* carlos (carlosrfranco)
* Diego Queiroz (DiegoQueiroz)
* Evan Farrell (moosilauke18)
* Greg Schueler (gschueler)
* Greg Zapp (ProTip)
* Jaime Tobar (jtobard)
* Jesse Marple (jessemarple)
* Luis Toledo (ltamaster)
* Stefan Kirrmann (kirrmann)
* Stephen Joyner (sjrd218)

## Bug Reporters

* ahormazabal
* aleroyer
* am312
* boudekerk
* carlosrfranco
* cwaltherf
* DiegoQueiroz
* finarfin
* gschueler
* hs-hub-world
* jairov4
* javiergoni
* jbanda15
* jessemarple
* JPst
* jtobard
* kirrmann
* ltamaster
* marcbejerano
* MegaDrive68k
* menathor
* moosilauke18
* nmamn
* ProTip
* RolandVExp
* sebastianbello
* sjrd218
* tintranvan
