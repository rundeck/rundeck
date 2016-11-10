% Version 2.6.10
% greg
% 11/10/2016

Release 2.6.10
===========

Date: 2016-11-10

Name: <span style="color: LimeGreen"><span class="glyphicon glyphicon-glass"></span> "cafe bonbon limegreen glass"</span>

## Notes

Notable Enhancements:

* Run Job Later feature: schedule a one-time Job execution
* Git SCM Plugins: ability to import jobs from a repo and "archive" the original UUID. Allows sharing a repo among multiple "non-production" projects within the same Rundeck server, and still maintain static UUIDs for the production project.
* Plugin localization and custom icons
* New Project home page
* Project ACLs shown in Access Control page in configuration section
* Custom navbar color configuration

Bug Fixes:

* Potential XSS fixed in Activity page
* Pagination fixed on Activity page
* Git plugin: importing from a git repo, and later changing to a different repo would cause an error

Other Changes:

* Job options are now always shown in the defined order
* Globals can be used in remote option value URLs

And more...

## Contributors

* Andres Montalban (amontalban)
* Fabrice Bacchella
* GitHub (web-flow)
* Greg Schueler (gschueler)
* Jamie L. Penman-Smithson (jamieps)
* Jeff Runningen
* Stian Mathiassen
* Sumesh P (sumeshpremraj)

## Bug Reporters

* Mapel88
* ahonor
* ajxb
* amontalban
* cstewart87
* dabest1
* egeland
* fbacchella
* gschueler
* jamieps
* jrunningen
* katanafleet
* kureus
* ltamaster
* penekk
* pfweberp
* puremourning
* rophy
* s-tokutake
* scaissie
* sebw
* smat
* ssbarnea
* sumeshpremraj
* willemdh

## Issues

* [API errors are always returned in XML format, not in JSON format](https://github.com/rundeck/rundeck/issues/2151)
* [RSS feed not accessable by public anymore](https://github.com/rundeck/rundeck/issues/2150)
* [Project config file editor: node executor/file copier validation failures show no message](https://github.com/rundeck/rundeck/issues/2144)
* [Adhoc job run display error](https://github.com/rundeck/rundeck/issues/2136)
* [java.lang.NullPointerException: Cannot get property 'workflow' on null object](https://github.com/rundeck/rundeck/issues/2134)
* [Options with one value and 'enforced allowed values' are not loaded anymore](https://github.com/rundeck/rundeck/issues/2131)
* [Custom color on top navbar](https://github.com/rundeck/rundeck/pull/2130)
* [Global options (from project.properties) not available in valuesUrl](https://github.com/rundeck/rundeck/issues/2125)
* [XSS vulnerability in Activity page](https://github.com/rundeck/rundeck/issues/2123)
* [API: Job run allows separate option params](https://github.com/rundeck/rundeck/pull/2121)
* [Plugin i18n](https://github.com/rundeck/rundeck/pull/2111)
* [Problems viewing the oldest executions on the Activity page ](https://github.com/rundeck/rundeck/issues/2097)
* [add project home landing page](https://github.com/rundeck/rundeck/pull/2093)
* [Add documentation to configure Rundeck with PostgreSQL DB](https://github.com/rundeck/rundeck/pull/2090)
* [the /etc/rundeck/profile in RPM packaging has syntax error](https://github.com/rundeck/rundeck/issues/2089)
* [List of Project ACLs added to security page.  rename to Access Control](https://github.com/rundeck/rundeck/pull/2079)
* [Escape HTML characters in ExecutionController.renderOutput](https://github.com/rundeck/rundeck/pull/2068)
* ["Send Notification?" is hardcoded.](https://github.com/rundeck/rundeck/issues/2042)
* [attributes with dot in prefix do not render in gui](https://github.com/rundeck/rundeck/issues/2040)
* [using Job References, ${DATE} only gets expanded as option value in the top job.](https://github.com/rundeck/rundeck/issues/2039)
* [Feature Request: Enable linking to job with runbook tab opened](https://github.com/rundeck/rundeck/issues/2036)
* [More redhatcompliance init file, more easier to use profile file](https://github.com/rundeck/rundeck/pull/2030)
* [Feature/api job info](https://github.com/rundeck/rundeck/pull/2025)
* [Remote URL job options do not update when the refresh button is selected](https://github.com/rundeck/rundeck/issues/2024)
* [Inline scripts token expansion broken in 2.6.9. How to escape the @ character?](https://github.com/rundeck/rundeck/issues/2021)
* [Firefox: Secure job option with a default value from keystore is overwritten on job run](https://github.com/rundeck/rundeck/issues/2015)
* [2.6.9-1: Drag workflow steps does not work unless you edit a step first](https://github.com/rundeck/rundeck/issues/2004)
* [Default values not showing for multi-valued job options in 2.6.9-1](https://github.com/rundeck/rundeck/issues/2002)
* [\[Options Providers\] 2.6.9 - display of sorted options does not respect "preserveOrder" or definition settings](https://github.com/rundeck/rundeck/issues/1998)
* [Fixes #1994](https://github.com/rundeck/rundeck/pull/1995)
* [replace "dtolabs.com" uri in acl internals](https://github.com/rundeck/rundeck/pull/1992)
* [Support for ad hoc scheduling of jobs](https://github.com/rundeck/rundeck/pull/1987)
* [colors disappeared in inline script](https://github.com/rundeck/rundeck/issues/1983)
* [SCM Import/Export with high job count](https://github.com/rundeck/rundeck/issues/1982)
* [SCM Import Fails with Cannot get property '\[UUID\]' on null object](https://github.com/rundeck/rundeck/issues/1970)
* [SCM import should have a "Remove UUIDs" option](https://github.com/rundeck/rundeck/issues/1503)
* [postgresql support is not documented](https://github.com/rundeck/rundeck/issues/1405)
* [Launcher jar on windows: stack trace on startup](https://github.com/rundeck/rundeck/issues/1326)
