% Version 2.9.0
% greg
% 08/03/2017

Release 2.9.0
===========

Date: 2017-08-03

Name: <span style="color: FireBrick"><span class="glyphicon glyphicon-grain"></span> "caffe latte firebrick grain"</span>

## Upgrading from Earlier versions

* See the [Upgrading Guide](../upgrading/index.html)

## Notes

This release contains a number of enhancements:

For more detail see [2.9.0 Changes](https://github.com/rundeck/rundeck/issues/2577)

1. Data Capture/Data Passing between steps 
	* Data capture plugin to match a regular expression in a step's log output and pass the values to later steps
2. Log Filter Plugins
	* These plugins can be applied to individual steps, or to the entire workflow
	* Data type plugins can detect or mark formatted data such as JSON, CSV, HTML, Markdown, etc. and render it in the GUI
	* Mask Passwords plugin removes any values from secure input options before it is logged
	* Highlighting/quelling: highlight keywords or selectively quell output from verbose scripts
	* (And of course, you can write your own plugins...)
2. New Job and Project features: 
	* Export a project to another Rundeck instance
	* Copy a Job to another Project
	* Reference a Job in another Project
	* Check state of a job in another project
	* Disable Job Schedules or all Executions for a project
	* Improved Time Zone support
	* Allow matching 0 nodes as success condition
	* Retry delay
4. GUI changes
	* Rearranged the Project and System configuration pages and navigation
	* Job workflow editor enhancements

Stay tuned! We are also adding many new features to [Rundeck Pro](http://rundeck.com).

## Contributors

* Greg Schueler (gschueler)
* Luis Toledo (ltamaster)
* Marcel Dorenkamp (mdorenkamp)
* jtobard

## Bug Reporters

* damageboy
* daveres
* gschueler
* hiribarne
* jtobard
* ltamaster
* mdorenkamp
* mrala
* pgressa
* roller

## Issues

[Milestone 2.9.0](https://github.com/rundeck/rundeck/milestone/56)

* [Project ACLS not applied to scheduled jobs, SCM context](https://github.com/rundeck/rundeck/issues/2660)
* [Error viewing execution after deleting job using orchestrator](https://github.com/rundeck/rundeck/issues/2657)
* [support chinese ](https://github.com/rundeck/rundeck/pull/2642)
* [Export project to another instance](https://github.com/rundeck/rundeck/pull/2641)
* [Activity Node Filter doesn't work as expected](https://github.com/rundeck/rundeck/issues/2640)
* [UI updates](https://github.com/rundeck/rundeck/pull/2626)
* [Search executions and retry jobs for plugins](https://github.com/rundeck/rundeck/pull/2617)
* [Enable responsive css](https://github.com/rundeck/rundeck/pull/2613)
* [Configuration GUI overhaul](https://github.com/rundeck/rundeck/pull/2611)
* [Add GUI toggle for not wrapping long lines in the log output](https://github.com/rundeck/rundeck/issues/2608)
* [Delay between retries](https://github.com/rundeck/rundeck/pull/2576)
* [Add autocomplete for job name in Job Reference editor](https://github.com/rundeck/rundeck/pull/2567)
* [Include Batix/rundeck-ansible-plugin](https://github.com/rundeck/rundeck/pull/2556)
* [Copy job to another project](https://github.com/rundeck/rundeck/pull/2546)
* [Passive mode for project](https://github.com/rundeck/rundeck/pull/2534)
* [External Job State Conditional plugin.](https://github.com/rundeck/rundeck/pull/2524)
* [Job Reference on another Project](https://github.com/rundeck/rundeck/pull/2519)
* [Time Zone support](https://github.com/rundeck/rundeck/pull/2504)
* [Shared data context and data passing between steps](https://github.com/rundeck/rundeck/pull/2482)
* [Random subset orchestrator doesn't work as expected](https://github.com/rundeck/rundeck/issues/2472)
* [Flag for success when node filter does not match any node](https://github.com/rundeck/rundeck/pull/2456)
* [Feature Request - Support JOB Markdown output](https://github.com/rundeck/rundeck/issues/2325)
* [Navbar invisible on mobile devices](https://github.com/rundeck/rundeck/issues/2278)
* [Delay between retries](https://github.com/rundeck/rundeck/issues/2083)
* [Masked Passwords are exposed if a job is run in debug mode](https://github.com/rundeck/rundeck/issues/1780)
* [added web.xml to rpmbuild spec so it doesnt get replaced on update](https://github.com/rundeck/rundeck/pull/1591)
* [RPM Update overwrites web.xml](https://github.com/rundeck/rundeck/issues/1590)
* [grep ansi color log output doesn't work](https://github.com/rundeck/rundeck/issues/1463)
* [Support custom Timezone in all views](https://github.com/rundeck/rundeck/issues/906)
* [Scheduled job Time zone support](https://github.com/rundeck/rundeck/issues/138)
* [Possibility to pass data between a job's steps](https://github.com/rundeck/rundeck/issues/116)
