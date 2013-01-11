Release 1.5 RC1
===========

Date: 1/9/2013

This release introduces a few new features and some bug fixes. The new features required some schema changes to the database, so 
direct upgrading from 1.4 to 1.5 is not possible.  Please read the Upgrading document for upgrade information.

There is now a new type of plugin, the Workflow Step Plugin, which should allow rundeck workflows to integrate with more systems in a more direct fashion. These plugins can be either "Workflow Steps" (run once per workflow) or "Node Steps" (run once per node in a workflow.)

Steps can now have Error Handlers which run if the step fails. They will be provided with context data about the reason the step failed.

Notable Changes:

* Step Plugins - Plugins can now be created and used for workflow or node steps. 
* Error Handlers - Each step in a workflow can now have an error handler that will be run if the step fails. 
* bug fix: Job references in a workflow will now only run once, and not operate as a node-step.

Issues:

* [remove unneeded "rdbsupport" config property for 1.5](https://github.com/dtolabs/rundeck/issues/262)
* [API: Now running execution project filter is not exact](https://github.com/dtolabs/rundeck/issues/261)
* [execution output api: xml content problems](https://github.com/dtolabs/rundeck/issues/259)
* [don't combine spaces for scripts output](https://github.com/dtolabs/rundeck/issues/258)
* [Job reference picker has incorrect behavior when clicking a group name](https://github.com/dtolabs/rundeck/issues/255)
* [Error handler failure reason as context data](https://github.com/dtolabs/rundeck/issues/248)
* [Workflow step plugins](https://github.com/dtolabs/rundeck/issues/246)
* [Job folder/group display still buggy](https://github.com/dtolabs/rundeck/issues/241)
* [can't delete job option](https://github.com/dtolabs/rundeck/issues/227)
* [dispatch with url option don't work](https://github.com/dtolabs/rundeck/issues/225)
* [Job references should run only once within a workflow](https://github.com/dtolabs/rundeck/issues/224)
* [divide by zero error on system info page](https://github.com/dtolabs/rundeck/issues/221)
* [workflow step failure handlers](https://github.com/dtolabs/rundeck/issues/218)
* [make H2 the default rundeck database backend](https://github.com/dtolabs/rundeck/issues/183)
