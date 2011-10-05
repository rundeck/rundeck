Release 1.3.2
===========

Date: 10/5/2011

This release fixes a number of bugs, and merges in some fixes drawn from the
development branch, as well as updates the ACL Policy support to improve 
authorization checks.

Release 1.3.2 fixes an issue with Tomcat based war deployments, and upgrades the included mysql-connector jar.

## Changes

Improved ACL Policy features:

* Project-level access control
    * your `<context project="abc">` now means something in XML format
    * Added `context:  { project: 'abc' } ` to YAML format
    * See updated aclpolicy format docs
* Improved authorization checks for Jobs
    * `workflow_create` action is required to match job name/group/project, when creating a new job or renaming/grouping/projecting
    * `workflow_run` action is required before any job can be run. previously "Create and Run" did not check this.
    * `events_read` action is required to see any history log events for a job, as well as adhoc jobs

See the Administration section of the documentation for more information about ACL Policy restrictions.

**Note**: These changes are intended to aid existing 1.3 users with aclpolicy restriction requirements using XML based files.  In RunDeck 1.4 the ACL Policy is completely revamped, requiring changes to your aclpolicy files on upgrade, and the XML format is no longer supported.

Changes to fix issues with Oracle/Mysql backend:

* Merged in table/field name fixes from development
* Merged field type changes (VARCHAR to TEXT/CLOB) for some fields that require longer strings
* Fixed some failing testcases when tested against Oracle backend
* Fixed some bugs using Oracle backend
    * Notably, ScheduledExecution.description field constraints were "(nullable:false, blank:true)", but Oracle cannot distinguish blank/null causing an error when blank was inserted.  changed to "nullable:true"

These changes should not affect Rundeck 1.3 upgrades with File-based installations.

## Closed Issues:

1.3.2:

* [#453 Fix issues with Tomcat war deployment](http://rundeck.lighthouseapp.com/projects/59277/tickets/453-fix-issues-with-tomcat-war-deployment)
* [#456 Request Mysql connector Jar Update](http://rundeck.lighthouseapp.com/projects/59277-development/tickets/456-request-mysql-connector-jar-update)

1.3.1:

* [#448 - RSS cannot be disabled](http://rundeck.lighthouseapp.com/projects/59277/tickets/448)
* [#447 - Default option filtering not honored from workflow (1.3.1)](http://rundeck.lighthouseapp.com/projects/59277/tickets/447)
* [#443 - fix issues with Oracle backend](http://rundeck.lighthouseapp.com/projects/59277/tickets/443)
* [#441 - events_read authorization checks on history needed](http://rundeck.lighthouseapp.com/projects/59277/tickets/441)
* [#439 - "Create and Run" is not limited by lack of workflow_execute](http://rundeck.lighthouseapp.com/projects/59277/tickets/439)
* [#438 - "project" context support for aclpolicy](http://rundeck.lighthouseapp.com/projects/59277/tickets/438)
* [#437 - workflow_create auth fix](http://rundeck.lighthouseapp.com/projects/59277/tickets/437)
* [#446 - upgrade snakeyaml](http://rundeck.lighthouseapp.com/projects/59277/tickets/446)
