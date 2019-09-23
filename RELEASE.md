# Release 3.1.1

Date: 2019-09-23

Name: <span style="color: pink"><span class="glyphicon glyphicon-plane"></span> "mozzarella stick pink plane"</span>

## Upgrading
See the upgrade documentation [here](https://docs.rundeck.com/3.1.0-rc2/upgrading/upgrade-to-rundeck-3.1.html).

## Enhancements

### Webhooks (incubating)
Much anticipated and maximally useful.. Webhooks have landed in Rundeck! This new incubating feature
empowers Rundeck to receive JSON events and direct them to jobs.
Check out the [Webhook Docs](https://docs.rundeck.com/3.1.1/manual/12-webhooks.html) for
instructions on enabling the feature and full details.

![Destroy your FOMO, never miss an important event!](https://docs.rundeck.com/assets/releases/3_1_1/webhook_promo_pd_sm.gif "Destroy your FOMO, never miss an important event!")


### Job Options
Job options display has been enhanced in the activities list and execution view. This should
provide better visual separation between the option names and option values.

![Contrasty!](https://docs.rundeck.com/assets/releases/3_1_1/job_opts.png "Contrasty!")

### Development

* Allow notification to use workflow exported variables [#5139](https://github.com/rundeck/rundeck/pull/5139)
* API to list installed plugins [#5259](https://github.com/rundeck/rundeck/pull/5259)
* Externalize Vue in webpack builds [#5217](https://github.com/rundeck/rundeck/pull/5217)

### Docker

* Added ldap nestedGroups setting to remco template [#5100](https://github.com/rundeck/rundeck/pull/5100)

### Misc

* 🌈 Upgrade font awesome to 5.10.2 [#5269](https://github.com/rundeck/rundeck/pull/5269)
* Execution log parsing has been speed up to improve log loading speed [#5253](https://github.com/rundeck/rundeck/pull/5253)
* New `scm_import` and `scm_export` ACLs for job writers [#5176](https://github.com/rundeck/rundeck/pull/5176)

## Bug Fixes

* Fix exception on average duration exceeded notification [#5153](https://github.com/rundeck/rundeck/pull/5153)
* Plugin uninstaller can now uninstall manually installed plugins [#5258](https://github.com/rundeck/rundeck/pull/5258)
* Fix error when editing jobs option with enforced values [#5146](https://github.com/rundeck/rundeck/pull/5146)
* Display correct time on job activity page [#5125](https://github.com/rundeck/rundeck/issues/5125)

## Issues

[Milestone 3.1.1](https://github.com/rundeck/rundeck/milestone/115)

* [cleanup argstring display in activity list and exec page](https://github.com/rundeck/rundeck/pull/5277)
* [Fix i18n in plugin repository UI](https://github.com/rundeck/rundeck/pull/5276)
* [Fix mispelled word. Fixes #5241.](https://github.com/rundeck/rundeck/pull/5270)
* [Upgrade font awesome to 5.10.2. Fixes #4756](https://github.com/rundeck/rundeck/pull/5269)
* [Fix #5261. Do not set not null constraint on AuthToken type. ](https://github.com/rundeck/rundeck/pull/5263)
* [(3.1.1-snapshot) db error on upgrade due to webhooks](https://github.com/rundeck/rundeck/issues/5261)
* [Turn off autocomplete on password field](https://github.com/rundeck/rundeck/pull/5260)
* [Fix #5193. Plugin uninstaller can now uninstall manually installed plugins](https://github.com/rundeck/rundeck/pull/5258)
* [Fix #5248. Option validator error](https://github.com/rundeck/rundeck/pull/5255)
* [Improve execution log parsing speed with CompileStatic](https://github.com/rundeck/rundeck/pull/5253)
* [Unable to restrict option to allowed values when using options provider plugin](https://github.com/rundeck/rundeck/issues/5248)
* [JSCH Documentation Plugin Broken](https://github.com/rundeck/rundeck/issues/5244)
* [Fix #5232 by adding the display attribute to PluginMeta.](https://github.com/rundeck/rundeck/pull/5236)
* [Docker - Add webhooks feature flag to template](https://github.com/rundeck/rundeck/pull/5233)
* [not able to install plugins from "plugin repository" UI](https://github.com/rundeck/rundeck/issues/5232)
* [Add creation date to project list on api](https://github.com/rundeck/rundeck/pull/5223)
* [Externalize Vue](https://github.com/rundeck/rundeck/pull/5217)
* [Fix #5125. Correct timezone offset on execution node status.](https://github.com/rundeck/rundeck/pull/5213)
* [Replaces job Status notification email using Pine Email Framework](https://github.com/rundeck/rundeck/pull/5208)
* [Tour plugin project configuration](https://github.com/rundeck/rundeck/pull/5192)
* [Tour plugins can't be configured at the project level](https://github.com/rundeck/rundeck/issues/5191)
* [Fixes #5147 Allow quotes in the middle of arguments to be interpreted as a single argument](https://github.com/rundeck/rundeck/pull/5182)
* [Update versions for plugins and libraries with reported vulnerabilities.](https://github.com/rundeck/rundeck/pull/5181)
* [New ACL for job writers #5078](https://github.com/rundeck/rundeck/pull/5176)
* [Attempt to fix #4286 by putting a sync lock around the service loader.](https://github.com/rundeck/rundeck/pull/5170)
* [Fix: Job mail notifications respect allow unsanitized property](https://github.com/rundeck/rundeck/pull/5169)
* [Dedupe setps on stepctx](https://github.com/rundeck/rundeck/pull/5158)
* [Execution steps not fully loading](https://github.com/rundeck/rundeck/issues/5157)
* [Fix proxy initialization exception for avg duration notifications](https://github.com/rundeck/rundeck/pull/5153)
* [Avg Duration Exceeded Notification Triggers Exception](https://github.com/rundeck/rundeck/issues/5149)
* [Resource model generator with quoted args](https://github.com/rundeck/rundeck/issues/5147)
* [Load full option object with lazy values on session object #5131 ](https://github.com/rundeck/rundeck/pull/5146)
* [fixing Scheduling using crontab displays warning](https://github.com/rundeck/rundeck/pull/5145)
* [Allow notification to use workflow exported variables #5061](https://github.com/rundeck/rundeck/pull/5139)
* [Scheduling using crontab displays warning](https://github.com/rundeck/rundeck/issues/5134)
* [Upon saving the job after editing option, encounter error "failed to lazily initialize a collection of role: rundeck.Option.values, could not initialize proxy - no Session"](https://github.com/rundeck/rundeck/issues/5131)
* [Upgrade to 3.1.0 on ubuntu overwrites realm.properties](https://github.com/rundeck/rundeck/issues/5126)
* [Incorrect time on job activity page](https://github.com/rundeck/rundeck/issues/5125)
* [Improves readability of options in job executions, answers #5044](https://github.com/rundeck/rundeck/pull/5105)
* [Docker - add ldap nestedGroups setting to remco template](https://github.com/rundeck/rundeck/pull/5100)
* [Bump mockito-all from 1.8.5 to 1.10.19](https://github.com/rundeck/rundeck/pull/4869)
* [Bump commons-httpclient from 3.0.1 to 3.1](https://github.com/rundeck/rundeck/pull/4867)
* [Bump asset-pipeline-grails from 2.14.7 to 3.0.10](https://github.com/rundeck/rundeck/pull/4866)
* [Bump commonmark from 0.10.0 to 0.11.0](https://github.com/rundeck/rundeck/pull/4863)
* [Bump sass-asset-pipeline from 3.0.5 to 3.0.10](https://github.com/rundeck/rundeck/pull/4862)
* [Bump jaxb-api from 2.3.0 to 2.3.1](https://github.com/rundeck/rundeck/pull/4859)
* [API to list installed plugins](https://github.com/rundeck/rundeck/issues/495)
* [Consider plugin title in search](https://github.com/rundeck/rundeck/pull/5288)

## Contributors

* Alberto Hormazabal
* Deyan Stoykov (deyanstoykov)
* GitHub (web-flow)
* Greg Schueler (gschueler)
* Jaime Tobar (jtobard)
* Jesse Marple (jessemarple)
* Luis Toledo (ltamaster)
* ProTip
* Rune Philosof (runephilosof)
* Stephen Joyner (sjrd218)
* carlos (carlosrfranco)
* dependabot-preview[bot]
* dependabot[bot] (dependabot-bot)

## Bug Reporters

* G3NSVRV
* ProTip
* ahonor
* ahormazabal
* amendlik
* bradym
* carlosrfranco
* dependabot-preview[bot]
* deyanstoykov
* gschueler
* jessemarple
* jplassnibatt
* jtobard
* ltamaster
* menathor
* paulholden
* praveenag
* runephilosof
* sjrd218
* wkk1020