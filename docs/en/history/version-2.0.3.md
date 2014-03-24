% Version 2.0.3
% greg
% 03/24/2014

Release 2.0.3
=============

Date: 2014-03-24

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

* David Petzel
* Greg Schueler (gschueler)
* Alex Honor (ahonor)

## Issues

* [Create from execution is broken [2.0.2]](https://github.com/rundeck/rundeck/issues/707)
* [Service fails to start on RHEL5](https://github.com/rundeck/rundeck/issues/682)
