% Version 2.6.9
% greg
% 08/03/2016

Release 2.6.9
===========

Date: 2016-08-03

Name: <span style="color: indigo"><span class="glyphicon glyphicon-tower"></span> "cafe bonbon indigo tower"</span>

## Notes

Notable Enhancements:

* Job execution will show more info about the step context in the Monitor/Summary tab (see [#1926](https://github.com/rundeck/rundeck/pull/1926))
* User Profile page: can select Spanish language manually
* Job Option editor GUI: order options via drag and drop
* Extended job description can be rendered as a Runbook tab (see [#1904](https://github.com/rundeck/rundeck/pull/1904))
* Some support for Oauth/preauth roles via a proxy (see [#1883](https://github.com/rundeck/rundeck/pull/1883))

Bug Fixes:

* dynamic refresh for cascading remote option values
* A number of SCM/git plugin fixes
* Plugin jar loading issues

## Contributors

* Alex Honor (ahonor)
* Dave Brothers (eidolonic)
* GitHub (web-flow)
* Greg Schueler (gschueler)
* Jaime Tobar (jtobard)
* John Stoltenborg (tlots)
* Miguel A. Fuentes Buchholtz (miguelantonio)
* Parth Soni (parth-kloudscript)
* Seth Klein (kindlyseth)
* variacode95

## Bug Reporters

* ARentz07
* ajrnz
* andysteady
* eblikstad
* eidolonic
* gschueler
* jtobard
* katanafleet
* kgeis
* kincl
* ltamaster
* miguelantonio
* niphlod
* papagr
* pdev77b
* philippevidal80
* richiereynolds
* rophy
* schast
* tlots

## Issues

* [Bug fix: multiple DATE format option values do not work](https://github.com/rundeck/rundeck/pull/1973)
* [Git Plugin: default import selection to use regex](https://github.com/rundeck/rundeck/issues/1972)
* [Update Quartz scheduler reference page](https://github.com/rundeck/rundeck/pull/1969)
* [Secure job option with a default value from keystore is overwritten on job run](https://github.com/rundeck/rundeck/issues/1966)
* [Improve error message: "Option \<name\> is required, but storage value could not be read."](https://github.com/rundeck/rundeck/issues/1954)
* [IE 11 issue: Node summary on execution page fails to load](https://github.com/rundeck/rundeck/issues/1953)
* [Script-based Remote script plugin: incorrect arguments passed](https://github.com/rundeck/rundeck/issues/1947)
* [Globals Variables to sending Email notification  (list of emails)](https://github.com/rundeck/rundeck/issues/1942)
* [Weird behaviour during enable/disable jobs scheduling.](https://github.com/rundeck/rundeck/issues/1941)
* [add a project/global configuration to specify default encoding](https://github.com/rundeck/rundeck/issues/1938)
* [SCM Export: create/update job causes error: Failed to serialize job, no content was written](https://github.com/rundeck/rundeck/issues/1931)
* [Enhance execution step summary display](https://github.com/rundeck/rundeck/pull/1926)
* [new options form: JS error when job has no options](https://github.com/rundeck/rundeck/issues/1914)
* [Run Job: wrong number of selected nodes shown](https://github.com/rundeck/rundeck/issues/1913)
* [GUI support for reordering options](https://github.com/rundeck/rundeck/pull/1907)
* [Extended job description rendered as a Runbook](https://github.com/rundeck/rundeck/pull/1904)
* [Home page: show stats for failed execution counts](https://github.com/rundeck/rundeck/issues/1903)
* [Compatiblity Mode in Internet Explorer](https://github.com/rundeck/rundeck/issues/1901)
* [2.6.8 on windows fails on pluginJars dirs](https://github.com/rundeck/rundeck/issues/1898)
* [GUI option dynamic refresh broken in 2.6.6 and 2.6.8](https://github.com/rundeck/rundeck/issues/1895)
* [Error building plugin: Class XXPlugin was not a valid plugin class for service: WorkflowNodeStep](https://github.com/rundeck/rundeck/issues/1894)
* [Running a job as a different user (asUser parameter)](https://github.com/rundeck/rundeck/issues/1893)
* [SCM export: invalid tag and renamed job causes "Entry not found by path"](https://github.com/rundeck/rundeck/issues/1885)
* [Preauthentication filter](https://github.com/rundeck/rundeck/pull/1883)
* [Context Variable: allow node options containing slash](https://github.com/rundeck/rundeck/issues/1823)
* [Remote git repositories are checked on every jobs-page load](https://github.com/rundeck/rundeck/issues/1743)
* [Rundeck local execution is case sensitive on osFamily = "windows"](https://github.com/rundeck/rundeck/issues/1727)
* [SCM commit JOB in Windows](https://github.com/rundeck/rundeck/issues/1708)
* [Rundeck i18n - ability to select language in GUI](https://github.com/rundeck/rundeck/issues/1699)
* [Job set as "execute locally" should not require privilege to rundeck server node](https://github.com/rundeck/rundeck/issues/1459)
* [Job reference step does not log step descriptions](https://github.com/rundeck/rundeck/issues/1370)
* [Improve workflow step up/down graphics for usability](https://github.com/rundeck/rundeck/issues/1360)
* [As a rundeck user, i'd like to have the possibility to sort rundeck job executions option on user interface.](https://github.com/rundeck/rundeck/issues/1002)
* [Very difficult to trace output to exact job step in rundeck v2.0](https://github.com/rundeck/rundeck/issues/667)
* [NTH: improve job option ordering to allow GUI drag and drop](https://github.com/rundeck/rundeck/issues/363)
