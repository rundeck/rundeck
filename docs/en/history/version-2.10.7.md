% Version 2.10.7
% greg
% 03/05/2018

Release 2.10.7
===========

Date: 2018-03-05

Name: <span style="color: Crimson"><span class="glyphicon glyphicon-sunglasses"></span> "cafe mocha crimson sunglasses"</span>

## Upgrading from Earlier versions

* See the [Upgrading Guide](../upgrading/index.html)

## Notes

Some bugfixes, and some improvements to Git SCM plugin:

* Export: Option to Synchronize automatically during fetch (default: false)
* Import: Option to Pull automatically when fetching (default: true)
* new Clean button in the GUI: allows removing local git repo 
* Import: automatic Tracking init during Setup, if you use a regular expression
	* API improvement: does not require a second step after setup to initialize Git Import
* Export: Push Remotely is checked by default when making commits
* (see full details [#PR3152](https://github.com/rundeck/rundeck/pull/3152))

## Contributors

* Greg Schueler (gschueler)
* Jaime Tobar (jtobard)
* Rene Fragoso (ctrlrsf)

## Bug Reporters

* SpencerMalone
* ctrlrsf
* gentunian
* jtobard

## Issues

[Milestone 2.10.7](https://github.com/rundeck/rundeck/milestone/71)

* [SCM usability enhancement](https://github.com/rundeck/rundeck/pull/3152)
* [PUT Project Configuration API endpoint fails w/ text/plain input](https://github.com/rundeck/rundeck/issues/3127)
* [Documentation: add job.executionType to context variables documentation](https://github.com/rundeck/rundeck/issues/1811)
* [Profile firstName does not accepts accents](https://github.com/rundeck/rundeck/issues/1581)
