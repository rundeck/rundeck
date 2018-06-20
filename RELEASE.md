Release 3.0.0-alpha2
===========

Date: 2018-06-14

Name: <span style="color: aquamarine"><span class="glyphicon glyphicon-apple"></span> "jalapeño popper aquamarine apple"</span>

## Prerelease Notes

**This is a prerelease** of Rundeck 3.0. There have been a number of changes and we are asking for community feedback
on this version. If you find a bug or regression, please file an issue at <https://github.com/rundeck/rundeck/issues>.

We are publishing the snapshot documentation for Rundeck 3.0 at this URL: <http://rundeck.org/3.0.x-SNAPSHOT/>. Currently the Upgrading Guide is *not* updated with specific Rundeck 3.0 information, so please read the notes below.

The largest change is that we've upgraded the underlying web-app framework to Grails 3. This affects some aspects of install and configuration:

Install:

* The "launcher jar" for Rundeck 2 is gone (long live the launcher jar). However the .war file now operates the same way. Just use the .war in the same way as the previous launcher jar, or deploy it as a webapp.

Configuration:

* The `web.xml` file is no longer available. If you were modifying this after install before, let us know how/for what reason. (Typically modifying session timeout or auth constraints). Also, please see Authentication Changes below.
* If using Mysql/other DBs which require a JDBC driver, be sure to specify it explicitly in the rundeck-config file, e.g. `dataSource.driverClassName=com.mysql.jdbc.Driver`
* If you update and get an error about Log4j configuration, add a line to your rundeck-config file: `rundeck.log4j.config.file=/.../server/config/log4j.properties` and specify the correct path to a log4j.properties file.

Authentication Changes:

* We no longer rely on "container-based" security/authentication (i.e. web.xml auth constraints, coupled with Jetty/Tomcat authentication setup.)
	We now use "Spring Security" for Grails, which moves the authentication checks into Rundeck itself.
	This enables SSO, Oauth, and other types of authentication which was difficult/impossible to implement before.
* The default JAAS authentication method still works, so existing JAAS based configuration should operate as expected.
* Pre-authentication modes should work as they did before.
* SSO integration: *documentation TBD*

Thanks:

A lot of work went into the Grails 3 upgrade, many thanks especially to:

* Alberto Hormazabal
* Stephen Joyner

👏👏👏

## Upgrading

For the most part, Rundeck 3.0 is drop-in compatible with existing Rundeck 2.11 installations.

We recommend doing a fresh install of 3.0.0-alpha1 and copying your Jobs/projects into it for testing.

If you are upgrading in-place, *Be sure to backup import data/configs before upgrading.*

If you are using the rundeck Launcher jar, replace it with the `.war` artifact, which can be renamed with a `.jar` extension if needed.

See the *Configuration* notes above.


## Additional Enhancements since Rundeck 2.11:

* Limit multiple executions of a job
* Encrypt passwords stored in configuration files

## Contributors

* Alberto Hormazabal (ahormazabal)
* Davy Gabard
* Davy Gabard (Kaldor37)
* GitHub (web-flow)
* Greg Schueler (gschueler)
* Jaime Tobar (jtobard)
* Jocelyn Thode
* Joseph Price (PriceChild)
* Luis Toledo (ltamaster)
* Martin (martinbydefault)
* OmriShiv
* ProTip
* Romain LEON (PeekLeon)
* Stephen Joyner
* Stephen Joyner (sjrd218)
* carlos (carlosrfranco)
* scollector65

## Bug Reporters

* Kaldor37
* Nomekrax
* PriceChild
* ahormazabal
* gschueler
* jquick
* jtobard
* kino71
* ltamaster
* sebastianbello
* sjrd218
* turlubullu
* wcliff

## Issues

[Milestone 3.0.0](https://github.com/rundeck/rundeck/milestone/76)

* [Git plugin setup page does not preserve current values in Select fields](https://github.com/rundeck/rundeck/issues/3483)
* [Set a max file size of 25mb configurable](https://github.com/rundeck/rundeck/pull/3477)
* [Error 400 importing projects over 128kb](https://github.com/rundeck/rundeck/issues/3476)
* [Security Advisory: Zip Slip directory traversal vulnerability](https://github.com/rundeck/rundeck/issues/3471)
* [ACL for uuid](https://github.com/rundeck/rundeck/pull/3456)
* [Addresses issue #2062 ](https://github.com/rundeck/rundeck/pull/3432)
* [Fixed various french translations](https://github.com/rundeck/rundeck/pull/3430)
* [importOptions missplaced in yaml/xml export.](https://github.com/rundeck/rundeck/issues/3429)
* [Create project via API with invalid project name does not return error](https://github.com/rundeck/rundeck/issues/3423)
* [BUG: Job Options not appearing in Duplicated Jobs](https://github.com/rundeck/rundeck/issues/3421)
* [Using variable in Storage path job options](https://github.com/rundeck/rundeck/pull/3420)
* [duplicate jobs page doesn't show options ](https://github.com/rundeck/rundeck/issues/3384)
* [rundeck access log contains "\[Ljava.lang.String;" instead of project](https://github.com/rundeck/rundeck/issues/3379)
* [Grails 3 Update](https://github.com/rundeck/rundeck/pull/3290)
* [Using variable in Storage path job options](https://github.com/rundeck/rundeck/issues/2092)
* [Encrypt passwords stored in configuration files](https://github.com/rundeck/rundeck/issues/2062)
* [Limit Multiple Executions](https://github.com/rundeck/rundeck/issues/1387)
* [i18n Update: node filter help](https://github.com/rundeck/rundeck/pull/3383)
