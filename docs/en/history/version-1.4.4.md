Release 1.4.4
===========

Date: 10/26/2012

This release marks the end of the 1.4 development cycle, and includes bug fixes and a few new features.

We are planning to make some changes in the DB schema for the next release (1.5.x) that may not be backwards 
compatible so have included a feature to export a Rundeck project into an archive file.  This will allow us
to change the schema yet still allow users to migrate their projects.

Notable Changes:

* bug fixes (scheduled jobs, mail notifications, rd-jobs yaml output, jenkins plugin + parallel jobs)
* Project archive/import - download an archive of Jobs, Executions and History that can be imported into a different project
* Added a second level of sudo password support
* Add a 'purge' action to rd-jobs tool to delete jobs
* Better support for Tomcat war deployment
* View all nodes button in Run page
* Cascading option values from remote URLs
* CLI tools can follow execution output from the server (rd-queue, run, dispatch)
* API enhancements:
    * query for executions and history reports
    * retrieve execution output

Issues: 

* [remote options URL failure allows text field input even if option is restricted](https://github.com/dtolabs/rundeck/issues/215) (bug)
* [project archive/import](https://github.com/dtolabs/rundeck/issues/212) (enhancement)
* [multiple sudo authentication support](https://github.com/dtolabs/rundeck/issues/211) (enhancement)
* [Document syntax of arguments passed to the run command](https://github.com/dtolabs/rundeck/issues/208) (enhancement, documentation)
* [add purge option to rd-jobs tool](https://github.com/dtolabs/rundeck/issues/207) (enhancement, cli)
* [Add query API for executions](https://github.com/dtolabs/rundeck/issues/205) (enhancement, api)
* [CLI tools can't authenticate to a tomcat war deployment of rundeck](https://github.com/dtolabs/rundeck/issues/204) (bug)
* [Allow history API to query for list of job names](https://github.com/dtolabs/rundeck/issues/203) (enhancement, api)
* [javascript problem: Can't change nodes when trying to run a saved job](https://github.com/dtolabs/rundeck/issues/194) (bug, ux)
* [Rundeck jobs fail to execute sometimes ](https://github.com/dtolabs/rundeck/issues/193) (bug)
* [Rundeck war should not contain servlet api libraries](https://github.com/dtolabs/rundeck/issues/192) (enhancement)
* [deb dependency requires GUI libraries](https://github.com/dtolabs/rundeck/issues/191) (enhancement, packaging)
* [Enable property expansion in framework level default ssh user ](https://github.com/dtolabs/rundeck/issues/189) (enhancement, configuration, ssh)
* [Mail notifications are broken in 1.4.3](https://github.com/dtolabs/rundeck/issues/186) (bug)
* [resource model source URL basic auth support is broken](https://github.com/dtolabs/rundeck/issues/184) (bug)
* [Update wiki/documentation for remote option provider](https://github.com/dtolabs/rundeck/issues/182) (documentation)
* [Parallel/Concurrent jobs fail](https://github.com/dtolabs/rundeck/issues/180) (bug)
* [cli tool rd-jobs format yaml does not generate any content in file for 1.4.3](https://github.com/dtolabs/rundeck/issues/179) (bug, cli)
* [Scheduled RunDeck jobs no longer work with RunDeck 1.4.3](https://github.com/dtolabs/rundeck/issues/178) (bug, scheduler, jobs)
* [Allow disabling of hover popups](https://github.com/dtolabs/rundeck/issues/174) (enhancement)
* [Add a button to view all nodes in nodes filter view](https://github.com/dtolabs/rundeck/issues/172) (enhancement, ux, filters)
* [need REST interface to retrieve execution ouput](https://github.com/dtolabs/rundeck/issues/145) (enhancement, api)
* [dispatcher needs option to queue job but also observe log](https://github.com/dtolabs/rundeck/issues/142) (enhancement)
* [Ability to change the default number of lines to display for the TAIL output in the rundeck job execution history](https://github.com/dtolabs/rundeck/issues/109) (enhancement)
* [feature for cascading select list from options provider](https://github.com/dtolabs/rundeck/issues/80) (enhancement)
