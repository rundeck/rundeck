% Version 2.7.0
% greg
% 11/30/2016

Release 2.7.0
===========

Date: 2016-11-30

Name: <span style="color: SlateBlue"><span class="glyphicon glyphicon-headphones"></span> "cafecito slateblue headphones"</span>

## Upgrading from 2.6

* See the [Upgrading Guide](../upgrading/index.html)

## New Features

* Date input type for Job Options
* Job reference node interesect
* Old CLI tools removed. You can separately install [rd](https://rundeck.github.io/rundeck-cli/) to replace them.
* Jetty upgraded
* Filter activity by Node
* Override node filter at job execution time

## Contributors

* Andres Montalban (amontalban)
* David Schile
* Fabrice Bacchella
* GitHub (web-flow)
* Greg Schueler (gschueler)
* Jaime Tobar (jtobard)
* Jamie L. Penman-Smithson (jamieps)
* Pavol Gressa (pgressa)
* Rene Fragoso (ctrlrsf)
* ltamaster

## Bug Reporters

* adrianshum
* ahonor
* bajacondor
* ctrlrsf
* grafjo
* gschueler
* jasonhensler
* jbguerraz
* jquick
* jtobard
* ltamaster
* mathieuchateau
* pgressa
* williamh

## Issues

[Milestone 2.7.0](https://github.com/rundeck/rundeck/milestone/38)

* [Add executionType](https://github.com/rundeck/rundeck/pull/2210)
* [allow `?opt.name=value&opt.name2=value2` url params to set option values](https://github.com/rundeck/rundeck/pull/2209)
* [New documentation about how to install the launcher on windows](https://github.com/rundeck/rundeck/pull/2205)
* [Remove CLI tools for #2182](https://github.com/rundeck/rundeck/pull/2199)
* [2.6.10: Inline script token expansion breaking perl](https://github.com/rundeck/rundeck/issues/2185)
* [Remove old CLI tools](https://github.com/rundeck/rundeck/issues/2182)
* [API: POST to key storage can create root file](https://github.com/rundeck/rundeck/issues/2181)
* [Feature/jetty upgrade](https://github.com/rundeck/rundeck/pull/2180)
* [Node filter override breaks jobs with "execute locally"](https://github.com/rundeck/rundeck/issues/2179)
* [Takeover schedule API is timing out in rundeck 2.6.10](https://github.com/rundeck/rundeck/issues/2176)
* [Align deb and rpm profile files](https://github.com/rundeck/rundeck/issues/2170)
* [Upgrade jetty to latest stable, 7.6.x or newer](https://github.com/rundeck/rundeck/issues/2133)
* [firefox GUI fix](https://github.com/rundeck/rundeck/pull/2122)
* [Why is parallelWorkflowStrategy in incubator for more than 2 years?](https://github.com/rundeck/rundeck/issues/2115)
* [Feature - override node filter (GUI enhancement)](https://github.com/rundeck/rundeck/pull/2110)
* [Add support for job reference node intersect](https://github.com/rundeck/rundeck/pull/2104)
* [rundeck CLI scripts naming](https://github.com/rundeck/rundeck/issues/2081)
* [Issue with disable schedule job on cluster](https://github.com/rundeck/rundeck/pull/2073)
* [Feature/node filter enhancements](https://github.com/rundeck/rundeck/pull/2056)
* [Replace old rundeck cli tools with modern version](https://github.com/rundeck/rundeck/issues/1899)
* ["run" and "dispatch" launchers are named too generically](https://github.com/rundeck/rundeck/issues/1896)
* [Add job.executionType context variable](https://github.com/rundeck/rundeck/pull/1835)
* [Disable option substitution into scripts](https://github.com/rundeck/rundeck/issues/1762)
* [Allow Node Filter to be provided on execution time](https://github.com/rundeck/rundeck/issues/1754)
* [This adds dattepicker functionality.](https://github.com/rundeck/rundeck/pull/1634)
* [2.5.1.1 - removing a step remove current one](https://github.com/rundeck/rundeck/issues/1315)
* [lots of non-multiple execution jobs can block starting jobs](https://github.com/rundeck/rundeck/issues/1305)
* [Feature Request: Filter activity by node](https://github.com/rundeck/rundeck/issues/1290)
* [Upgrade h2 lib to 1.4.187](https://github.com/rundeck/rundeck/pull/1204)
