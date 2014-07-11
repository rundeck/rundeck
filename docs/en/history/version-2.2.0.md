% Version 2.2.0
% greg
% 07/11/2014

Release 2.2.0
=============

Date: 2014-07-11

New features:

* Job timeout: specify a maximum duration for a job. If the job execution exceeds this duration, it will be halted (as if killed manually). You can use a simple format like "120m" (120 minutes) or "2h" (2 hours).  You can specify the timeout via a job option by setting it to `${option.name}`.  This only affects the job when executed directly, not when run as a job reference.
* Job retry: specify a maximum retry attempt for a job.  If the job fails or is timed out, it will be retried.  The maximum value can be specified via a job option if set to `${option.name}`.
* Delete executions: delete executions individually or in bulk. Requires a 'delete_execution' action allowed via aclpolicy.

Some bug fixes are included, as well as some pull requests to enhance the LDAP login module:

* Support nested LDAP group membership (see [Added support for nested groups in JettyCachingLdapLoginModule.](https://github.com/rundeck/rundeck/pull/829))
* Support a "supplementalRoles" setting, which can help avoid the `!role` issue. See [Login Module Configuration](../administration/authenticating-users.html#login-module-configuration).

*Upgrade notes:* 

Several domain fields were added. Mysql and H2 should upgrade in place without issue.

## Contributors

* jdmulloy
* Greg Schueler (gschueler)
* Alex Honor (ahonor)
* new23d
* Bart van der Schans (schans)
* Andreas Knifh (knifhen)

## Issues

* [Server listening port number defined in profile instead of service start script](https://github.com/rundeck/rundeck/pull/845)
* [Install fails when /etc/rundeck is a symlink](https://github.com/rundeck/rundeck/pull/842)
* [Multiple Recepient E-mail Notification](https://github.com/rundeck/rundeck/issues/834)
* [Added support for nested groups in JettyCachingLdapLoginModule.](https://github.com/rundeck/rundeck/pull/829)
* [Add Job retry behavior](https://github.com/rundeck/rundeck/pull/825)
* [Job's references are reported as Killed](https://github.com/rundeck/rundeck/issues/821)
* [Ace editor cursor position bug on mac retina display](https://github.com/rundeck/rundeck/issues/820)
* [add a Job Timeout behavior](https://github.com/rundeck/rundeck/issues/815)
* [Added support for the supplementalRoles option to the LDAP Module](https://github.com/rundeck/rundeck/pull/803)
* [Ansi 256 color support doesn't work](https://github.com/rundeck/rundeck/issues/797)
* [Delete executions](https://github.com/rundeck/rundeck/pull/767)