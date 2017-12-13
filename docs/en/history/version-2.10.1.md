% Version 2.10.1
% greg
% 11/20/2017

Release 2.10.1
===========

Date: 2017-11-20

Name: <span style="color: Tomato"><span class="glyphicon glyphicon-knight"></span> "cafe mocha tomato knight"</span>

## Upgrading from Earlier versions

* See the [Upgrading Guide](../upgrading/index.html)

## Notes

Primarily Bug fixes.

Enhancements:

* New: Global Variable step plugin: copy captured data values into a global context in any variable group.  E.g. data from a node step to use it in a non-node-step.
	* Within a job, copy data to the `export.*` group, to have it available in later steps if the job is included as a Job Reference.

## Contributors

* Greg Schueler (gschueler)
* Luis Toledo (ltamaster)
* Michihito Shigemura (shigemk2)
* jtobard
* morihaya

## Bug Reporters

* JustRiedy
* Morihaya
* gschueler
* jtobard
* leonboot
* ltamaster
* pchevallereau
* shigemk2
* vinillum

## Issues

[Milestone 2.10.1](https://github.com/rundeck/rundeck/milestone/64)

* [Improvement on load jobs page](https://github.com/rundeck/rundeck/pull/2953)
* [Uplift variables to global (2)](https://github.com/rundeck/rundeck/pull/2952)
* [Update dependencies: jackson-databind, commons-beanutils](https://github.com/rundeck/rundeck/pull/2949)
* [Change the JSCH authentication error message](https://github.com/rundeck/rundeck/pull/2934)
* [Dependency cleanup](https://github.com/rundeck/rundeck/pull/2933)
* [Bug: cluster mode is not enabled](https://github.com/rundeck/rundeck/issues/2932)
* [Instance scope step plugin properties not shown in Plugin listing page](https://github.com/rundeck/rundeck/issues/2924)
* [Key browser dialog is broken for new projects](https://github.com/rundeck/rundeck/issues/2919)
* [java.lang.IllegalStateException: stack is empty](https://github.com/rundeck/rundeck/issues/2914)
* [Add ui plugin support for ACL editor pages](https://github.com/rundeck/rundeck/pull/2906)
* [2.10: Edit System ACL File: cannot save after submitting invalid file](https://github.com/rundeck/rundeck/issues/2904)
* [fix typo. uploaded ot =\> uploaded to](https://github.com/rundeck/rundeck/pull/2897)
* [IllegalStateException: output was closed and NullPointerException](https://github.com/rundeck/rundeck/issues/2887)
* [RXSS vulnerability](https://github.com/rundeck/rundeck/issues/2883)
* [Fix: model source plugin failure should log project name](https://github.com/rundeck/rundeck/pull/2869)
* [API request for invalid path returns HTML response](https://github.com/rundeck/rundeck/issues/2867)
* [Include aws resource model source plugin](https://github.com/rundeck/rundeck/pull/2857)
* [Fix link in administration/managing-node-sources](https://github.com/rundeck/rundeck/pull/2856)
* [single valued options with "selected=true" fetched from remote URL no longer works](https://github.com/rundeck/rundeck/issues/2854)
* [Activity Log - Strange column widths](https://github.com/rundeck/rundeck/issues/2823)
* [Cannot get AuthContext without subject (Invalid session?)](https://github.com/rundeck/rundeck/issues/2710)
* [Download job definition yaml format shows inline](https://github.com/rundeck/rundeck/issues/824)
