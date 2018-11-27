Release 3.0.9
===========

Date: 2018-11-27

Name: <span style="color: gold"><span class="glyphicon glyphicon-gift"></span> "jalapeño popper gold gift"</span>

## Notes

Bug fixes and updates to the docker image.

* We have added new ACLs allowing access to Git/SCM import (job create, update, delete) without allowing normal GUI or API modifications.
* New: test JAAS login configuration from the commandline.

## Contributors

* Dave Riseley (driseley)
* Greg Schueler (gschueler)
* Greg Zapp (ProTip)
* Jaime Tobar (jtobard)
* Jesse Marple (jessemarple)
* Luis Toledo (ltamaster)
* Petr (petrkle)
* Stefan Kirrmann (kirrmann)
* Stephen Joyner (sjrd218)
* miguelantonio

## Bug Reporters

* ProTip
* aparedero
* driseley
* gjvc
* gschueler
* gtsteffaniak
* jessemarple
* jplassnibatt
* jtobard
* kirrmann
* ltamaster
* miguelantonio
* mlamutt
* pchevallereau
* petrkle
* plambert
* sebastianbello
* sjrd218
* wilreichert

## Issues

[Milestone 3.0.9](https://github.com/rundeck/rundeck/milestone/92)

* [Fix #4020 indent job groups in tree](https://github.com/rundeck/rundeck/pull/4253)
* [Remove extranious spaces from config](https://github.com/rundeck/rundeck/pull/4252)
* [Fix node display to use single line per node](https://github.com/rundeck/rundeck/pull/4251)
* [The nodes page each node takes up 2 lines](https://github.com/rundeck/rundeck/issues/4250)
* [Adds ID to tour list item](https://github.com/rundeck/rundeck/pull/4248)
* [Docker - Allow configurable MaxRAMFraction](https://github.com/rundeck/rundeck/issues/4244)
* [Disable toggle off of first run message](https://github.com/rundeck/rundeck/pull/4243)
* [Fix link to docs for Email notification help text](https://github.com/rundeck/rundeck/pull/4241)
* [Fix flow control status handling for job state conditional step](https://github.com/rundeck/rundeck/pull/4235)
* [Fixes #4141. Use combined shared context from the last step when executing the error handler](https://github.com/rundeck/rundeck/pull/4233)
* [Fixes ehcache warn message on bootstrap.](https://github.com/rundeck/rundeck/pull/4228)
* [3.0.8: WARNING message appearing at startup :  n.s.ehcache.config.ConfigurationFactory : No configuration found](https://github.com/rundeck/rundeck/issues/4227)
* [Set an ID for the "first run message" div](https://github.com/rundeck/rundeck/pull/4222)
* [scm_update acl special permission](https://github.com/rundeck/rundeck/pull/4220)
* [Grails MaxUploadSize should be configurable for Docker ](https://github.com/rundeck/rundeck/issues/4216)
* [Fix camelcase keys in remco template for docker image](https://github.com/rundeck/rundeck/pull/4207)
* [Docker image env variable RUNDECK_JAAS_LDAP_ROLEPREFIX doesn't work](https://github.com/rundeck/rundeck/issues/4203)
* [Command line tester for checking Jaas auth setup. ](https://github.com/rundeck/rundeck/pull/4202)
* [Fix #3452 and #3987 by adding a login module that can hot reload realm.properties](https://github.com/rundeck/rundeck/pull/4194)
* [rundeck-config.properties containing blankspaces](https://github.com/rundeck/rundeck/issues/4189)
* [Automatically generated server UUID in rundeck docker container breaks scheduled jobs](https://github.com/rundeck/rundeck/issues/4181)
* [Adding Services to the DynamicProperties interface](https://github.com/rundeck/rundeck/pull/4180)
* [job state conditional is not working since rundeck 2.9.x](https://github.com/rundeck/rundeck/issues/4178)
* [Fix for Rundeck #4167 - Null NotificationPlugin config](https://github.com/rundeck/rundeck/pull/4171)
* [Notification Plugins with no job level configuration fail in Rundeck 3.0.8](https://github.com/rundeck/rundeck/issues/4167)
* [Variables are not getting passed to a Workflow step's error handler](https://github.com/rundeck/rundeck/issues/4141)
* [Referenced Job successOnEmptyNodeFilter](https://github.com/rundeck/rundeck/pull/4103)
* ["continue on empty node set" doesn't work in a referenced job](https://github.com/rundeck/rundeck/issues/4077)
* [Allow GIT import from SCM without "update" access in the ACL](https://github.com/rundeck/rundeck/issues/4058)
* [Rundeck 3: jobs view should be tree-based, as in Rundeck 2.x](https://github.com/rundeck/rundeck/issues/4020)
* [3.0.x and above no longer accepts "refreshInterval" in jaas-loginmodule.conf](https://github.com/rundeck/rundeck/issues/3987)
* [When i add new user, it's obligatory to restart Rundeck](https://github.com/rundeck/rundeck/issues/3452)
* [improve JettyCachingLdapLoginModule](https://github.com/rundeck/rundeck/issues/391)
