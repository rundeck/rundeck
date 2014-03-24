% Version 2.0.2-SNAPSHOT
% greg
% 03/13/2014

Release 2.0.2
=============

Date: 2014-03-13

This is a bugfix release.

*Important Upgrade Note*: This bug [Issue 661](https://github.com/rundeck/rundeck/issues/661)
is caused by the VARCHAR length being too small for a database column.  Unfortunately grails won't
automatically update the VARCHAR size when you upgrade.

* If you are using a different database (mysql, etc.), you will have
to update your schema manually.
* If you are using H2, you can use this
script to update your database:

    1. Shutdown rundeck
    2. Run [rundeck-2.0.2-h2-upgrade.sh](https://gist.github.com/gschueler/9534814#file-rundeck-2-0-2-h2-upgrade-sh)
    3. Start rundeck


## Contributors

* David Wittman
* Greg Schueler
* Alex Honor

## Issues

* [Grammar fix in Job editor view](https://github.com/rundeck/rundeck/pull/702)
* [Import of an Exported Rundeck Project Results in "No such property: year for class: java.lang.String"](https://github.com/rundeck/rundeck/issues/698)
* [Plugin development guide broken into per plugin pages](https://github.com/rundeck/rundeck/issues/695)
* [Custom Attributes unusable in dynamic filters](https://github.com/rundeck/rundeck/issues/691)
* [ Required options are not visible when the job page is refreshed](https://github.com/rundeck/rundeck/issues/690)
* [[2.0.1] Option field is split when use Run Again... button](https://github.com/rundeck/rundeck/issues/684)
* [Rundeck 2.0.1 - cron settings reset in ui](https://github.com/rundeck/rundeck/issues/678)
* [Rundeck 2.0.1 - Improve look of unauthorized access pages](https://github.com/rundeck/rundeck/issues/675)
* [Activity page: can show activity for wrong project](https://github.com/rundeck/rundeck/issues/674)
* [Project import: failure if execution workflow has nodeStep jobref](https://github.com/rundeck/rundeck/issues/673)
* [Rundeck 2.0.1 - Can not hide 'Command' / adhoc section](https://github.com/rundeck/rundeck/issues/672)
* [Update upstart init script to allow service stop command](https://github.com/rundeck/rundeck/pull/670)
* [Rundeck can't be stopped/restarted on Ubuntu](https://github.com/rundeck/rundeck/issues/669)
* [Show all tags button doesn't work correctly on Job run page](https://github.com/rundeck/rundeck/issues/668)
* [rundeck 2.0.1 - change target nodes - sql error when run is activated ](https://github.com/rundeck/rundeck/issues/661)
* [Rundeck 2.0.1 - remove UUID doesn't work when uploading job definition ](https://github.com/rundeck/rundeck/issues/658)
* [Rundeck 2.0.1 - 2013 copyright in footer](https://github.com/rundeck/rundeck/issues/657)
* [Export archive failed with NullPointerException in Rundeck 2.0.0](https://github.com/rundeck/rundeck/issues/656)

