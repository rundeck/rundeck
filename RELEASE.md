Release 1.4.1
===========

Date: 12/14/2011

Notable Changes:

* SSH Password authentication 
* Secondary Sudo password authentication
* Grails upgraded to 1.3.7
* Maven2 build support added
* Bugfixes

Possible breaking changes:

* ACL Policy: Job authorizations now use "name" to filter jobs based on name, not "job" which was erroneously used in 1.4.0.x.  See the [Upgrading Guide](http://rundeck.org/1.4.1/upgrading/)

Tickets:

* [#492 - aclpolicy job resource name key incorrect](http://rundeck.lighthouseapp.com/projects/59277/tickets/492)
* [#482 - support remote Sudo authentication](http://rundeck.lighthouseapp.com/projects/59277/tickets/482)
* [#478 - add Password based SSH](http://rundeck.lighthouseapp.com/projects/59277/tickets/478)
* [#493 - launcher fails to start if "etc" dir already exists](http://rundeck.lighthouseapp.com/projects/59277/tickets/493)
* [#490 - windows build issue: conflicting commons-collections jars](http://rundeck.lighthouseapp.com/projects/59277/tickets/490)
* [#489 - maven2 build issue: rundeckapp tests are not run](http://rundeck.lighthouseapp.com/projects/59277/tickets/489)
* [#491 - Composite Job launch to many threads](http://rundeck.lighthouseapp.com/projects/59277/tickets/491)
* [#486 - aclpolicy context values validation](http://rundeck.lighthouseapp.com/projects/59277/tickets/486)
* [#485 - enter key in job execution option text fields return to job show page](http://rundeck.lighthouseapp.com/projects/59277/tickets/485)
* [#488 - job form shows groups from other projects](http://rundeck.lighthouseapp.com/projects/59277/tickets/488)
* [#483 - documentation aclpolicy "tag" typo](http://rundeck.lighthouseapp.com/projects/59277/tickets/483)
* [#472 - Add pidfile arg to status function call in status action of init script](http://rundeck.lighthouseapp.com/projects/59277/tickets/472)
* [#466 - Add Ability to Bind 4440 to Localhost Address](http://rundeck.lighthouseapp.com/projects/59277/tickets/466)
* [#468 - Using options in names and tags doesn't work anymore](http://rundeck.lighthouseapp.com/projects/59277/tickets/468)
* [#392 - Can't save simple job history filter](http://rundeck.lighthouseapp.com/projects/59277/tickets/392)
* [#465 - project names can't have spaces](http://rundeck.lighthouseapp.com/projects/59277/tickets/465)
* [#479 - rundeck.log is empty after debian installation](http://rundeck.lighthouseapp.com/projects/59277/tickets/479)
* [#480 - script commands fail after debian installation](http://rundeck.lighthouseapp.com/projects/59277/tickets/480)
* [#12 - Upgrade rundeck to latest grails version](http://rundeck.lighthouseapp.com/projects/59277/tickets/12)
* [#477 - add maven2 build](http://rundeck.lighthouseapp.com/projects/59277/tickets/477)
* [#471 - Rundeck link missing context](http://rundeck.lighthouseapp.com/projects/59277/tickets/471)
* [#421 - Multi-Select options GUI should warn about using quotes](http://rundeck.lighthouseapp.com/projects/59277/tickets/421)