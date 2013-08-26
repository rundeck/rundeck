% Version 1.6.1
% greg
% 08/24/2013

Release 1.6.1
===========

Date: 2013-08-24

Notable Changes:

* Bug fixes and updates to documentation to reflect changes in 1.6
* Added a unix PAM login module
* Added a feature to allow static definition of API auth tokens in a file
* Restored ability to use Job references as Node steps in a workflow (i.e. execute the job reference for each matched node in the parent job.)
* Fixed issue using the Jenkins rundeck-plugin when authenticating to Rundeck

Contributors:

* Alex Honor
* Greg Schueler
* Martin Strigl

Issues:

* [Static authentication tokens defined in configuration file](https://github.com/dtolabs/rundeck/issues/524)
* [Jenkins rundeck-plugin fails to authenticate to rundeck](https://github.com/dtolabs/rundeck/issues/523)
* [* add the possibility to use ${session.user} for variable expansion in r...](https://github.com/dtolabs/rundeck/issues/521)
* [Add a unix PAM JAAS login module](https://github.com/dtolabs/rundeck/issues/518)
* [Restore node-step functionality for Job References](https://github.com/dtolabs/rundeck/issues/517)
* [API for now running executions over all projects](https://github.com/dtolabs/rundeck/issues/515)
* [Potential hanging job after execution completes](https://github.com/dtolabs/rundeck/issues/514)
* [Project archive import fails to copy log files](https://github.com/dtolabs/rundeck/issues/513)
* [Project import fails if execution has blank threadcount in xml](https://github.com/dtolabs/rundeck/issues/512)
* [Add a "Scaling Rundeck" chapter](https://github.com/dtolabs/rundeck/issues/504)
* [IE Javascript issue: can't enter option name](https://github.com/dtolabs/rundeck/issues/503)
* [GUI Java Script ReferenceError: ExecutionOptions is not defined](https://github.com/dtolabs/rundeck/issues/501)
* [Log API and Web requests](https://github.com/dtolabs/rundeck/issues/499)
* [feature request: add the variable ${job.user.name} to the Option model provider](https://github.com/dtolabs/rundeck/issues/494)
* [copy icon missing in 1.6](https://github.com/dtolabs/rundeck/issues/493)
* [Update docs with 1.6 screenshots and navigation changes](https://github.com/dtolabs/rundeck/issues/479)
* [Job scheduling issue with rundeck 1.5](https://github.com/dtolabs/rundeck/issues/411)
* [Working around Local Command automatic escaping](https://github.com/dtolabs/rundeck/issues/395)
* [Invalid option name can be defined in xml](https://github.com/dtolabs/rundeck/issues/366)
* [Add Option to strip UUID for imported jobs](https://github.com/dtolabs/rundeck/issues/249)
* [scp: ambiguous target  -  for Job with script referencing a node with a space](https://github.com/dtolabs/rundeck/issues/168)
