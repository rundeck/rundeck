Release 2.0.0-beta1
===========

Date: 2014-01-09

Rundeck 2.0.0-beta1 introduces a large number of major changes, both enhancements and bug fixes, that build on top of the work done for Rundeck 2.0.0-alpha1

## Beta1

This is the first "beta" for Rundeck 2.0, please read these notes:

* Upgrading from Rundeck 1.6 is not yet described in the documentation.  As such, if you want to use your 1.6 projects, please do a project Export and then an import into a clean install of rundeck 2.0 beta1.
* There are probably some bugs due to some of the new features and updates

Please report bugs you find:

* Github Issues: <https://github.com/dtolabs/rundeck/issues>
* Mailing list: <rundeck-discuss@googlegroups.com>
* IRC: #rundeck on freenode.net ([webchat link](http://webchat.freenode.net/?nick=rundeckuser.&channels=rundeck&prompt=1))
* Twitter: [@rundeck](https://twitter.com/rundeck)

## Notable Changes

* New feature: live execution state view
    * Live view of your job execution to see what step is running where.
    * Collates the output by node and step.
    * Instantly drill in to view the output for the step and node that failed
    * View node metrics and collated output.
    * **Please give a big thank-you to (an anonymous) "sponsored development client" for funding the work for this feature!**
* New Projects home page displays readme and message of the day files that can be customized with markdown to display notices to users
* Improved Node filter
    * Now supports attribute searches
    * New filter expression syntax, simple examples: 
        - `tags: a+b` filters by tags
        - `environment: (prod|qa)` Regular expression filter on an attribute called `environment`
    * New simpler UI
* Improved Nodes page to better navigate the data about the nodes in your infrastructure
    * Navigate nodes through attribute links
    * Run a job or execute a command from filter sets
* New Commands page dedicated to ad hoc command execution.
    * Controls over thread count and error handling
    * Reference saved defined filters or express your own.
* Improved Activity views with tabbed views for common queries
    * Tabs for Now running, recent, errors and executions by you.
* Box score metrics for executions. Use Rundeck as an information radiator.
    * Percent completed and Success/Failure metrics displayed for each execution 

## Enhancements

* Knockout JS, Bootstrap 3, flatly
* Caching and error catching for resource model source plugins
* New node filter expressions
* Execution model API and json representation stored with log output
* Optimized internals to reduce service loading time
* Cruft removal  (legacy formats and syntaxes)
* Step descriptions for job steps. Give your step a brief description to show your users during execution.
* Copy file step plugin copies files from rundeck server to remote nodes.
* API
    * Better REST-ful behavior
    * removed use of 302 redirects between requests and some responses


## Acknowledgements

* Alex Honor
* Greg Schueler
* Damon Edwards
* John Burbridge
* Matt Wise at Nextdoor.com
* Etienne Grignon at Disney
* Srinivas Peri and Aya Ivtan at Adobe
* Mark Maun and Eddie Wizelman at Ticketmaster

## Issues

* [Rundeck should catch errors and cache node data from Resource Model Source providers](https://github.com/dtolabs/rundeck/issues/609)
* [MS IE / Rundeck Nodes Page: "Enter a shell command" caption not visible](https://github.com/dtolabs/rundeck/issues/607)
* [Refactor some execution finalization code for #511](https://github.com/dtolabs/rundeck/pull/604)
* [Node attributes with ":" character breaks XML serialization.](https://github.com/dtolabs/rundeck/issues/603)
* [Remove rpm java dependency](https://github.com/dtolabs/rundeck/issues/601)
* [rundeck does not output spaces/tabs properly](https://github.com/dtolabs/rundeck/issues/600)
* [edit job and duplicate to a new job buttons not-clickable in 2.0.0-1-alpha1](https://github.com/dtolabs/rundeck/issues/598)
* [Send Notification not saved](https://github.com/dtolabs/rundeck/issues/594)
* [Delete job](https://github.com/dtolabs/rundeck/issues/592)
* [Missing username causes failure with "Execution failed: X: null", even if project.ssh.user is set](https://github.com/dtolabs/rundeck/issues/589)
* [Default Option values are ignored when a jobs is referenced from another job..](https://github.com/dtolabs/rundeck/issues/577)
* [Remove dead/unused keys from framework.properties](https://github.com/dtolabs/rundeck/issues/575)
* [Remove auto-project creation from Setup](https://github.com/dtolabs/rundeck/issues/574)
* [The quick 'Run' page should allow for thread count adjustment as well as 'on failure' behavior changes.](https://github.com/dtolabs/rundeck/issues/510)
* [obsolete RDECK_HOME and rdeck.home](https://github.com/dtolabs/rundeck/issues/508)
* ['group' and 'user' field should be wildcard-able in the aclpolicy files](https://github.com/dtolabs/rundeck/issues/359)
* [upgrade grails to 2.x](https://github.com/dtolabs/rundeck/issues/219)
