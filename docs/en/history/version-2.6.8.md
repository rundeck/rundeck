% Version 2.6.8
% greg
% 06/10/2016

Release 2.6.8
===========

Date: 2016-06-10

Name: <span style="color: hotpink"><span class="glyphicon glyphicon-pushpin"></span> "cafe bonbon hotpink pushpin"</span>

## Notes

Fixed bugs and an enhancement:

* Framework and project global variables. Use `framework.globals.X=Y` or `project.globals.X=Y` in you configuration to expose `${globals.X}` to jobs/executions.

## Contributors

* Alberto Hormazabal (ahormazabal)
* Diomidis Spinellis (dspinellis)
* Greg Schueler (gschueler)
* shigemk2

## Bug Reporters

* ahonor
* ddzed15
* dspinellis
* gschueler
* katanafleet
* makered
* remixtj
* richiereynolds
* royjenkins
* shigemk2

## Issues

* [Add framework and project global variables](https://github.com/rundeck/rundeck/pull/1890)
* [Script plugins for Workflow/Node steps: allow script file extension to be specified](https://github.com/rundeck/rundeck/issues/1869)
* [Script plugins for Workflow/Node steps: allow key storage automatic read](https://github.com/rundeck/rundeck/issues/1868)
* [API: /api/17/project/name/jobs/import does not return JSON when ?format=xml is used](https://github.com/rundeck/rundeck/issues/1860)
* [API: 404 error for /api/17/project/name/run/url](https://github.com/rundeck/rundeck/issues/1859)
* [API: project create json with null description results in "null"](https://github.com/rundeck/rundeck/issues/1856)
* ["Run Again" job reverses multi-value arguments](https://github.com/rundeck/rundeck/issues/1851)
* [Error while reading project acls via API](https://github.com/rundeck/rundeck/issues/1850)
* [Plugin displayType: CODE supports syntax](https://github.com/rundeck/rundeck/issues/1845)
* [Cannot delete executions since upgrade to 2.6.6](https://github.com/rundeck/rundeck/issues/1844)
* [Archive out of date Japanese documentation](https://github.com/rundeck/rundeck/issues/1838)
* [Fix deadlink quartz](https://github.com/rundeck/rundeck/pull/1836)
* [option cascading not working at 100% (after upgrading 2.4.2 to 2.6.6)](https://github.com/rundeck/rundeck/issues/1832)
* [Simple Configuration edit page: should prompt when you navigate away before saving](https://github.com/rundeck/rundeck/issues/1731)
* [Log storage fails to retrieve remote file if unable to rename temp file](https://github.com/rundeck/rundeck/issues/1702)
* [Ensure creation of /tmp/rundeck directory](https://github.com/rundeck/rundeck/pull/1664)
* [Secure Option key Storage Path UI should disable some of the non relevant checkbox/fields](https://github.com/rundeck/rundeck/issues/1605)
* [When using "Run Again" options revert to their default value.](https://github.com/rundeck/rundeck/issues/1123)
