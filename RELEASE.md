Release 3.0.0
===========

Date: 2018-07-27

Name: <span style="color: aquamarine"><span class="glyphicon glyphicon-apple"></span> "jalapeño popper aquamarine apple"</span>

## Notes

Welcome to Rundeck 3.0, which consists of both under-the-hood changes (upgrading the underlying application framework) and visual changes (a revamped UI).

The application framework upgrade from Grails 2 to Grails 3 gets a lot of technical debt off of our plate,
and enables some long-asked-for features, such as SSO.

Our new UI is just the first step on a path towards improved usability and flexibility,
and we have big plans for more in the future.

Changes in open-source Rundeck (aka "Rundeck Core") also carry over into [Rundeck Pro](https://www.rundeck.com).

We are also synchronizing our Rundeck Pro and Rundeck Core release versions to make it simpler. However, not
every release of Rundeck Pro will go in our "supported" channel. We will continue to work on the
3.0.x development line of Rundeck Core, leading towards a supported Rundeck Pro 3.0 release to come later.
If you are interested in trying Rundeck Pro 3.0, please visit [Rundeck Pro](https://www.rundeck.com).

## Documentation

Documentation is available at <http://rundeck.org/docs>.

Our documentation project is ongoing, and we want your feedback: corrections, comments, and contributions.
Please let us know how we can improve it: <https://github.com/rundeck/docs>.

## Changes

**Interface**

*New "Jalapeño Popper" UI*

The new 3.0 UI is an aesthetic update with an eye towards what comes next. Based on a new Rundeck UI Style Guide, the entire application has been reviewed and updated with more consistent interfaces that make it easier to use, enable easier enhancements and modifications, and improve cross-browser compatibility.

We are still working on this: please give us feedback!

**Installation**

*Executable war*

The "launcher jar" for Rundeck 2 is gone (long live the launcher jar). However the .war file now operates the same way. Just use the .war in the same way as the previous launcher jar, or deploy it as a webapp.

*Official Docker Image*

Still "incubating", we have published an offical docker image. Please give us your feedback on this 
development effort.

* [Official docker image](https://hub.docker.com/r/rundeck/rundeck/)


**Authentication**

We no longer rely on "container-based" security/authentication (i.e. web.xml auth constraints, coupled with Jetty/Tomcat authentication setup.)	We now use "Spring Security" for Grails, which moves the authentication checks into Rundeck itself.	This enables SSO, Oauth, and other types of authentication which was difficult/impossible to implement before.

The default JAAS authentication method still works, so existing JAAS based configuration should operate as expected.

Pre-authentication modes should work as they did before.

*SSO Integration*

Okta integration is included in Rundeck Pro. See [Rundeck SSO](http://rundeck.org/docs/administration/security/single-sign-on.html)

## Thanks

A lot of work went into the Grails 3 upgrade, many thanks especially to:

* Alberto Hormazabal
* Stephen Joyner

👏👏👏

## Known Issues and Limitations


*Startup error about log4j*

If you update and get an error about Log4j configuration, add a line to your rundeck-config file: `rundeck.log4j.config.file=/.../server/config/log4j.properties` and specify the correct path to a log4j.properties file.

*Customizing web.xml is no longer possible*

The `web.xml` file is no longer available. If you were modifying this after install before, let us know how/for what reason. (Typically modifying session timeout or auth constraints). Also, please see Authentication Changes section.

Note that the [required role customization](https://github.com/rundeck/rundeck/issues/590) is no longer necessary

*Must define JDBC driver class name*

If using Mysql/other DBs which require a JDBC driver, be sure to specify it explicitly in the rundeck-config file, e.g. `dataSource.driverClassName=com.mysql.jdbc.Driver`

*Oracle and other relational DB Support*

Support for Oracle is still considered experimental.  We have confirmed that it works with oracle 12c and 11g,
however we would like your feedback.

## Upgrading

For the most part, Rundeck 3.0 is drop-in compatible with existing Rundeck 2.11 installations.

We recommend doing a fresh install of 3.0.0 and copying your Jobs/projects into it for testing.

If you are upgrading in-place, *Be sure to backup import data/configs before upgrading.*

If you are using the rundeck Launcher jar, replace it with the `.war` artifact, which can be renamed with a `.jar` extension if needed.

See the *Known Issues and Limitations* notes above.


## Additional Enhancements since Rundeck 2.11:

* Limit multiple executions of a job
* Encrypt passwords stored in configuration files
* API additions: Retry a job based on previous execution, access metrics information

## Contributors

* Alberto Hormazabal (ahormazabal)
* Davy Gabard (Kaldor37)
* Greg Schueler (gschueler)
* Jaime Tobar (jtobard)
* Jijo Varghese
* Jocelyn Thode
* Joseph Price (PriceChild)
* OmriShiv
* ProTip
* Stephen Joyner (sjrd218)
* carlos (carlosrfranco)
* scollector65

## Bug Reporters

* Kaldor37
* Nomekrax
* PriceChild
* ahonor
* ahormazabal
* cwaltherf
* diranged
* gschueler
* jijojv
* jquick
* jtobard
* kino71
* ls-initiatives
* ltamaster
* reno-oner
* sebastianbello
* sjrd218
* turlubullu
* wcliff

## Issues

[Milestone 3.0.0](https://github.com/rundeck/rundeck/milestone/76)

* [Can't save node filter](https://github.com/rundeck/rundeck/issues/3740)
* [Fix use of Oracle backend](https://github.com/rundeck/rundeck/pull/3733)
* [Enable file option on scheduled jobs](https://github.com/rundeck/rundeck/pull/3728)
* [Create new branch on git-export](https://github.com/rundeck/rundeck/pull/3714)
* [Fix Error message: ORA-01795 (rd3)](https://github.com/rundeck/rundeck/pull/3705)
* [Add metrics to API, update to APIv25](https://github.com/rundeck/rundeck/pull/3692)
* [Limit user interactive sessions](https://github.com/rundeck/rundeck/issues/3683)
* [First exec/job run of a new project can fail with "no matched nodes"](https://github.com/rundeck/rundeck/issues/3675)
* [NPE on referenced Job](https://github.com/rundeck/rundeck/pull/3652)
* [Problem wth scm+cluster using https](https://github.com/rundeck/rundeck/issues/3623)
* [Show current API version in System Report page](https://github.com/rundeck/rundeck/issues/3598)
* [getUserRoles ldap search attributes filter](https://github.com/rundeck/rundeck/pull/3579)
* [Feature switch to enable job pagination](https://github.com/rundeck/rundeck/pull/3561)
* ["Not found" when pressing cancel on job edit view in French](https://github.com/rundeck/rundeck/issues/3511)
* [Git plugin setup page does not preserve current values in Select fields](https://github.com/rundeck/rundeck/issues/3483)
* [Set a max file size of 25mb configurable](https://github.com/rundeck/rundeck/pull/3477)
* [Error 400 importing projects over 128kb](https://github.com/rundeck/rundeck/issues/3476)
* [Security Advisory: Zip Slip directory traversal vulnerability](https://github.com/rundeck/rundeck/issues/3471)
* [Fixed various french translations](https://github.com/rundeck/rundeck/pull/3458)
* [ACL for uuid](https://github.com/rundeck/rundeck/pull/3456)
* [Fixed various french translations](https://github.com/rundeck/rundeck/pull/3430)
* [importOptions missplaced in yaml/xml export.](https://github.com/rundeck/rundeck/issues/3429)
* [Create project via API with invalid project name does not return error](https://github.com/rundeck/rundeck/issues/3423)
* [BUG: Job Options not appearing in Duplicated Jobs](https://github.com/rundeck/rundeck/issues/3421)
* [Using variable in Storage path job options](https://github.com/rundeck/rundeck/pull/3420)
* [Default UI language set to En ignored - revert to browser preferred value](https://github.com/rundeck/rundeck/issues/3410)
* [duplicate jobs page doesn't show options ](https://github.com/rundeck/rundeck/issues/3384)
* [rundeck access log contains "Ljava.lang.String;" instead of project](https://github.com/rundeck/rundeck/issues/3379)
* [Grails 3 Update](https://github.com/rundeck/rundeck/pull/3290)
* [resources.xml not created](https://github.com/rundeck/rundeck/issues/3185)
* [Can't pass option type file to a jobref](https://github.com/rundeck/rundeck/issues/2598)
* [Using variable in Storage path job options](https://github.com/rundeck/rundeck/issues/2092)
* [Encrypt passwords stored in configuration files](https://github.com/rundeck/rundeck/issues/2062)
* [Don't allow starting rundeckd multiple times.](https://github.com/rundeck/rundeck/pull/1873)
* [ACL based on a job's UUID](https://github.com/rundeck/rundeck/issues/1812)
* [Limit Multiple Executions](https://github.com/rundeck/rundeck/issues/1387)
* [Make the "required role" in web.xml configurable](https://github.com/rundeck/rundeck/issues/590)