% Version 2.1.2
% greg
% 05/23/2014

Release 2.1.2
=============

Date: 2014-05-23

Bugfix release. Some enhancements:

* ANSI colorized output support
* Updated icons to distinguish command, script and script-file steps

*Upgrade notes:* 

1. If you are upgrading from 2.0.1 or earlier, see the notes about schema changes in the previous release notes: [2.0.2 Release Notes](../history/version-2.0.2.html).
2. See the [Upgrading Guide](http://rundeck.org/2.1.0/upgrading/index.html)

## Contributors

* Greg Schueler (gschueler)

## Issues

* [Create job with nodeset: breaks using tagsa+tagb](https://github.com/rundeck/rundeck/issues/795)
* [node filter exclusion of tags doesn't seem to work](https://github.com/rundeck/rundeck/issues/793)
* [Add separate icons for script and scriptfile steps #787](https://github.com/rundeck/rundeck/pull/792)
* [Built in step icons should differ for command, script, url](https://github.com/rundeck/rundeck/issues/787)
* [Ansi color output support](https://github.com/rundeck/rundeck/pull/786)
* [Wrong elapsed time for jobs running more than 24 hours.](https://github.com/rundeck/rundeck/issues/784)
* [Directory node source should merge tags for multiple nodes](https://github.com/rundeck/rundeck/issues/783)
* [Expand variables in ssh-keypath from a node #780](https://github.com/rundeck/rundeck/pull/782)
* [SSH key storage path fails for node executor](https://github.com/rundeck/rundeck/issues/781)
* [SSH keypath for a node should allow embedded context variables](https://github.com/rundeck/rundeck/issues/780)
* [Add loading indicator for nodes in nodes/commands page #759](https://github.com/rundeck/rundeck/pull/779)
* [Add missing context to execution data](https://github.com/rundeck/rundeck/pull/778)
* [Importing a job always sets the month to *](https://github.com/rundeck/rundeck/issues/774)
* [crontab parsing broken on intial import](https://github.com/rundeck/rundeck/issues/773)
* [step description not encoded in form field](https://github.com/rundeck/rundeck/issues/771)
* [Node results table should not uppercase attribute headers](https://github.com/rundeck/rundeck/issues/770)
* [NPE if node step throws exception](https://github.com/rundeck/rundeck/issues/769)
* [Create and Run job button in job create form is redundant](https://github.com/rundeck/rundeck/issues/768)
* [2.1.1 Debug output causes NPE when executing script or scp file](https://github.com/rundeck/rundeck/issues/766)
* [HipChat Plugin Error with Rundeck 2.1.1](https://github.com/rundeck/rundeck/issues/764)
* [Documentation: API tokens access has wrong URL path](https://github.com/rundeck/rundeck/issues/763)
* [Display better status message when project node source is loading](https://github.com/rundeck/rundeck/issues/759)
* [Receiving 400 error after logging out and trying to log back in](https://github.com/rundeck/rundeck/issues/758)
* [allow per-job custom email subject line for #755](https://github.com/rundeck/rundeck/pull/756)
* [Allow per-job custom email notification subject line](https://github.com/rundeck/rundeck/issues/755)
* [Windows/chrome UI: long node list and job args overflow the page](https://github.com/rundeck/rundeck/issues/717)
* [plugin not found: plugin cache dir has wrong permissions](https://github.com/rundeck/rundeck/issues/565)
* [Unable to use multiple LDAP servers](https://github.com/rundeck/rundeck/issues/541)
* [SSL truststore path is misconfigured by default in OS packages](https://github.com/rundeck/rundeck/issues/507)
* [Add rundeck server name and UUID context variables to option model provider url](https://github.com/rundeck/rundeck/issues/500)

