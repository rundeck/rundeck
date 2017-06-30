% Version 2.8.3
% greg
% 06/30/2017

Release 2.8.3
===========

Date: 2017-06-30

Name: <span style="color: OrangeRed"><span class="glyphicon glyphicon-flash"></span> "cafe cubano orangered flash"</span>

## Upgrading from 2.8.0

**Important Note**: please see the [Upgrading Guide](../upgrading/index.html) if you are using Mysql or H2 database,
and are upgrading from Rundeck 2.8.0.

A DB schema change requires a manual fix before upgrading.

## Upgrading from Earlier versions

* See the [Upgrading Guide](../upgrading/index.html)

## Notes

This release is primarily bug fixes. 

Some other changes:

* If a step has an Error Handler that is marked as "keepgoing on success", then Error level log output from the step is changed to Normal level
* Executions that were running when the Rundeck server was shutdown will be set to "Incomplete" when the server restarts.
* Some internal library versions were upgraded

## Contributors

* Alex Honor (ahonor)
* Greg Schueler (gschueler)
* Loïc Blot (nerzhul)
* Sean Reifschneider (linsomniac)
* jtobard
* ltamaster

## Bug Reporters

* arthurlogilab
* cr42
* flapp
* gschueler
* jtobard
* kmusard
* linsomniac
* ltamaster
* matthewsrogers
* nerzhul
* ronave
* schast
* shoelzle
* soar
* tomdeblende
* uralk
* willemdh
* zionio

## Issues

[Milestone 2.8.3](https://github.com/rundeck/rundeck/milestone/55)

* [Add documentation for unlimited max token expiration time if set to 0](https://github.com/rundeck/rundeck/pull/2599)
* [single valued options with "selected=true" fetched from remote URL don't work](https://github.com/rundeck/rundeck/issues/2585)
* [2.8: ACL policy causes exception for matching clause of a resource attribute that is not present](https://github.com/rundeck/rundeck/issues/2559)
* [single valued options with "selected=true" fetched from remote URL should not used on run again](https://github.com/rundeck/rundeck/issues/2552)
* [Framework ssh timeout value used for both connect and command max time](https://github.com/rundeck/rundeck/issues/2547)
* [Potential fix to #2538](https://github.com/rundeck/rundeck/pull/2539)
* [Exception: property not found: framework.var.dir](https://github.com/rundeck/rundeck/issues/2538)
* [Exception when exporting archive without project_acl authorization](https://github.com/rundeck/rundeck/issues/2528)
* [Problem adding the first api token](https://github.com/rundeck/rundeck/issues/2525)
* [Strange code editor behavior: delete option doesn't work](https://github.com/rundeck/rundeck/issues/2523)
* [Fix problem with S3 plugin](https://github.com/rundeck/rundeck/pull/2512)
* [Documentation: Changing wording of project global variable override.](https://github.com/rundeck/rundeck/pull/2510)
* [Issue exporting job with node intersection](https://github.com/rundeck/rundeck/issues/2503)
* [Documentation fix: java 8 is required](https://github.com/rundeck/rundeck/pull/2502)
* [js issue when running job from popup](https://github.com/rundeck/rundeck/issues/2494)
* [Generated tokens always are created with same user roles](https://github.com/rundeck/rundeck/issues/2492)
* [Job Editor completion helper breaks when editing a job with a file upload](https://github.com/rundeck/rundeck/issues/2487)
* [LocalCommand: empty option variable is not expanded](https://github.com/rundeck/rundeck/issues/2486)
* [SCM GIT job exports different on different servers](https://github.com/rundeck/rundeck/issues/2483)
* [java.lang.NullPointerException on debian jessie install ](https://github.com/rundeck/rundeck/issues/2481)
* [Update postgresql connector to 42.0.0](https://github.com/rundeck/rundeck/pull/2480)
* [API: Rundeck 2.8.0, Token creation response for api v18 request is incorrect](https://github.com/rundeck/rundeck/issues/2479)
* [Reduce Error logs on steps with Error Handler](https://github.com/rundeck/rundeck/pull/2474)
* [Ad hoc scheduled executions could not be rescheduled after a restart and will be killed](https://github.com/rundeck/rundeck/issues/2470)
* [Cleaned up executions marked as "incomplete"](https://github.com/rundeck/rundeck/pull/2466)
* [Add autocomplete to notification fields in job editor](https://github.com/rundeck/rundeck/pull/2462)
* [Improve slow db queries related to activity page and home page](https://github.com/rundeck/rundeck/pull/2457)
* [Update not-yet-commons-ssl dependency to 0.3.17](https://github.com/rundeck/rundeck/pull/2454)
* [Update jackson-databind to 2.8.8.1](https://github.com/rundeck/rundeck/pull/2453)
* [Upgrade jsch to 0.1.54](https://github.com/rundeck/rundeck/pull/2450)
* [Simplify rdb queries for project home page statistics](https://github.com/rundeck/rundeck/issues/2448)
* [Scheduled job executions broken after restart](https://github.com/rundeck/rundeck/issues/2271)
* [Rescheduling ad hoc execution fails when restarting rundeckd](https://github.com/rundeck/rundeck/issues/2167)
* [rd-acl test suggests incorrect policy for key storage](https://github.com/rundeck/rundeck/issues/1626)
