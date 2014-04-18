% Backup and Recovery

## Backup

While running, export the Job definitions if you do not have these in source control:

(1) Export the jobs. You will have to do this for each project

    ~~~~~~ {.bash}
    rd-jobs list -f /path/to/backup/dir/project1/jobs.xml -p project1
    rd-jobs list -f /path/to/backup/dir/project2/jobs.xml -p project2
    ...
    ~~~~~~

(2) Stop the server. See: [startup and shutdown](#startup-and-shtudown). (Rundeck data file backup should only be done with the server down.)

    ~~~~~~ {.bash}
    rundeckd stop
    ~~~~~~ 

(3) Copy the data files. (Assumes file datastore configuration). The
location of the data directory depends on the installation method:

   * RPM install: `/var/lib/rundeck/data`
   * Launcher install: `$RDECK_BASE/server/data`

    ~~~~~~ {.bash}
    cp -r data /path/to/backup/dir
    ~~~~~~ 
             
(4) Copy the log (execution output) files.

   * RPM install: `/var/lib/rundeck/logs`
   * Launcher install: `$RDECK_BASE/var/logs`

    ~~~~~~ {.bash}
    cp -r logs /path/to/backup/dir
    ~~~~~~

(5) Start the server

    ~~~~~~ {.bash}
    rundeckd start
    ~~~~~~

## Recovery

(1) Stop the server. See: [startup and shutdown](startup-and-shtudown.html). (Rundeck recovery should only be done with the server down.)

    ~~~~~~ {.bash}
    rundeckd stop
    ~~~~~~ 

(2) Restore data/logs dir from backup (Refer to above for appropriate log/data path):

    ~~~~~~ {.bash}
    cp -r /path/to/backup/logs logspath
    cp -r /path/to/backup/data datapath
    ~~~~~~ 


(3) Start the server:

    ~~~~~~ {.bash}
    rundeckd start
    ~~~~~~ 

(4) Reload the Job definitions. You will have to do this for each project:

    ~~~~~~ {.bash}
    rd-jobs load -f /path/to/backup/dir/project1/jobs.xml -p project1
    rd-jobs load -f /path/to/backup/dir/project2/jobs.xml -p project2
    ~~~~~~ 

## Project Import and Export

As of Rundeck 1.4.4, you can export a Project's database contents into an archive file, and later import it into another project.

You can use this mechanism for:

* backup
* migration from one database backend to another
* upgrading from one rundeck version to another

### Export an archive

To export, visit the "Admin" link in the Rundeck page header.

Click on the link under "Export Archive" to download an archive containing the project Jobs, Executions and History.

This archive can be imported into any other Rundeck project.

The archive will contain:

* All Job definitions from the project
* All Executions from the project (both Job and Adhoc executions)
* All Execution log files (output logs)
* All History reports from the project

Note that the archive **will not contain**:

* The Project config file `project.properties` located under your `$RDECK_BASE/projects/[name]/etc`
* Resource definitions (such as `resources.xml` or resources received from external providers.)

You should back up those contents separately if necessary.

### Import an archive

To import the contents of an exported archive, visit the "Admin" link in the Rundeck page header.

Click on "Import Archive" to display the import form.  

Choose the rundeck archive file to import (should end with ".rdproject.jar").

Click "Import".

The import process:

* Creates any Jobs in the archive not found in this project with a new unique UUID
* Updates any Jobs in the archive that match Jobs found in the project (group and name match)
* Creates new Executions for the imported Jobs, and creates the output log files on disk
* Creates new History reports for imported Executions and Jobs

Note that because the archive does not contain the project configuration or resource definitions, you
will have to configure those separately for the new or updated project.