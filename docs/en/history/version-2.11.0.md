% Version 2.11.0
% greg
% 04/27/2018

Release 2.11.0
===========

Date: 2018-04-27

Name: <span style="color: Olive"><span class="glyphicon glyphicon-music"></span> "cappuccino olive music"</span>

## Upgrading from Earlier versions

* See the [Upgrading Guide](../upgrading/index.html)

## Notes

New Features:

* SCM/Git plugin: 
	* works in Cluster mode
	* improvements to jobs page load speed
	* can disable SCM status checks in the GUI
* Projects: 
	* Set a Label to display instead of the name
	* new default: projectsStorageType defaults to `db` (see below)
* Job reference steps:
	* Can reference jobs via UUID as well as group/name, so you can rename jobs [#3115](https://github.com/rundeck/rundeck/pull/3115)
	* Deleting a job will check if any steps reference the job and warn you
	* Automatically pass job options to the job reference [#3056](https://github.com/rundeck/rundeck/pull/3056)
	* Notifications for referenced jobs are triggered
	* Statistics (success/duration) of referenced jobs are updated
	* Timeout for referenced jobs is enforced
	* Choose behavior if the referenced job is disabled: fail or pass
	* (see [#3040](https://github.com/rundeck/rundeck/pull/3040))
* Job Options: 
	* Set a Label to display instead of the name
	* Multivalue option delimiter is available in data context as `${option.name.delimiter}`/`$RD_OPTION_NAME_DELIMITER`
* Plugins: admins can disable/enable plugins for a project in the GUI or via project config [#3122](https://github.com/rundeck/rundeck/pull/3122)
* Job Node Threadcount: can use an option value
* Job Notification:
	* Can send a notification if the job duration exceeds an absolute time, or relative to the job's average [#3087](https://github.com/rundeck/rundeck/pull/3087)
	* Separate notifications for failure vs. retryable failure (i.e.) [#2864](https://github.com/rundeck/rundeck/pull/2864)
* Job Definition visibility ACL:
	* A new ACL access level `view` is a subset of the `read` access level for jobs, and does not allow users to view the "Definition" tab of a Job, or download the XML/YAML definitions.
* Plugin development:
	* dynamic list of Select value inputs for Java plugins
	* Documentation for LogFilter and ContentConverter plugins added
	* Groovy Plugin DSL added for LogFilter and ContentConverter plugins
* Execution View:
	* Can link or redirect to the Log Output tab for Executions
* Internationalization:
	* Improvements, and French translation added by PeekLeon! 👏👏 🇫🇷
* API:
	* Added endpoints for writeable Resource Model Sources (such as built in File plugin)
	* List project sources, retrieve node definitions from each source
	* Writeable sources allow updating node data via API

Bug fixes:

* many

Potentially breaking changes:

**RPM spec:**

The `rundeck` user/group is now created within system UID ranges [#3195](https://github.com/rundeck/rundeck/pull/3195).

**ACLs: Edit Project Configuration/Nodes GUI access level requirements changed:**

Previously: GUI actions "Project > Edit Configuration" and "Project > Edit Nodes" required `admin` project access. Now: only `configure` level access is required.

NOTE: API behavior was always this way, so this change simply aligns the access requirements.

Potential security implications:

* users/roles granted `configure` access to a project will now be able to modify Project Nodes or Configuration via the GUI
* the same users/roles would already have this access if using the API

See: [#3084](https://github.com/rundeck/rundeck/pull/3084)

**ACLs: Job Definition visibility**

A new ACL access level `view` is a subset of the `read` access level for jobs, and does not allow users to view the "Definition" tab of a Job, or download the XML/YAML definitions.

ACLs which allow `read` to Jobs, will work as before. To disallow Job Definition viewing/downloading, you should change your ACLs to only allow `view` access.

**Project Storage Type is now `db` by default:**

If you want to continue using filesystem storage for project config/readme/motd files, you will need to set this in your `rundeck-config.properties` before upgrading:

    rundeck.projectsStorageType=filesystem

Upgrading an existing `filesystem` configuration to `db` is automatic, and project configs/readme/motd will be loaded into DB storage at system startup.

To encrypt the DB storage, you will need to [enable encryption for the "Project Configuration" storage layer](../plugins-user-guide/bundled-plugins.html#jasypt-encryption-plugin).

## Contributors

* Antoine-Auffret
* Greg Schueler (gschueler)
* Jaime Tobar (jtobard)
* Luis Toledo (ltamaster)
* RNavarro (ronave)
* Romain LEON (PeekLeon)
* Steven Grimm
* carlos (carlosrfranco)
* jbguerraz
* jtobard
* mickymiek

## Bug Reporters

* MustaphaB1
* PeekLeon
* TomGudman
* aaronmaxlevy
* ahonor
* carlosrfranco
* csgyuricza
* dbeckham
* dirkniblick
* emiliohh
* giovanimarin
* gschueler
* jbguerraz
* johnpaularthur
* joshuaspence
* jtobard
* komodo472
* ltamaster
* makered
* mathieuchateau
* pawadski
* robinwolny
* ronave
* sebastianbello

## Issues

[Milestone 2.11.0](https://github.com/rundeck/rundeck/milestone/65)

* [Support groovy plugin DSL for LogFilter/ContentConverter plugins](https://github.com/rundeck/rundeck/pull/3319)
* [Bug: Errors redirect to non-existent page when editing Nodes using GUI](https://github.com/rundeck/rundeck/issues/3317)
* [Follow execution enhancements](https://github.com/rundeck/rundeck/pull/3304)
* [Invalid aclpolicy yaml causes stacktrace](https://github.com/rundeck/rundeck/issues/3301)
* [Enable Internationalization for Project config level settings](https://github.com/rundeck/rundeck/issues/3300)
* [API: add writeable resource model endpoints](https://github.com/rundeck/rundeck/pull/3297)
* [Internationalization of views/user/login](https://github.com/rundeck/rundeck/pull/3295)
* [ French language selection menu ](https://github.com/rundeck/rundeck/pull/3281)
* [Fix #3048 hide add log filter button for job refs](https://github.com/rundeck/rundeck/pull/3278)
* [Add "Initial Execution ID" as a context variable for Job Retries jobs:](https://github.com/rundeck/rundeck/pull/3276)
* [Make rundeck.projectsStorageType=db default](https://github.com/rundeck/rundeck/issues/3275)
* [Fix git-import plugin error ](https://github.com/rundeck/rundeck/pull/3274)
* [SCM import plugin error when fetching remote changes](https://github.com/rundeck/rundeck/issues/3273)
* [Translation to french](https://github.com/rundeck/rundeck/pull/3272)
* [Create empty resources file](https://github.com/rundeck/rundeck/pull/3271)
* [Fixing Job Filter just show 20 jobs: https://github.com/rundeck/runde…](https://github.com/rundeck/rundeck/pull/3270)
* [Fixing Scheduled jobs fail when using roleNameAttribute="distinguishe…](https://github.com/rundeck/rundeck/pull/3267)
* [Fix Bug 3256](https://github.com/rundeck/rundeck/pull/3265)
* [Disable JSON check for remote options](https://github.com/rundeck/rundeck/pull/3260)
* [Fix #3258 add user.login info to setup input](https://github.com/rundeck/rundeck/pull/3259)
* [BUG: "Description" field is blanked when "Project Name" is an invalid string](https://github.com/rundeck/rundeck/issues/3256)
* [adding passphrase storage in simple configuration GUI](https://github.com/rundeck/rundeck/pull/3255)
* [Project free form label](https://github.com/rundeck/rundeck/pull/3253)
* [GRPC ready](https://github.com/rundeck/rundeck/pull/3251)
* [Cannot create an empty resources.xml file](https://github.com/rundeck/rundeck/issues/3246)
* [Scheduled jobs fail when using roleNameAttribute="distinguishedName" in the AD configuration ](https://github.com/rundeck/rundeck/issues/3242)
* [Fix issues with test ordering](https://github.com/rundeck/rundeck/pull/3236)
* [Correct storage type from "filesystem" to "file" (Documentation only)](https://github.com/rundeck/rundeck/pull/3230)
* [Fix 500 error api response for running executions with invalid project name](https://github.com/rundeck/rundeck/pull/3228)
* [Malformed Request Breaks API](https://github.com/rundeck/rundeck/issues/3223)
* [Job Filter just show 20 jobs](https://github.com/rundeck/rundeck/issues/3221)
* [Add "Initial Execution ID"  as a context variable for  Job Retries  jobs](https://github.com/rundeck/rundeck/issues/3207)
* [Edit job doesn't always change the schedule owner](https://github.com/rundeck/rundeck/pull/3204)
* [Changing RPM Spec file to add rundeck user and group within system UI…](https://github.com/rundeck/rundeck/pull/3195)
* [Option "label" field for display name](https://github.com/rundeck/rundeck/pull/3135)
* [Plugins control](https://github.com/rundeck/rundeck/pull/3122)
* [Job Reference by UUID](https://github.com/rundeck/rundeck/pull/3115)
* [Scm cluster config](https://github.com/rundeck/rundeck/pull/3102)
* [Average notification duration enhancement](https://github.com/rundeck/rundeck/pull/3087)
* [On Retryable Failure notification is erroneously selected by default](https://github.com/rundeck/rundeck/issues/3086)
* [Fix #3012 admin access required to modify project config/nodes in gui](https://github.com/rundeck/rundeck/pull/3084)
* [Allow option value for thread count](https://github.com/rundeck/rundeck/pull/3068)
* [Checking for dynamic properties unless the step is a "plugin" type](https://github.com/rundeck/rundeck/pull/3066)
* [Customize average duration condition for the notification plugin.](https://github.com/rundeck/rundeck/issues/3064)
* [JobRef import options](https://github.com/rundeck/rundeck/pull/3056)
* [Project description](https://github.com/rundeck/rundeck/pull/3054)
* [Enhancement Request / project.ssh-key-passphrase-storage-path not shown in  "simple configuration" GUI](https://github.com/rundeck/rundeck/issues/3051)
* [Exposing the delimiter on a environment variable for multi-valued opt…](https://github.com/rundeck/rundeck/pull/3050)
* [GUI should not allow Log Filter to be added to a Job Reference workflow step](https://github.com/rundeck/rundeck/issues/3048)
* [Scm speed enhancement](https://github.com/rundeck/rundeck/pull/3046)
* [Documentation: Log Filter/Content Converter plugins](https://github.com/rundeck/rundeck/issues/3042)
* [Better child job support](https://github.com/rundeck/rundeck/pull/3040)
* [API: add upload endpoints for writeable model sources, replacing old resources API](https://github.com/rundeck/rundeck/issues/3037)
* [Extend plugin interface to dynamically read configuration input choices](https://github.com/rundeck/rundeck/pull/3029)
* [Add project description to the project table.](https://github.com/rundeck/rundeck/issues/3027)
* [Import options defined for Job when it is added as a workflow step](https://github.com/rundeck/rundeck/issues/3022)
* [ACLs for project admin and delete_execution are mutually exclusive](https://github.com/rundeck/rundeck/issues/3012)
* [Enhancement request: Not able to load JSON remote options - Rundeck requires the content-type header to be set to 'application/json'](https://github.com/rundeck/rundeck/issues/2922)
* [Job Timeout & Kill Job Does Not Halt Job Steps](https://github.com/rundeck/rundeck/issues/2911)
* [Separate notifications for retryable failures](https://github.com/rundeck/rundeck/pull/2864)
* [Add unicode icons to HTML page title for execution results](https://github.com/rundeck/rundeck/pull/2791)
* [Rundeck don't update job name's reference after to change job's name](https://github.com/rundeck/rundeck/issues/2701)
* [Exposing the delimiter on a environment variable for multi-valued options](https://github.com/rundeck/rundeck/issues/2554)
* [Allow option value for thread count](https://github.com/rundeck/rundeck/issues/2440)
* [job status "killed" when running a job with the job refrence error handler on multiple nodes](https://github.com/rundeck/rundeck/issues/2222)
* [How to trigger child jobs using email notifications](https://github.com/rundeck/rundeck/issues/1841)
* [SCM: not compatible with clustering/HA](https://github.com/rundeck/rundeck/issues/1622)
* [Child job notifications don't trigger](https://github.com/rundeck/rundeck/issues/1574)
* [After deleting a project, Log file storage resume might cause exception in logs](https://github.com/rundeck/rundeck/issues/1384)
* [Feature request prevent job download as XML/YAML](https://github.com/rundeck/rundeck/issues/1167)
* [Workflow broken after job rename](https://github.com/rundeck/rundeck/issues/1155)
* [Feature Request: redirect to 'Log Output' tab after submitting a job instead of summary tab](https://github.com/rundeck/rundeck/issues/1144)
* [Feature Request: job execution links with #output should take you to the actual tab](https://github.com/rundeck/rundeck/issues/1107)
* [Feature Request: When using the retry option, allow option to suppress notifications until the last execution](https://github.com/rundeck/rundeck/issues/1067)
* [Job delete/rename: should check whether any job references would break](https://github.com/rundeck/rundeck/issues/257)
