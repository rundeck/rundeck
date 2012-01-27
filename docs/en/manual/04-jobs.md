% Jobs 
% Alex Honor; Greg Schueler
% November 20, 2010

In previous sections of this manual, you learned how to execute
ad-hoc commands across a filtered set of Node resources. This chapter
introduces a fundamental Rundeck feature, *Jobs*. But first, one might
ask why introduce another layer over ad-hoc command execution. 

Here are some issues that might arise over time:

* One might find certain ad-hoc command executions are repeated, and
  perhaps, represent what has become a routine procedure. 
* Another user in your group needs a simple self-service interface to
  run a procedure across a set of nodes.
* Routine procedures need to be encapsulated and become the basis for
  other routine procedures.  

Jobs provide a means to encapsulate a procedure in a logically
named Job. A *Job* is a configuration representing the steps in a
procedure, a Node filter specification, and dispatcher execution
control parameters. Jobs access is governed by an access control
policy that describes how users are granted authorization to use Jobs.

Rundeck lets you organize and execute Jobs,  and observe the output as
the Job is running. You can view a list of the currently running Jobs
that is dynamically updated as the Jobs progress. Jobs can also be
killed if they need to be stopped.

Each Job has a record of every time it has been executed, and the
output from those executions can be viewed.

The next sections describes how to navigate and run existing Jobs. In
later sections, the topic of Job creation will be covered.

