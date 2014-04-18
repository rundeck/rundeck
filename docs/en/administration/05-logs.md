% Logs

Depending on the installer used, the log files will be under a base
directory:

*   RPM: `/var/log/rundeck`
*   Launcher: `$RDECK_BASE/server/logs`

The following files will be found in the log directory:

     .
     |-- command.log
     |-- rundeck.audit.log
     |-- rundeck.jobs.log
     |-- rundeck.options.log
     |-- rundeck.log
     `-- service.log

Different facilities log to their own files:

* `command.log`: Shell tools log their activity to the command.log
* `rundeck.audit.log`: Authorization messages pertaining to aclpolicy
* `rundeck.job.log`: Log of all job definition changes
* `rundeck.options.log`: Logs remote HTTP requests for Options JSON data
* `rundeck.log`: General Rundeck application messages
* `service.log`: Standard input and output generated during runtime

See the [#log4j.properties](configuration-file-reference.html#log4j.properties) section for information 
about customizing log message formats and location.
