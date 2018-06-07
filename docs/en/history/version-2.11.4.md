% Version 2.11.4
% greg
% 06/07/2018

Release 2.11.4
===========

Date: 2018-06-07

Name: <span style="color: SaddleBrown"><span class="glyphicon glyphicon-pushpin"></span> "cappuccino saddlebrown pushpin"</span>

## Upgrading from Earlier versions

* See the [Upgrading Guide](../upgrading/index.html)

## Notes

Bug and security fixes.

This release addresses a potential security issue in processing of zip files,
please see the this issue: [Security Advisory: Zip Slip directory traversal vulnerability #3471](https://github.com/rundeck/rundeck/issues/3471).

## Contributors

* Greg Schueler (gschueler)
* Jaime Tobar (jtobard)
* Stephen Joyner (sjrd218)

## Bug Reporters

* gschueler
* jtobard
* kino71

## Issues

[Milestone 2.11.4](https://github.com/rundeck/rundeck/milestone/78)

* [2.11: backport bug fixes](https://github.com/rundeck/rundeck/issues/3494)
    * [#3384 duplicate jobs page doesn't show options]((https://github.com/rundeck/rundeck/issues/3384)
    * [#3423 Create project via API with invalid project name does not return error]((https://github.com/rundeck/rundeck/issues/3423)
    * [2.11: Git plugin setup page does not preserve current values in Select fields](https://github.com/rundeck/rundeck/issues/3493)
    * [backport: Zip Slip fix for 2.11](https://github.com/rundeck/rundeck/pull/3486)
