% Version 1.5
% greg
% 02/20/2013

Release 1.5
===========

Date: 2/21/2013

This release introduces a few new features and some bug fixes. The new features
required  some schema changes to the database, so direct upgrading from 1.4 to
1.5 is not possible.   Please read the Upgrading document for upgrade
information.

There is now a new type of plugin, the Workflow Step Plugin, which should allow
rundeck  workflows to integrate with more systems in a more direct fashion.
These plugins can be  either "Workflow Steps" (run once per workflow) or "Node
Steps" (run once per node in a workflow.)

Steps can now have Error Handlers which run if the step fails. They will be
provided with context data about the reason the step failed.

Notable Changes:

* Step Plugins - Plugins can now be created and used for workflow or node steps.
* Error Handlers - Each step in a workflow can now have an error handler that
* will be run if the step fails.  bug fix: Job references in a workflow will now
* only run once, and not operate as a node-step.

Plugin developers:

You will need to update your plugins to work in Rundeck 1.5. Refer to the
Developer Guide for more information.

Issues:

* [User profile page broken](https://github.com/dtolabs/rundeck/issues/308)
* [Update docs for 1.5 release](https://github.com/dtolabs/rundeck/issues/307)
* [Update sample scripts in plugin docs for best practices conformance](https://github.com/dtolabs/rundeck/issues/297)
* [allow optional build parameter to rundeckapp to specify use of a local copy of the grails zip](https://github.com/dtolabs/rundeck/issues/296)
* [expose loglevel for executions](https://github.com/dtolabs/rundeck/issues/293)
* [script based plugin caching issue](https://github.com/dtolabs/rundeck/issues/290)
* [rpm/deb rundeck-config needs to be udpated](https://github.com/dtolabs/rundeck/issues/289)
* [Node dispatch threadcount can be set to blank](https://github.com/dtolabs/rundeck/issues/282)
* [job import: threadcount does not get set](https://github.com/dtolabs/rundeck/issues/281)
* [Now running and History views don't use Job view filter](https://github.com/dtolabs/rundeck/issues/273)
* [History views default to recentFilter=1d, should be all events](https://github.com/dtolabs/rundeck/issues/272)
* [History project filter is not exact](https://github.com/dtolabs/rundeck/issues/271)
* [Allow group path in URL of jobs page to filter groups](https://github.com/dtolabs/rundeck/issues/270)
* [Node dispatch threadcount can be set to blank. export+import fails.](https://github.com/dtolabs/rundeck/issues/269)
* [Bulk delete of jobs via GUI](https://github.com/dtolabs/rundeck/issues/268)
* [add release notes to generated docs](https://github.com/dtolabs/rundeck/issues/264)
* [more compatible rpm dependency for java](https://github.com/dtolabs/rundeck/issues/263)
* [remove unneeded "rdbsupport" config property for 1.5](https://github.com/dtolabs/rundeck/issues/262)
* [API: Now running execution project filter is not exact](https://github.com/dtolabs/rundeck/issues/261)
* [execution output api: xml content problems](https://github.com/dtolabs/rundeck/issues/259)
* [don't combine spaces for scripts output](https://github.com/dtolabs/rundeck/issues/258)
* [Job reference picker has incorrect behavior when clicking a group name](https://github.com/dtolabs/rundeck/issues/255)
* [Error handler failure reason as context data](https://github.com/dtolabs/rundeck/issues/248)
* [Workflow step plugins](https://github.com/dtolabs/rundeck/issues/246)
* [Job folder/group display still buggy](https://github.com/dtolabs/rundeck/issues/241)
* [dispatch -s scriptfile is broken](https://github.com/dtolabs/rundeck/issues/228)
* [can't delete job option](https://github.com/dtolabs/rundeck/issues/227)
* [dispatch with url option don't work](https://github.com/dtolabs/rundeck/issues/225)
* [Job references should run only once within a workflow](https://github.com/dtolabs/rundeck/issues/224)
* [divide by zero error on system info page](https://github.com/dtolabs/rundeck/issues/221)
* [workflow step failure handlers](https://github.com/dtolabs/rundeck/issues/218)
* [make H2 the default rundeck database backend](https://github.com/dtolabs/rundeck/issues/183)
* [UUID permits spaces](https://github.com/dtolabs/rundeck/issues/171)
