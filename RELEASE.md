Release 2.10.0
===========

Date: 2017-10-16

Name: <span style="color: Teal"><span class="glyphicon glyphicon-glass"></span> "cafe mocha teal glass"</span>

## Upgrading from Earlier versions

* See the [Upgrading Guide](http://rundeck.org/docs/upgrading/index.html)

## Notes

New features:

* GUI editable Resource Model source plugins.  Allows the xml/yaml/json resource data to be edited in the GUI.
	* The built-in File model source can be marked as "writeable", allowing admins to edit the resources file in the GUI
* GUI editable ACLs. Modify the aclpolicy yaml files in the GUI and see validation info.
* Jobs can define Notifications in the event of Average Duration Exceeded
* API updated to allow user profile modifications
* Job Filters can specify whether a job is scheduled
* Key Value Data capture Log Filter can capture a value and specify a hardcoded name for the variable

Plus: bug fixes, documentation typo fixes

## Contributors

* Greg Schueler (gschueler)
* Michihito Shigemura (shigemk2)
* damageboy
* jtobard

## Bug Reporters

* gschueler
* jtobard
* ltamaster
* ronave
* shigemk2

## Issues

[Milestone 2.10.0](https://github.com/rundeck/rundeck/milestone/62)

* [Fix typo in tutorials/project-setup](https://github.com/rundeck/rundeck/pull/2847)
* [Execution log storage partial checkpointing](https://github.com/rundeck/rundeck/pull/2790)
* [Key Storage Selector doesn't work on Config Page](https://github.com/rundeck/rundeck/issues/2785)
* [List Future Schedule on jobsAjax](https://github.com/rundeck/rundeck/pull/2778)
* [ACL File editor](https://github.com/rundeck/rundeck/pull/2772)
* [Feature: GUI editable File resource model sources](https://github.com/rundeck/rundeck/pull/2753)
* [Api endpoint to modify user profile.](https://github.com/rundeck/rundeck/pull/2741)
* [Jobs filter enhancement](https://github.com/rundeck/rundeck/pull/2733)
* [Named pattern log filter](https://github.com/rundeck/rundeck/pull/2720)
* [Job Notification for jobs exceeding average duration](https://github.com/rundeck/rundeck/pull/2665)
* [API acl system level bug](https://github.com/rundeck/rundeck/issues/2569)
