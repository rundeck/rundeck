Release 3.0.24
===========

Date: 2019-07-19

Name: <span style="color: rebeccapurple"><span class="glyphicon glyphicon-tent"></span> "jalapeño popper rebeccapurple tent"</span>

## Notes

### Enhancements

* User profile API tokens pagination

### Bug Fixes

* Fixed node enhancers not working when using filesystem project config [#5066](https://github.com/rundeck/rundeck/pull/5066)
* Fix potential NPE in node enhancer [#5068](https://github.com/rundeck/rundeck/pull/5068)
* Numeruous CVEs addressed by library version bumps in [#5048](https://github.com/rundeck/rundeck/pull/5048)
* (Enterprise) Fixed failure in **File Transfer Plugin** when using backup source with non-default port

## Issues

[Milestone 3.0.24](https://github.com/rundeck/rundeck/milestone/111)

* [\[3.0.x backport\] fix potential npe caused by race condition in node enhancer](https://github.com/rundeck/rundeck/pull/5068)
* [Backport PR #5005 - EnhancedNodeService bean was not loaded](https://github.com/rundeck/rundeck/pull/5066)
* [Backport PR #4990 - Adds pagination of tokens on user profile](https://github.com/rundeck/rundeck/pull/5065)
* [Backport PR #4990 - Adds pagination of tokens on user profile](https://github.com/rundeck/rundeck/pull/5062)
* [backport of PR #5047 - Update library dependencies to address CVEs](https://github.com/rundeck/rundeck/pull/5048)
* [\[3.0.x\] Job execution might fail, with NPE stacktrace in service log (node enhancer feature)](https://github.com/rundeck/rundeck/issues/5018)
* [rundeck "CVE-2019-11272" Spring Security Update Plz~](https://github.com/rundeck/rundeck/issues/5002)

## Contributors

* Alberto Hormazabal
* Greg Schueler (gschueler)
* Greg Zapp (ProTip)
* carlos (carlosrfranco)

## Bug Reporters

* ahormazabal
* carlosrfranco
* gschueler
* happylie