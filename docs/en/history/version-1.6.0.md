% Version 1.6.0
% greg
% 08/02/2013

Release 1.6.0
===========

Date: 2013-08-02

Rundeck 1.6.0 introduces two major changes and a number of bug fixes and enhancements:

* Logging system plugins.
    * New facility allows integration with other systems, like Logstash, and improves Rundeck's behavior in a clustered cloud environment by allowing you to have log files synched to a shared storage more easily.
* Refreshed GUI
    * Did you know it hasn't really been changed since Rundeck 1.0?!  We fixed some of the usability complaints about the old GUI, and our goal was to address usability without having to change functionality, but some of our future GUI enhancement ideas snuck in.

Notable issues:

* [Allow scripts to be run as sudo](https://github.com/dtolabs/rundeck/issues/343) - popular request!
* [Use defaults for optional parameter values, and expand to blank value when missing](https://github.com/dtolabs/rundeck/issues/352) - More intuitive and less annoying!

Many thanks for the helpful contributions from the community!

Contributors:

* Alex Honor
* Greg Schueler
* Jonathan Li
* Kim Ho
* UnsignedLong

Issues:

* [Email subject line for aborted job says "KILLING" should say "KILLED"](https://github.com/dtolabs/rundeck/issues/477)
* [onstart email notification doesn't get saved](https://github.com/dtolabs/rundeck/issues/476)
* [Optional project element for job definitions](https://github.com/dtolabs/rundeck/issues/474)
* [Unprivileged users can access execution api actions](https://github.com/dtolabs/rundeck/issues/472)
* [Improve IE8/9 support](https://github.com/dtolabs/rundeck/issues/471)
* [Rename Admin link to Configure](https://github.com/dtolabs/rundeck/issues/470)
* [Re-running an adhoc command should return to Nodes/Command page](https://github.com/dtolabs/rundeck/issues/469)
* [Save job by default](https://github.com/dtolabs/rundeck/issues/468)
* [Cascading option does not display useful text if no choices available](https://github.com/dtolabs/rundeck/issues/467)
* [RSS feed content for job execution contains inline script](https://github.com/dtolabs/rundeck/issues/466)
* [Job "Created by" information is not accurate](https://github.com/dtolabs/rundeck/issues/465)
* [Cascading options can fail to load remote options, if an option has only a single allowed value](https://github.com/dtolabs/rundeck/issues/464)
* [RSS title should include options & date/time](https://github.com/dtolabs/rundeck/issues/463)
* [RSS feed for jobs does not reflect history view](https://github.com/dtolabs/rundeck/issues/462)
* [Import Job definition: existing UUID will move a job to the current project](https://github.com/dtolabs/rundeck/issues/461)
* [Potential division by zero in email notification](https://github.com/dtolabs/rundeck/issues/460)
* [Remove /api/1/reports/create endpoint](https://github.com/dtolabs/rundeck/issues/457)
* [Remove --noqueue flag from dispatch tool](https://github.com/dtolabs/rundeck/issues/456)
* [LDAP login module does not perform CN validation on certificate when ldaps is specified](https://github.com/dtolabs/rundeck/issues/455)
* [GUI updates for Rundeck 1.6](https://github.com/dtolabs/rundeck/issues/454)
* [project archive/import should allow preserving job UUIDs](https://github.com/dtolabs/rundeck/issues/452)
* [Cluster mode servers should not kill currently running jobs on other server UUIDs](https://github.com/dtolabs/rundeck/issues/451)
* [No Project Access: should display user groups](https://github.com/dtolabs/rundeck/issues/450)
* [aclpolicy example doesn't include application scope in example](https://github.com/dtolabs/rundeck/issues/449)
* [RSS feed should default to not be enabled](https://github.com/dtolabs/rundeck/issues/448)
* [Update Notification plugins: support other property scopes](https://github.com/dtolabs/rundeck/issues/443)
* [Timeout trying to lock table: remove h2 pessimistic locks](https://github.com/dtolabs/rundeck/issues/441)
* [Remove Project dropdown in Job form](https://github.com/dtolabs/rundeck/issues/434)
* [Project archive with incomplete execution causes error on import](https://github.com/dtolabs/rundeck/issues/430)
* [Job execution context should include the server URL, execution URL, and server UUID](https://github.com/dtolabs/rundeck/issues/428)
* [Feature/log storage - adds plugin system for streaming logs and storing log files](https://github.com/dtolabs/rundeck/issues/426)
* [Some log lines are truncated in GUI](https://github.com/dtolabs/rundeck/issues/425)
* [rpm install: new project doesn't automatically create resources.xml (regression)](https://github.com/dtolabs/rundeck/issues/424)
* [Remove cruft libs](https://github.com/dtolabs/rundeck/issues/422)
* [Jar plugins: add ability to resolve classes from embedded libs first](https://github.com/dtolabs/rundeck/issues/419)
* [asUser feature stops working after scheduling a job](https://github.com/dtolabs/rundeck/issues/418)
* ["num parameter is required" error when adding a workflow step to a job](https://github.com/dtolabs/rundeck/issues/416)
* [Feature/log storage - adds plugin system for streaming logs and storing log files](https://github.com/dtolabs/rundeck/issues/414)
* [example ssh-script plugin: disable hostkey verification](https://github.com/dtolabs/rundeck/issues/413)
* [update footers with simplifyops links](https://github.com/dtolabs/rundeck/issues/410)
* [Missing osFamily attribute in Node definition causes command to fail](https://github.com/dtolabs/rundeck/issues/406)
* [Use defaults for optional parameter values, and expand to blank value when missing](https://github.com/dtolabs/rundeck/issues/352)
* [Rundeck execution follow UI splits single steps into multiple sections](https://github.com/dtolabs/rundeck/issues/347)
* [Allow scripts to be run as sudo](https://github.com/dtolabs/rundeck/issues/343)
* [changing project via dropdown does not update job pane.](https://github.com/dtolabs/rundeck/issues/306)
