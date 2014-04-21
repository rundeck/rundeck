% Version 2.0.4
% greg
% 04/18/2014

Release 2.0.4
=============

Date: 2014-04-18

This is a bugfix release (see Issues.)

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

* Greg Schueler (gschueler)

## Issues

* [Required option value icon shown even when value is set](https://github.com/rundeck/rundeck/issues/740)
* [[2.0.3] Documentation: job 'create' access not shown in admin example ACL policy](https://github.com/rundeck/rundeck/issues/738)
* [debian package should not include jetty6 libs](https://github.com/rundeck/rundeck/issues/735)
* [Job Run again after changing nodes doesn't keep the same nodes](https://github.com/rundeck/rundeck/issues/734)
* [Allow serving content with an SSL terminated proxy](https://github.com/rundeck/rundeck/issues/732)
* [ressources.xml handling questions](https://github.com/rundeck/rundeck/issues/730)
* [Default Node File Copier -> Script Execution -> Missing variable](https://github.com/rundeck/rundeck/issues/725)
* [V2.0.2 : Job report's page is broken when Job's parameters are too Long](https://github.com/rundeck/rundeck/issues/721)
* [Job fails with "No matched nodes" unless target nodes explicitly selected](https://github.com/rundeck/rundeck/issues/719)
* [Some UI issues in V2.X but not critic](https://github.com/rundeck/rundeck/issues/717)
* ["run -f" in follow mode provides incorrect return code to shell](https://github.com/rundeck/rundeck/issues/714)
* [Rundeck 2.0.2 - Project Configuration error when cancelling](https://github.com/rundeck/rundeck/issues/708)
* [Deep link to UI not honoured is user authenticates (2.0.x)](https://github.com/rundeck/rundeck/issues/703)
* [Large execution output takes a long time to load](https://github.com/rundeck/rundeck/issues/655)
* [Job option names are not modifiable in 1.6.1+](https://github.com/rundeck/rundeck/issues/559)