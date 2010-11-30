% RD-JOBS(1) RunDeck User Manuals | Version 1.0
% Greg Schueler; Alex Honor
% November 20, 2010

# NAME

rd-jobs - List or load jobs to the RunDeck server

# SYNOPSIS

dispatch [*action*] [*action options*]...

# DESCRIPTION

The dispatch command is used to remotely execute ad-hoc shell commands
on a set of nodes which are specified by the filter options.

The rd-jobs command lists Jobs stored on the server and optionally downloads them as XML, or uploads Job XML to the server from a file.

The tool works in one of two *ACTION* modes:

* list
: list the stored Jobs on the server (default action)

* load
: upload XML job definitions up to the server

# OPTIONS


-h, \--help
: Print usage message.

-v
: Run verbosely.

## LIST ACTION OPTIONS

-g, \--group *GROUP*
: Group name. List jobs within this group or sub-group

-i, \--idlist	*ID*
: Job ID List. List Jobs with these IDs explicitly. Comma-separated, e.g.: 1,2,3.

-n, \--name *NAME*
: Job Name. List jobs matching this name.

-p, \--project *PROJECT*
: Project name. List jobs within this project.

-f, \--file *FILE*
: File path. For list action, path to store the job definitions found in XML.

# LOAD ACTION OPTIONS

-d, \--duplicate *update|skip|create*
: Duplicate job behavior option. When loading jobs, treat definitions that already exist on the server in the given manner: 'update' existing jobs,'skip' the uploaded definitions, or 'create' them anyway. (load action. default: update)

-f, \--file *FILE*
: File path. For load action, path to an XML FILE to upload.


# LIST ACTION 

The List action queries the server for a list of matching jobs, and displays the result list. Optionally the definitions of the matching jobs can be stored in a file if `-f` is specified.

The jobs can be specifed explicitly by ID using the `-i/--idlist` option. Otherwise they are searched using the options as filter criteria, and all matching Jobs are returned.

If no options to the list action are supplied, then all Jobs on the
server are returned.

*Examples*

List all jobs on the server:

    rd-jobs

List only a single job by ID:

    rd-jobs -i 123

List a set of jobs by ID and store them in a file:

    rd-jobs -i 1,23,4 --file out.xml

List all jobs in the project "demo"

    rd-jobs -p demo

Output from the command will list the job name, ID number in brackets,
and the URL to view the Job in the server.

    Found 5 jobs:
       1: Build all [38] <http://localhost:8080/rundeck/scheduledExecution/show/38>
       2: Build and Update Server0 [31] <http://localhost:8080/rundeck/scheduledExecution/show/31>
       3: Build and Update Server1 [17] <http://localhost:8080/rundeck/scheduledExecution/show/17>
       4: Build and Update Server2 [45] <http://localhost:8080/rundeck/scheduledExecution/show/45>
       5: Deploy Server4 [46] <http://localhost:8080/rundeck/scheduledExecution/show/46>
   
# LOAD ACTION
   
The Load action uploads the specified file to the server, and the list of loaded jobs are displayed. If any Jobs cannot be stored (e.g. the user is unauthorized to run a certain command), the list of unsuccessfully created jobs are also displayed.

The `-d,\--duplicate` option lets you specify what should happen if any
of the Job definitions have the same Name and Group of an existing Job
on the server. The default option is "update", which means to
overwrite the existing definitions with the new version. "skip" means
to ignore the uploaded definition. "create" means to create a new Job
with the uploaded definition (hence making the Group+Name non-unique).

*Examples*

Load a file to the server:

    rd-jobs load -f jobs.xml

Output:

    Total Jobs Uploaded: 2 jobs
    Succeeded creating/updating 2  Jobs:
       1: Build and Update Server2 [45] <http://localhost:8080/rundeck/scheduledExecution/show/45>
       2: Deploy Server4 [46] <http://localhost:8080/rundeck/scheduledExecution/show/46>

If a Job was not successfully created, you will see it mentioned along with a message about why it failed. The number next to the Job name indicates the index of the definition in the uploaded XML:

    rd-jobs load -f jobs.xml

Output:

    Total Jobs Uploaded: 3 jobs
    Failed to add 1 Jobs:
       3: Build Server1 : Project was not found: north
    Project was not found: north
    Succeeded creating/updating 2  Jobs:
       1: Build and Update Server2 [45] <http://localhost:8080/rundeck/scheduledExecution/show/45>
       2: Deploy Server4 [46] <http://localhost:8080/rundeck/scheduledExecution/show/46>

If the -d skip is specified, then any jobs definitions that were skipped will be listed:

    rd-jobs load -f jobs.xml -d skip

Output:

    Total Jobs Uploaded: 2 jobs
    Skipped 2 Jobs:
       1: Build and Update Server2 [45] <http://localhost:8080/rundeck/scheduledExecution/show/45>
       2: Deploy Server4 [46] <http://localhost:8080/rundeck/scheduledExecution/show/46>

   
# SEE ALSO

`run` (1).

The RunDeck source code and all documentation may be downloaded from
<https://github.com/dtolabs/rundeck/>.
