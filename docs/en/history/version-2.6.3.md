% Version 2.6.3
% greg
% 02/25/2016

Release 2.6.3
===========

Date: 2016-02-25

Name: <span style="color: dodgerblue"><span class="glyphicon glyphicon-leaf"></span> "cafe bonbon dodgerblue leaf"</span>

## Notes

Primarily bug fixes, and some enhancements:

* Asynchronous nodes cache. Per-project and global toggle and configurable retention delay.
By default, projects now use an asynchronous method for	retrieving Node data from Resource Model Sources. 
You should see an improvement in page load when using slow model sources (such as a URL for a slow endpoint).
* Performance improvements with many thousands of Nodes.  The Nodes and Commands page, as well as other places
where Nodes are loaded (e.g. Job editor) now should be much more responsive when you have e.g. 20K nodes.
The Nodes page now uses result paging, and some parts of the UI now truncate the result set as well if you have many nodes.


## Contributors

* Alex Honor (ahonor)
* Bryon Williams
* Greg Schueler (gschueler)
* Luis Toledo
* Miguel A. Fuentes Buchholtz (miguelantonio)
* Rophy Tsai (rophy)
* mathieuchateau

## Bug Reporters

* Alicia-Solinea
* SydOps
* ahonor
* ajxb
* arminioa
* bryonwilliams
* gschueler
* kamaradclimber
* kmusard
* ltamaster
* makered
* mathieuchateau
* obrienmorgan
* rasebo
* richiereynolds
* robizz
* rophy
* schast
* snebel29
* ssbarnea

## Issues

* [toggle job execution/schedule can be triggered with GET without synchronizer token](https://github.com/rundeck/rundeck/issues/1709)
* [Add bulk job edit: enable/disable schedule and execution](https://github.com/rundeck/rundeck/issues/1703)
* [ExecutionJob: Failed to update job statistics](https://github.com/rundeck/rundeck/issues/1701)
* [some links are dead](https://github.com/rundeck/rundeck/issues/1688)
* [Workflow step editor: after deleting a script and hitting save, it disappears](https://github.com/rundeck/rundeck/issues/1686)
* [Code example in documentation incorrect](https://github.com/rundeck/rundeck/issues/1678)
* [Documentation Proposed Change log4j](https://github.com/rundeck/rundeck/pull/1677)
* [Missing step plugin causes execution to show no output](https://github.com/rundeck/rundeck/issues/1673)
* [Orchestrator plugin input cannot use job options](https://github.com/rundeck/rundeck/issues/1672)
* [missing API documentation for job execution and schedule enable/disable](https://github.com/rundeck/rundeck/issues/1670)
* [SCM plugins not shown in Plugins listing page](https://github.com/rundeck/rundeck/issues/1668)
* [Add documentation for changing date formats, and general localization](https://github.com/rundeck/rundeck/issues/1667)
* [Script-based plugin setting for merging parent environment variables](https://github.com/rundeck/rundeck/issues/1666)
* [Feature: Asynchronous nodes cache](https://github.com/rundeck/rundeck/pull/1662)
* [Nodes UI page updates issues](https://github.com/rundeck/rundeck/issues/1658)
* [Secure options with defaults in key storage](https://github.com/rundeck/rundeck/issues/1657)
* [Node page UI updated for large node sets](https://github.com/rundeck/rundeck/pull/1651)
* [rd-acl doesn't support project_acl resource type](https://github.com/rundeck/rundeck/issues/1650)
* [lack of quoting tags links in Nodes browse page](https://github.com/rundeck/rundeck/issues/1647)
* [SCM: GitExportPlugin - Could not serialize job: java.lang.IllegalArgumentException: Format not supported: yaml](https://github.com/rundeck/rundeck/issues/1644)
* [SCM: NoSuchFileException](https://github.com/rundeck/rundeck/issues/1642)
* [Passive mode - more explicit for already running job](https://github.com/rundeck/rundeck/pull/1635)
* [Enhance project archive API: select only a set of executions for output](https://github.com/rundeck/rundeck/pull/1621)
* [Added CODE renderingType to allow plugins to render textareas with th…](https://github.com/rundeck/rundeck/pull/1620)
* [execution query api for older than relative date](https://github.com/rundeck/rundeck/pull/1617)
* [fix #1525 project storage file read should not close stream](https://github.com/rundeck/rundeck/pull/1615)
* [fix #1611](https://github.com/rundeck/rundeck/pull/1614)
* [500 http response when execution does not exist](https://github.com/rundeck/rundeck/issues/1611)
* [Invalid username and password message displayed on a successful login which was preceeded by a failed login](https://github.com/rundeck/rundeck/issues/1610)
* [Querying 5k nodes leads to very long queries on the webui](https://github.com/rundeck/rundeck/issues/1597)
* [fix long line wrapping for log output #1558](https://github.com/rundeck/rundeck/pull/1596)
* [Multiple Authentication Modules documentation error](https://github.com/rundeck/rundeck/issues/1593)
* [The size (8192) given to the column 'json_data' exceeds the maximum allowed for any data type (8000)](https://github.com/rundeck/rundeck/issues/1579)
* [Document how to grant via ACL for Per-Job schedule and execution toggling](https://github.com/rundeck/rundeck/issues/1578)
* [Console output is not wrapped making impossible to see long lines](https://github.com/rundeck/rundeck/issues/1558)
* [Export project is not working when using the GUI - 2.6.0-1](https://github.com/rundeck/rundeck/issues/1525)
* [Cancel/Save options on workflow step become unresponsive after error](https://github.com/rundeck/rundeck/issues/1448)
* [Prevents remote option spam in issue 1391 by using onChange event](https://github.com/rundeck/rundeck/pull/1415)
* [Log Output gone mad in 2.5.2](https://github.com/rundeck/rundeck/issues/1368)
* [Simultaneously triggered jobs sometimes hang](https://github.com/rundeck/rundeck/issues/1310)
* [Job timeouts before actual timeout value](https://github.com/rundeck/rundeck/issues/1302)
