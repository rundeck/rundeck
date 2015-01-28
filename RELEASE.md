Release 2.4.0
=============

Date: 2014-12-16

Name: <span style="color: indigo"><span class="glyphicon glyphicon-briefcase"></span> "americano indigo briefcase"</span>

This release has bug fixes and new features, including some GUI improvements.

## New Features

* Job references can override Node Filters. See updates to [XML](../man5/job-xml.html#jobref) and [YAML](../man5/job-yaml.html#job-reference-entry) job definition formats as well.
* Job and Option descriptions can contain markdown and HTML. For Jobs, the first line is the short description, and following lines are interpreted as markdown.
* MSSQL compatibility
* Some GUI tweaks and changes
    * A new Action menu is available next to the Job name in job listing and view pages.  Actions include Edit, Duplicate, Delete, and download XML/YAML definitions.
    * The Delete Job link in the Job Edit page has been removed
    * The Job Edit link in job lists has been replaced with an Action menu
    * Hovering on job name in job lists now triggers the detail popup after a slight delay. Previously it was triggered by hovering on the Edit link.
    * Execution follow page layout has been rearranged
    * Added extended Job descriptions (sanitized Markdown/html)
    * Job Edit/Create page: you are now asked to confirm navigation away from the page if you have made changes to the Job

## Incubator features

* Parallel step execution
    - this can be enabled with `rundeck-config.properties` entry:

            feature.incubator.parallelWorkflowStrategy=true

* ssh-agent forwarding for ssh connections
    - this can be enabled per node, project, or server
        framework.properties:

            framework.local-ssh-agent=<true|false>
            framework.local-ttl-ssh-agent=<time in sec>

        project.properties:

            project.local-ssh-agent=<true|false>
            project.local-ttl-ssh-agent=<time in sec>

        Node properties:

            local-ssh-agent=<true|false>
            local-ttl-ssh-agent=<time in sec>

## Compatibility notes


A bug in API v11 XML responses caused them to sometimes be incorrectly wrapped in a `<result>` element, this has now been corrected.  See the [API Docs](../api/index.html) for information.  The Rundeck API Java Client library has been updated to workaround this issue (for previous versions of Rundeck).

## What is "americano indigo briefcase"?

New versions of Rundeck will have a name based on the version number. The 2.x theme is Coffee, and 2.4.x is "americano". The point release defines a combination of color and icon we can display in the GUI for easier visual differentiation. 2.4.0 is "indigo briefcase".

> Why yes, I'd like an americano, thank you.

## Contributors

* Alex Honor (ahonor)
* Greg Schueler (gschueler)
* Jason (jasonhensler)
* Jonathan Li (onejli)
* Mathieu Payeur Levallois (mathpl)
* Ruslan Lutsenko (lruslan)
* mezbiderli

## Bug Reporters

* Whitepatrick
* adamhamner
* ahonor
* danifr
* davealbert
* foundatron
* gmichels
* gschueler
* jasonhensler
* jcmoraisjr
* katanafleet
* lruslan
* mathpl
* mezbiderli
* new23d
* onejli
* ujfjhz
* zarry

## Issues

* [Cancel editing resource model source doesn't work](https://github.com/rundeck/rundeck/issues/1051)
* [Job run form triggered from Jobs list page incorrectly shows next scheduled time as "never"](https://github.com/rundeck/rundeck/issues/1044)
* [Nodes yaml format: if attribute values are not strings, throws exception](https://github.com/rundeck/rundeck/issues/1039)
* [Rundeck under Tomcat7 dump a lot a of serialization Warning ](https://github.com/rundeck/rundeck/issues/1036)
* [Project Config permission needs Project Create ACL](https://github.com/rundeck/rundeck/issues/1031)
* [ssh-agent forwarding limited to job execution](https://github.com/rundeck/rundeck/pull/1029)
* [Parameterize grails central](https://github.com/rundeck/rundeck/pull/1027)
* [Failure saving project config when empty password field value is used](https://github.com/rundeck/rundeck/issues/1025)
* [Job references can override Node filters](https://github.com/rundeck/rundeck/pull/1024)
* [Allow markup in job and option descriptions](https://github.com/rundeck/rundeck/issues/1020)
* [script-based file copier plugin fails to load project/framework configuration](https://github.com/rundeck/rundeck/issues/1018)
* [Add server uuid element to the /system/info endpoint](https://github.com/rundeck/rundeck/issues/1017)
* [Project config: plugins with same property names will render same values](https://github.com/rundeck/rundeck/issues/1016)
* ["Copy file" step moves file rather than copy ](https://github.com/rundeck/rundeck/issues/1015)
* [Do not look further if nodefilter.dispatch is not set](https://github.com/rundeck/rundeck/pull/1013)
* [rundeck.gui.login.welcome no longer allows html tags like <b></b> to make all or part of the welcome message bold.](https://github.com/rundeck/rundeck/issues/1012)
* [Fix API v11 xml wrapper responses](https://github.com/rundeck/rundeck/pull/1010)
* [Improve validation messages during job import](https://github.com/rundeck/rundeck/issues/1009)
* [APIv11 responses should not include <result> element](https://github.com/rundeck/rundeck/issues/1008)
* [ux: clicking "Top" link when browsing jobs in a group takes you to the rundeck home page](https://github.com/rundeck/rundeck/issues/999)
* [Add MSSQL Support](https://github.com/rundeck/rundeck/pull/972)
* [job.serverUrl not available in reference job.](https://github.com/rundeck/rundeck/issues/965)
* [HMAC request tokens expiring prematurely: "request did not include a valid token"](https://github.com/rundeck/rundeck/issues/960)
* [Issues/927 parallel execution](https://github.com/rundeck/rundeck/pull/929)
* [Delete execution link should not be shown while execution is running](https://github.com/rundeck/rundeck/issues/891)
* [Editing two workflow steps is unsupported](https://github.com/rundeck/rundeck/issues/849)
* [Rundeck using MSSQL datasource](https://github.com/rundeck/rundeck/issues/848)
* [URL encode ${option.[name].value} in Cascading Remote Options](https://github.com/rundeck/rundeck/issues/811)
* [Job editor: don't allow user to lose changes](https://github.com/rundeck/rundeck/issues/254)
* [Cannot use UTF8 in rundeck ](https://github.com/rundeck/rundeck/issues/222)
* [Jobref calls should support overriding node filter params](https://github.com/rundeck/rundeck/issues/131)
