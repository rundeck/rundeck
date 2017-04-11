% Version 2.8.0
% greg
% 04/10/2017

Release 2.8.0
===========

Date: 2017-04-10

Name: <span style="color: MidnightBlue"><span class="glyphicon glyphicon-camera"></span> "cafe cubano midnightblue camera"</span>

## Upgrading from Earlier versions

* See the [Upgrading Guide](../upgrading/index.html)

## Notes

Import Changes:

* Java 8 is required.

New Features:

* Jobs Options now have a [File input type](../manual/jobs.html#file-option-type).
	* Uploaded files are stored temporarily on the server while the execution runs
	* [API support](../api/index.html#upload-a-file-for-a-job-option)
* API Token Roles
	* API Tokens now have associated Roles and Expiration date.
	* [Access Control](../administration/access-control-policy.html#api-token-authorization-roles): 
		* Authorized users can generate a token with their own roles.
		* Authorized users can specify other access roles.
	* [Admins can specify maximum token lifetime](../administration/configuration-file-reference.html#security).
	* [API support](../api/index.html#authentication-tokens)
* Job editor autocomplete supports global variables
* Project configuration for displaying Readme/Motd on Projects List or Project Home page
* Project configuration for collapsing/expanding Job groups by default
* Project Archive Export: User can select what to include in the archive
	* [API support](../api/index.html#project-archive-export)
	* [API supports async export](../api/index.html#project-archive-export-async)
* New Step Plugin: Refresh nodes. Causes Rundeck to refresh the nodes list for the project, so that subsequent Job Reference steps can use any new nodes.
* File Copy Plugin: Recursive and wildcard file copy
* Job Options: Multivalue options can be selected by default
* Job Options: [Remote Option values can specify default selections](../manual/jobs.html#json-format)

Other changes:

* [Optional `Referer` Header verification for CSRF attack prevention](../administration/configuration-file-reference.html#security)
* Plugin properties support [`Options` multivalue type](../developer/plugin-development.html#plugin-properties) and [java annotations](http://rundeck.org/docs/developer/plugin-annotations.html#plugin-properties)

Bugfixes:

* Error calling Job Run API for a scheduled Job


## Contributors

* Dan Dunckel (dandunckelman)
* GitHub (web-flow)
* Greg Schueler (gschueler)
* Philippe Muller (pmuller)
* gitpmside (mathieuchateau)
* jtobard

## Bug Reporters

* bzlowrance
* dandunckelman
* djalai
* gschueler
* hlerebours
* jtobard
* mathieuchateau
* pkr1234
* pmuller
* rophy

## Issues

[Milestone 2.8.0](https://github.com/rundeck/rundeck/milestone/52)

* [Project config page: resource model source with CODE rendering field with empty value causes NPE](https://github.com/rundeck/rundeck/issues/2413)
* [Project specific ACLs stored on filesystem are not loaded](https://github.com/rundeck/rundeck/issues/2408)
* [Support global vars in Job editor autocomplete](https://github.com/rundeck/rundeck/issues/2407)
* [Option to hide readme/motd on projects list page](https://github.com/rundeck/rundeck/issues/2404)
* [IllegalArgumentException at job start on storage dirs creation](https://github.com/rundeck/rundeck/issues/2400)
* [Fix typo: s/losf/lsof/](https://github.com/rundeck/rundeck/pull/2398)
* [Revert default startpage setting when clicking on a project](https://github.com/rundeck/rundeck/issues/2395)
* [Enhancement: Flag to not include executions in project archive generation](https://github.com/rundeck/rundeck/issues/2394)
* [fix #921 add project.jobs.gui.groupExpandLevel configuration](https://github.com/rundeck/rundeck/pull/2392)
* [API: Calling the API to run a job when the schedule is enabled trigger an error message](https://github.com/rundeck/rundeck/issues/2389)
* [Expose refresh node cache to plugin](https://github.com/rundeck/rundeck/pull/2380)
* [Readme.md hidden on Main Menu Feature request.](https://github.com/rundeck/rundeck/issues/2377)
* [Packaging: Deb: uninstall "purge" script is not correct](https://github.com/rundeck/rundeck/issues/2370)
* [Add RDECK_CONFIG_FILE environment variable](https://github.com/rundeck/rundeck/pull/2368)
* [Require Java 8](https://github.com/rundeck/rundeck/issues/2365)
* [CopyFile Plugin enhancement](https://github.com/rundeck/rundeck/pull/2359)
* [Api token enhancement](https://github.com/rundeck/rundeck/pull/2358)
* [File input option type for Jobs](https://github.com/rundeck/rundeck/pull/2351)
* [Prevent csrf attacks](https://github.com/rundeck/rundeck/pull/2236)
* [Feature Request: web api should have same ACL as the api token user](https://github.com/rundeck/rundeck/issues/1550)
* [OpenJDK 64-Bit Server VM warning: ignoring option MaxPermSize=256m; support was removed in 8.0](https://github.com/rundeck/rundeck/issues/1367)
* ["Default" for multivalue option fetched from remote URL](https://github.com/rundeck/rundeck/issues/1189)
