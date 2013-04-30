Release 1.4.3
===========

Date: 6/21/2012

Notable Changes:

* Security fix for issue [#555 - vulnerability with ldap authentication](http://rundeck.lighthouseapp.com/projects/59277/tickets/555)
* Scripts can be executed from URLs
* Secure options now have two types (authentication or normal)
* Easily run a job with the same arguments as a previous execution
* Bugfixes

See the [Upgrading Guide](http://rundeck.org/1.4.3/upgrading/) if you are upgrading from Rundeck 1.3.

Tickets: 
* [#538 - Can't change nodes when trying to run a saved job](http://rundeck.lighthouseapp.com/projects/59277/tickets/538)
* [#555 - vulnerability with ldap authentication](http://rundeck.lighthouseapp.com/projects/59277/tickets/555)
* [#554 - in-place upgrade using the launcher leaves old jars in place](http://rundeck.lighthouseapp.com/projects/59277/tickets/554)
* [#563 - multipleExecutions Error](http://rundeck.lighthouseapp.com/projects/59277/tickets/563)
* [#287 - Force the home directory to be /home/rundeck (or perhaps /var/lib/rundeck/)](http://rundeck.lighthouseapp.com/projects/59277/tickets/287)
* [#517 - default ssh key for projects doesn't match rpm install's rundeck user ssh key](http://rundeck.lighthouseapp.com/projects/59277/tickets/517)
* [#552 - allow sudo auth configuration at project/framework level](http://rundeck.lighthouseapp.com/projects/59277/tickets/552)
* [#526 - adhoc execution page shows kill button when it is not authorized](http://rundeck.lighthouseapp.com/projects/59277/tickets/526)
* [#572 - export more vars for script plugins (contents basedir, var dir, etc)](http://rundeck.lighthouseapp.com/projects/59277/tickets/572)
* [#571 - ScriptResourceModel plugin can't use script-args and script-interpreter](http://rundeck.lighthouseapp.com/projects/59277/tickets/571)
* [#551 - Secure option values cannot be used in scripts/commands](http://rundeck.lighthouseapp.com/projects/59277/tickets/551)
* [#537 - Temp files not being removed from /tmp when using a script as a resource model source](http://rundeck.lighthouseapp.com/projects/59277/tickets/537)
* [#523 - passwordless sudo shouldn't fail after the timeout value](http://rundeck.lighthouseapp.com/projects/59277/tickets/523)
* [#518 - undocumented gui default startpage configuration](http://rundeck.lighthouseapp.com/projects/59277/tickets/518)
* [#524 - Secure option values for sudo/ssh do not get passed to sub-jobs](http://rundeck.lighthouseapp.com/projects/59277/tickets/524)
* [#230 - Allow URL values for scriptfiles](http://rundeck.lighthouseapp.com/projects/59277/tickets/230)
* [#528 - authorization for api call to system/info is not checked](http://rundeck.lighthouseapp.com/projects/59277/tickets/528)
* [#550 - warning message after upgrade to 1.4.2](http://rundeck.lighthouseapp.com/projects/59277/tickets/550)
* [#558 - Prevent job names containing slashes (/)](http://rundeck.lighthouseapp.com/projects/59277/tickets/558)
* [#553 - dispatch yields NullPointerException and fails](http://rundeck.lighthouseapp.com/projects/59277/tickets/553)
* [#560 - Re-run a job with the same arguments](http://rundeck.lighthouseapp.com/projects/59277/tickets/560)
* [#567 - Execution page: Collapse view checkbox is set incorrectly](http://rundeck.lighthouseapp.com/projects/59277/tickets/567)
* [#570 - Add "execution id" to job-context data in running jobs](http://rundeck.lighthouseapp.com/projects/59277/tickets/570)
* [#527 - NPE on node view if a node has no description defined](http://rundeck.lighthouseapp.com/projects/59277/tickets/527)
* [#529 - default apitoken aclpolicy differs for rpm/deb and launcher install](http://rundeck.lighthouseapp.com/projects/59277/tickets/529)
* [#545 - rundeck option cannot take integer value](http://rundeck.lighthouseapp.com/projects/59277/tickets/545)
* [#519 - Dispatch to one node only shows "1 node ok" even if job fails](http://rundeck.lighthouseapp.com/projects/59277/tickets/519)
* [#564 - upgrade commons-codec dependency](http://rundeck.lighthouseapp.com/projects/59277/tickets/564)
* [#530 - add faq/documentation about mysql autoreconnect flag](http://rundeck.lighthouseapp.com/projects/59277/tickets/530)
* [#544 - The CronExpression link in docs and Web GUI to http://www.quartz-scheduler.org returns 404](http://rundeck.lighthouseapp.com/projects/59277/tickets/544)
* [#522 - documentation typo/truncation on plugin dev guide](http://rundeck.lighthouseapp.com/projects/59277/tickets/522)