If you want to skip ahead, you can go straight to
[Creating Jobs](jobs.html#creating-jobs).

## Job groups

As many jobs will accumulate over time, it is useful to organize Jobs
into groups. A group is a logical set of jobs, and one job group can
exist inside another. Rundeck displays job lists as a set of folders
corresponding to the group structure your jobs define.

Beyond organizing jobs, groups assist in defining access control
policy, as we'll cover later in the Authorization chapter.

## Job UUIDs

When created, each new job will be assigned a unique UUID.  If you are writing
the Job definition using one of the supported formats you can assign the UUID
yourself.

You can use the UUID to make sure that when you rename or change the group for
your job in your job definition, it will modify the correct job in the server.

The UUID is also useful when porting Job definitions between Rundeck instances.

(Note: Rundeck also assigns each Job an internal "ID" value, although this value is
not portable between Rundeck instances. As of Rundeck 1.3+ the UUID should be used
in lieu of ID.)

## Listing and filtering Jobs

All Job activity begins on the main "Jobs" page inside Rundeck. After
logging in, press the "Jobs" button in the top navigation bar and any
Jobs you are authorized to see will be displayed. 

If the Jobs were defined inside groups, you will see the listing
grouped into a folder like structure. These folders represent the Job
groups described earlier. You can navigate these folders by pressing
the folder icon to reveal its contents. 

Once you have navigated to a Job, you will see its name, possibly its
description and a summary total of how many times it has been executed.

Clicking on the job name will will expand the window to show the Job
detail. You will see a button bar containing icons representing the
actions you are able to perform. Other Job detail will include what
command(s) it will run, filter expressions and other dispatcher options.

### Filtering Jobs

The Job page lets you search for Jobs using the Filter option.

Click the "Filter" link to show the filter options:

![Job filter form](../figures/fig0317.png)

This will show the Filter fields. Enter a value in any of the filter fields:

* Job Name: the name of the job
* Group: the name of the job group
* Description: Job description text

You can type a substring or a regular expression in any of these
fields.

After pressing the "Filter" button, the Job list will be filtered to
include only the matching jobs.

![Job filtered list](../figures/fig0318.png)

To refine the filter, click on the blue-outlined Filter description,
and change the filter fields.

To reset the filter and go back to the full job page, click the
"Clear" button in the Filter fields.

## Viewing Job detail

From a filtered job listing, a Job's detail  can be previewed by hovering 
the mouse pointer over the Job's name. A popup view contains the Job's detail.
Click outside the popup to close it.

![Job detail popup](../figures/fig0320.png)

Pressing the link for the Job name will navigate to  a separate
page where job detail and a button control bar is displayed. These
buttons enable users to delete, copy, edit export or run the Job.

![Job detail page](../figures/fig0321.png)

The buttons displayed on the control bar reflect the
[authorization policy](getting-started.html#authorization) enforced for the user.

The information in the Job detail view includes:

* Name, description and group
* Execution statistics like when the job was last run, it's average success rate, and duration 
* Details including project name and workflow steps, and log level

Pressing the "Show Matches" link will display the list of Nodes where the Job will run.

## Running a Job

Jobs can be run from the shell or from the graphical console.

From the command line, use the [run](../manpages/man1/run.html) shell tool.
Here's an example that starts a hypothetical job named "restart"
belonging in the "apps/web" Job group in project "myproject":

    $ run -j apps/web/restart -p myproject
    Job execution started:
    [51] restart <http://strongbad:4440/execution/follow/51>

From the graphical console, any stored job can be started 
from the Jobs page. Navigate to the desired Job
from the filtered listing and then press the green
"Run" icon to immediately queue and run the Job. 
If you do not see the Run icon, it
means your login does not have "run" privileges.

![Job run button](../figures/fig0319.png)

If you navigated to the Job's detail page, you press
the "Run" button from there.

![Job run button](../figures/fig0319-b.png)

After the Run button has been pressed, a dialog will
open where you can choose execution options.

### Choose execution options

Jobs can be defined to prompt the user for options. This page contains
a form presenting any of these Job options.

Some options will have default values while others may present you
with a menu of choices. Some options are optional while others are
required. Lastly, their might be a pattern governing what values are
acceptable. 

If there are any such Job options, you can change them here before
proceeding with the execution.

When you are ready, press "Run Job Now". The job will enter
the execution queue and you can track its execution in the 
[Now running](rundeck-basics.html#now-running) section.

### Following Running Jobs

Once you have started running a Job, you can follow the Job's output
in the Execution Follow page. 
   
On the Jobs page, look in the "Now running" section
and click the "output >>" link in the row with the desired Job name.
  
If you pressed the "run" button from the Job's detail page, your
browser will already have been directed to the Execution Follow page.
 
## Creating Jobs

Rundeck allows you to define two kinds of Jobs.

* Temporary: A *temporary Job* defines a set of commands to execute and
   a node filter configuration. 
* Saved: A *saved job* also defines a set of commands to execute and
   dispatcher options but can be given a name and stored in a
   group. Additionally, saved Jobs can be given an execution schedule.

From the Jobs, page press the "New Job" button to begin creating a Job.

![New Job button](../figures/fig0301.png)

### Temporary Jobs

A temporary job is a bit like an ad-hoc command except you get more
control over how the commands will execute plus the execution can be
better tracked within the Rundeck webapp.

To create a temporary job, begin by logging in to the Rundeck
graphical console, and press the "Jobs" tab.

1.  Locate the "New Job" button in the right hand corner and press it to display the "Create New Job" form.
1.  A job is defined in terms of one or more workflow steps. In the Workflows area, click the "Add a step" link.
1.  Workflow steps can be one of several types. Click the "Script" workflow step type.
1.  A script type can be any script that can be executed on the target
hosts. Type in the "info" shell script we executed earlier using
dispatch.
1.  At the bottom of the form, push the "Run and Forget" button to begin execution.
1.  Execution output can be followed on the subsequent page.

![Temporary job form](../figures/fig0302.png)

### Saved Jobs

Running ad hoc commands and temporary jobs are a typical part of day
to day administrative work. Occasionally, ad-hoc commands become
routine procedures and if were reusable, would become more valuable. These jobs
could be handed off to others in the team or invoked from within other
Jobs. Rundeck provides an interface to declare and save jobs, both
graphically or declared with an XML file.


### Simple saved job

For the first saved Job example, create a Job that calls the info script.

1.   Like in the earlier example, begin by pressing the "New Job" button.
1.   Within the new job form:
     -   Select "Yes" for the "Save this job?"
     prompt. Pressing Yes reveals a form to define a name, group and
     description for the job. 
     -   For "Job Name", enter "info" and for the "Group", enter
     "adm/resources". 
     -   If you want to specify your own UUID you can enter it in the field. 
     Otherwise a unique value will be set for you.
     -   Providing a description will be come helpful to other users to understand the intent and purpose for the Job.
     -   Check the box for "Dispatch to Nodes"
     -   Choose the "Node Exclude Filters" and enter the name of your Rundeck server. This will cause the job to run on just the remote Nodes (eg., centos54 and ubuntu).
     -   Type in and info script 
     -   Save the Workflow step
     -   Press the "Create" button at the bottom of the page.
     ![Simple saved job form](../figures/fig0303.png)
1.   After the the job is created, the browser is directed to the Jobs page. The folder structure reflecting the group naming will show one Job.
     -    Navigate through the folders buttons to the new job 
1.   Notice the green arrow button.
     -    Press the button to run the Job.
     ![Simple saved job](../figures/fig0304.png)

1.   Press the "Run Job Now" button to begin execution.
     -    The job will be queued and executed. 
1.   Look in the "Now running" section.
     -    Press the "output >>" link to go to the execution follow page.
     ![Simple saved job output](../figures/fig0305.png)

### Multiple Executions

By default, a saved job is "Single Execution": it can only have a single execution running at a time.  This is useful if the steps the Job performs might be interfered with if another separate process was also performing them on the same Node(s).

However, in some cases it is useful to allow a Job to be executed more than once simultaneously.

You can make a job allow "Multiple Executions" by toggling the value to Yes in the Job editor field shown below:

![Multiple executions](../figures/fig-manual-jobs-multiexec.png)

### Node dispatching and filtering

When you create a job you can choose between either running the job only locally (on the Rundeck server), or dispatching it to multiple nodes (including the Rundeck server if you want).

In the GUI, the "Dispatch to Nodes" checkbox lets you enable node dispatching.  When you click this box you are presented with the Node Filtering interface:

![Node Filtering interface](../figures/fig0305-b.png)

You can click the different filter fields "Name", and "Tags" to enter filter values for those fields.  As you update the values you will see the "Matched Nodes" section updated to reflect the list of nodes that will match the inputs.  You can click "More" to see more of the available inclusion filters, and you can click "Extended Filters" to enter
exclusion filters for the same fields.

You can set the maximum number of simultaneous threads to use by changing the "Thread Count" box.  A value of 1 means all node dispatches happen sequentially, and any greater value means that the node dispatches will happen in parallel.

If you set "Keep going on error?" to "Yes", then if any node dispatches fail for any reason, the rest will continue to be executed until all have been executed.  At the end of the workflow for all nodes, the Job Execution will fail if any of the nodes had failed.

If you leave it at the default value of "No", then if any node dispatches fail for any reason, no further dispatches will be executed and the Job Execution will fail immediately.

#### Dynamic node filters

In addition to entering static values that match the nodes, you can also use 
more dynamic values.

If you have defined Options for the Job (see [Job Options](#job-options)), you
can use the values submitted by the user when the job is executed as part of the
node filtering.

Simply set the filter value to `${option.name}`, where "name" is the name of the option.

When the job is executed, the user will be prompted to enter the value of the option, and
that will then be used in the node filter to determine the nodes to dispatch to.

**Note**: Since the dynamic option value is not set yet, the "Matched Nodes" shown in the node filtering input may indicate that there are "None" matched.  Also, when the Job is executed, you may see a message saying "Warning: The Node filters specified for this Job do not match any nodes, execution may fail." The nodes matched will be determined
after the user enters the option values.

## Scheduled Jobs

Saved jobs can be configured to run on a periodic basis. 
If you want to create a Scheduled Job, select Yes under "Schedule to
run repeatedly?"

![Scheduled job simple form](../figures/fig0306.png)

The schedule can be defined in a simple graphical chooser or Unix
crontab format.

To use the simple chooser, choose an hour and minute. You can then
choose "Every Day" (default), or uncheck that option and select
individual days of the week. You can select "Every Month" (default) or
unselect that option and choose specific months of the year:

If the crontab time and date format is preferred, enter a cron
expression.

![Scheduled job crontab form](../figures/fig0307.png)

Use the crontab syntax referenced here: [CronExpression](http://www.quartz-scheduler.org/docs/api/1.8.1/org/quartz/CronExpression.html)

After the Job has been updated to include a schedule, a clock icon
will be displayed when the Job is listed:

![Scheduled job icon](../figures/fig0308.png)

## Job Notifications

You can configure notifications to occur when a Job Execution finishes with either success or failure.

If you want to receive notifications, click Yes under "Send Notification?".

![Notification form](../figures/fig0322.png)

You can enable notifications for either Success or Failure, and either notification by email, or by webhooks.  Click the checkbox next to the type of notification to enable.

![Notifications enabled](../figures/fig0323.png)

Enter either comma-separated email addresses for email notification, or comma-separated URLs for webhook notification.

When the Job finishes executing, all "success" notifications will be triggered if the Job is successful.  Otherwise, all "failure" notifications will be triggered if the Job fails or is cancelled.

### Webhooks

Rundeck Jobs can be configured to POST data to a webhook URL when they succeed or fail.

* For more info about configuring jobs to use webhook notifications, see the chapter [Jobs - Job Notifications](jobs.html#job-notifications).
* For more info about webhooks in general see: <http://webhooks.pbwiki.com/>

When a Rundeck Job webhook notification is triggered, the server will send a POST request to one or more configured URLs.  The request will contain XML content containing information about the Execution that has finished.  The request will also contain special HTTP Headers to include some information about the notification and the Execution.  You can also configure your URLs to have property tokens that will be replaced with specific details about the Job, Execution or Notification prior to the webhook request being submitted.

#### Execution Notification Content

The content of the POST request will be XML, with a single `<notification>` root element.  This element will contain `<executions..><execution>...</execution></executions>` content. This inner content is of the same format as the XML returned from the Web API for Execution information. See the chapter [API - Listing Running Executions](../api/index.html#listing-running-executions) for more information.

Attributes of the `notification` element will include:

`trigger`

:    The type of notification trigger.  Either "success" or "failure".

`executionId`

:    The ID of the Execution

`status`

:    The result status of the Execution.  Either "succeeded", "failed" or "aborted".

*Example*

    <notification trigger="success" executionId="[ID]" status="[STATUS]">
        <executions count="1">
            <execution ...>
                ...
            </execution>
        </executions>
    </notification>

#### Execution Notification Headers

The POST request will also contain several custom HTTP headers, providing another way to receive some of the webhook information:

`X-Rundeck-Notification-Trigger`

:    The notification trigger type, either "success" or "failure".

`X-Rundeck-Notification-Execution-ID`

:    The Execution ID

`X-Rundeck-Notification-Execution-Status`

:    The status of the execution, either "succeeded", "failed", or "aborted".

#### Execution Notification URL Token Expansion

As well, the URLs configured for the webhook notification may contain tokens that will be expanded with values taken from the associated job and execution, such as `${job.name}`.

Available tokens for expansion are:

`job.PROPERTY`

:    Properties about the Job, including:

    `name`
  
     :    the Job name
  
    `group`
  
    :    The Job group, or a blank string
  
    `id`
  
    :    the Job Id
  
    `project`
  
    :    the Project name

`execution.PROPERTY`

:    Properties about the Execution, including:

    `id`
    
    :    The Execution ID

    `user`
    
    :    The user who executed the job

    `status`
    
    :    The execution status, one of "succeeded","failed",or "aborted"

`notification.trigger`

:    The trigger associated with the notification, one of "success" or "failure".

So for example, this URL:

    http://server/callback?id=${execution.id}&status=${execution.status}&trigger=${notification.trigger}

Will have the tokens replaced with the appropriate values prior to making the webhook request.

## Job history

In the Jobs page, you can see the outcome of previous executions of
Jobs by clicking the "Executions" link for the Job.

![Job executions link](../figures/fig0309.png)

This returns a filtered history pertaining to that Job.  You can click on
any past execution in the list to see the full execution state.

![Job executions matches](../figures/fig0310.png)

From the Job detail page, one can also see previous execution history.

## Killing Jobs

Jobs that are currently running can be Killed immediately.

WARNING: This feature should be used with caution, as it forcibly
kills the Java Thread that the Job is running on. It may result in the
Rundeck server becoming flaky. It is a deprecated feature of Java that
is not recommended to be used, so do so only when extremely necessary.

From the History view Now Running section, or in the Job execution
follow page, click on the "Kill Job Now" button for the running Job.

When prompted "Really kill this job?" Click the "Yes" button.

The Job will terminate with a "Killed" completion status.

See also: [rd-queue](../manpages/man1/rd-queue.html).

## Deleting Jobs

In the Job detail page, click the red "X" icon for to delete the Job.

![Job delete button](../figures/fig0311.png)

Click "Yes" when it says "Really delete this Job?"

## Updating and copying Jobs

All of the data you set when creating a job can be modified (except UUID). To edit a
Job, you can click the Pencil icon:

![Job edit button](../figures/fig0312.png)

Similarly, to Copy a Job definition to a new Job, press the Copy button.

![Job copy button](../figures/fig0313.png)

## Exporting Job definitions

Job definitions created inside the Rundeck graphical console can be
exported to XML or YAML file formats and be used for later import. 

Two methods exist to retrieve the Job definitions: via Rundeck's
graphical interface, and via the <code>rd-jobs</code> shell tool.

In the Job detail page, locate the icon with a document symbol in the toolbar. It is labeled
"Download Job definition file"  in the mouse tool tip. Clicking on the icon will let you
choose either XML or YAML format to download the definition.

![Job export button](../figures/fig0314.png)

Click the preferred format to initiate the file download to your
browser. 

If you prefer to use the command line, open a shell on the Rundeck server.
Run the ``rd-jobs`` command to write it to disk. By default,
rd-jobs will dump all Job definitions to one file. To limit it to just
a single Job specify its name with `-n` or its ID with `-i`:

    rd-jobs -p project -n "job-name" -f job.xml

This will store the results in the "job.xml" file.

To export it in YAML format, specify the `-F` option:

    rd-jobs -p project -n "job-name" -F yaml -f job.yaml

This will export in the YAML document format file.

The XML and YAML document formats are described here:

* XML:  [job-v20(5)](../manpages/man5/job-v20.html)
* YAML: [job-yaml-v12(5)](../manpages/man5/job-yaml-v12.html)

Consult the [rd-jobs(1)](../manpages/man1/rd-jobs.html) manual page for additional command usage.

## Importing Job definitions

If you have a "job.xml" file (See above) and want to upload it via
the GUI web interface, you can do so.

Click on the New Job" button in the Job list.

In the "Create New Job" form, click on the button that says "Upload Definition..." on the right side:

![Job import button](../figures/fig0315.png)

Click the Choose File button and choose your job.xml file to upload.

![Job import form](../figures/fig0316.png)

Choose an option where it says "When a job with the same name already
exists:":

* Update - this means that a job defined in the xml will overwrite any
  existing job with the same name  
* Skip - this means that a job defined in the xml will be skipped over
  if there is an existing job with the same name  
* Create - this means that the job defined in the xml will be used to
  create a new job if there is an existing job with the same name.  

Click the Upload button. If there are any errors with the Job
definitions in the XML file, they will show up on the page.  

## Summary

After reading this chapter, you should be familiar with Rundeck Jobs
and able to find and run them. You should understand how to create
temporary and saved jobs and understand how to find their history.
Finally, you should be aware of how to export and import Job
definitions as XML documents.

Next, we'll cover how to create multi-step procedures using Job
Workflows.

