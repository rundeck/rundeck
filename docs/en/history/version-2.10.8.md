% Version 2.10.8
% greg
% 03/23/2018

Release 2.10.8
===========

Date: 2018-03-23

Name: <span style="color: DodgerBlue"><span class="glyphicon glyphicon-bell"></span> "cafe mocha dodgerblue bell"</span>

## Upgrading from Earlier versions

* See the [Upgrading Guide](../upgrading/index.html)

## Notes

Bug Fixes:

* Fix an issue causing Vault key storage plugin to not work
* Fix issue with cluster mode using MSSQL or Oracle DB field types. Note: If using those DBs see [#3125](https://github.com/rundeck/rundeck/issues/3125)

Enhancements:

* SCM git plugin enhancements [#3192](https://github.com/rundeck/rundeck/pull/3192):
    * Import can delete jobs which were removed from git
    * API updated to include synch status for jobs
    * API updated to allow specifying list of jobs to delete during import action

Other changes:

* Ansible plugin upgraded to version 2.3.0
* API version &rarr; 22

## Contributors

* Antoine-Auffret
* Greg Schueler (gschueler)
* Jaime Tobar (jtobard)
* Luis Toledo (ltamaster)

## Bug Reporters

* Antoine-Auffret
* gschueler
* jplassnibatt
* jtobard
* komodo472
* ltamaster
* sebastianbello

## Issues

[Milestone 2.10.8](https://github.com/rundeck/rundeck/milestone/72)

* [Update/apiv22 scm import](https://github.com/rundeck/rundeck/pull/3216)
* [API: SCM Git plugin Import action 'import-all' should be renamed to 'import-jobs'](https://github.com/rundeck/rundeck/issues/3215)
* [fix: update dependencies flagged by snyk](https://github.com/rundeck/rundeck/pull/3213)
* [Upgrade ansible plugin to the 2.3.0 release](https://github.com/rundeck/rundeck/pull/3202)
* [Fix: key storage data type not set automatically with Vault plugin](https://github.com/rundeck/rundeck/pull/3196)
* [Scm Git Import can delete jobs when git file is removed](https://github.com/rundeck/rundeck/pull/3192)
* [correct optional options when creating a project](https://github.com/rundeck/rundeck/pull/3191)
* [Referenced job error handlers cannot be found when job is referenced from other projects](https://github.com/rundeck/rundeck/issues/3189)
* [Fix: Matched Nodes list is not (Auto) Refreshed in Jobs](https://github.com/rundeck/rundeck/pull/3171)
* [Can't abort a job, message is "Unable to modify the execution"](https://github.com/rundeck/rundeck/issues/3155)
* [serverNodeUUID field to varchar ](https://github.com/rundeck/rundeck/pull/3126)
* [Oracle+MSSQL DB error SqlExceptionHelper](https://github.com/rundeck/rundeck/issues/3125)
* [Log File Storage exception: NullPointerException: Cannot get property 'filetype' on null object](https://github.com/rundeck/rundeck/issues/3089)
* [Bug / Matched Nodes list is not (Auto) Refreshed in Jobs](https://github.com/rundeck/rundeck/issues/3075)
* [API: Scm (export/import) action inputs should include item status](https://github.com/rundeck/rundeck/issues/2330)
