# Release 3.1.2

Date: 2019-09-27

Name: <span style="color: plum"><span class="glyphicon glyphicon-pushpin"></span> "mozzarella stick plum pushpin"</span>

## Upgrading
See the upgrade documentation [here](https://docs.rundeck.com/3.1.0-rc2/upgrading/upgrade-to-rundeck-3.1.html).

## Enhancements

### Allow unicode characeters in project description

## Bug Fixes

* Fixed SCM plugin API memory leak [#5301](https://github.com/rundeck/rundeck/issues/5301)
* Fixed Rundeck Plugin Options type preventing saving node step [#5302](https://github.com/rundeck/rundeck/pull/5302)
* `Enterprise` Fix s3 execution log plugin not loading

## Issues

[Milestone 3.1.2](https://github.com/rundeck/rundeck/milestone/121)

* [Fix #5301 fix initProject, and clean old listeners](https://github.com/rundeck/rundeck/pull/5304)
* [Fix issue with plugin select value with multiOption](https://github.com/rundeck/rundeck/pull/5302)
* [memory leak: git scm plugin accessed via API](https://github.com/rundeck/rundeck/issues/5301)
* [Fix #5083 - Fixing the regex expression to consider "." character when perform auto completions](https://github.com/rundeck/rundeck/pull/5300)
* [Fix #5113. Allow unicode characters in project description.](https://github.com/rundeck/rundeck/pull/5285)
* [The project.description from filesystem file etc/project.properties to db not support Unicode description](https://github.com/rundeck/rundeck/issues/5113)
* [Rundeck Plugin Options type prevents saving of node step](https://github.com/rundeck/rundeck/issues/4109)

## Contributors

* GitHub (web-flow)
* Greg Schueler (gschueler)
* Greg Zapp (ProTip)
* Stephen Joyner (sjrd218)
* carlosrfranco

## Bug Reporters

* carlosrfranco
* chnliyong
* cjmcken
* gschueler
* sjrd218