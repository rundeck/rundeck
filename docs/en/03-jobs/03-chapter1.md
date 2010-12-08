% RUNDECK(1) RunDeck User Manuals | Version 1.0
% Alex Honor
% November 20, 2010

# Jobs 

In previous sections of this manual, you learned how to execute
ad-hoc commands across a filtered set of Node resources. This chapter
introduces a fundamental RunDeck feature, *Jobs*. But first, one might
ask why introduce another layer over ad-hoc command execution. 

Here are some issues that might arise over time:

* One might find certain ad-hoc command executions are repeated, and
  perhaps, represent what has become a routine procedure. 
* Another user in your group needs a simple self-service interface to
  run a procedure across a set of nodes.
* Routine procedures need to be encapsulated and be the basis for
  other routine procedures.  

Jobs provide a means to encapsulate a procedure in a logically
named Job. A *Job* is a configuration representing the steps in a
procedure, a Node filter specification, and dispatcher execution
control parameters. Jobs access is governed by an access control
policy that describes how users are granted authorization to use Jobs.

RunDeck lets you organize and execute Jobs,  and observe the output as
the Job is running. You can view a list of the currently running Jobs
that is dynamically updated as the Jobs progress. Jobs can also be
killed if they need to be stopped.

Each Job has a record of every time it has been executed, and the
output from those executions can be viewed.

The next sections describes how to navigate and run existing Jobs. In
later sections, the topic of Job creation will be covered.

If you want to skip ahead, you can go straight to
[Creating Jobs](#creating-jobs).

## Job groups

As many jobs will accumulate over time, it is useful to organize Jobs
into groups. A group is a logical set of jobs, and one job group can
exist inside another. RunDeck displays job lists as a set of folders
corresponding to the group structure your jobs define.

Beyond organizing jobs, groups assist in defining access control
policy, as we'll cover later in the Authorization chapter.

## Listing and filtering Jobs

All Job activity begins on the main "Jobs" page inside RunDeck. After
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

![Job filter form](figures/fig0317.png)

This will show the Filter fields:

Enter a value in any of the filter fields:

* Job Name: the name of the job
* Group: the name of the job group
* Description: Job description text

You can type a substring or a regular expression in any of these
fields.

After pressing the "Filter" button, the Job list will be filtered to
include only the matching jobs.

![Job filtered list](figures/fig0318.png)

To refine the filter, click on the blue-outlined Filter description,
and change the filter fields.

To reset the filter and go back to the full job page, click the
"Clear" button in the Filter fields.


## Running a Job

Any stored job can be started from the Job page by pressing the green
"Run" icon in the Job control bar. If you do not see the Run icon, it
means your login does not have "run" privileges.

![Job run button](figures/fig0319.png)

Jobs can also be started from the command line using the
<code>run</code> shell tool.

Here's an example that starts a hypothetical job named "restart"
belonging in the "apps/web" Job group:

    $ run -j apps/web/restart
    Job execution started:
    [51] restart <http://strongbad:4440/execution/follow/51>

After the Run button has been pressed the page will be directed to
choose execution options.

### Choose execution options

Jobs can be defined to prompt the user for options. This page contains
a form presenting any of these Job options.

Some options will have default values while others may present you
with a menu of choices. Some options are optional while others are
required. Lastly, their might be a pattern govering what values are
acceptable. 

If there are any such Job options, you can change them here before
proceeding with the execution.

When you are ready press "Run Job Now" page and you will be directed
to page where you can follow the progress of the Job. You can press
the "Cancel" button 

### Following Running Jobs
Once you have started running a Job, you can follow the output of the
job in the Execution Follow page. 

Depending where you are in the RunDeck console, you can track a
running Job starting from several locations:

* If you have just pressed the Run button for a Job and chose its
  execution options and pressed "Run Job Now" you will automatically
  be directed to this page.
   
* From the Jobs page, you can click to the Job you are interested in
  tracking and click the spinning cursor icon labeled "now".

* From the History page, open the "Now Running" area adn then click
  on the "output »" link for the running  execution.  
  
  
## Creating Jobs

With RunDeck you can define two kinds of Jobs.

* Temporary: A temporary Job defines a set of commands to execute and
  a node filter configuration. 
* Saved: Saved jobs also define a set of commands to execute and
  dispatcher options but can be given a name and stored in a
  group. Additionally, saved Jobs can be given an execution schedule.

From the Jobs, page press the "New Job" button to begin creating a Job.

![New Job button](figures/fig0301.png)

### Temporary Jobs

A temporary job is a bit like an ad-hoc command except you get more
controls about how the commands will execute plus the execution can be
tracked within the RunDeck webapp.

To create a temporary job, begin by logging in to the RunDeck
graphical console, and press the "Jobs" tab.

1.  Locate the "New Job" button in the right hand corner and press it to display the "Create New Job" form.
1.  A job is defined in terms of one or more workflow steps. In the Workflows area, click the "Add a step" link.
1.  Workflow steps can be one of several types. Click the "Script" workflow step type.
1.  A script type can be any script that can be executed on the target
hosts. Type in the "info" shell script we executed earlier using
dispatch.
1.  At the bottom of the form, push the "Run and Forget" button to begin execution.
1.  Execution output can be followed on the subsequent page.

![Temporary job form](figures/fig0302.png)

### Saved Jobs

Running ad hoc commands and temporary jobs are a typical part of day
to day administrative tasks. Occasionally, ad-hoc commands become
routine procedures and if were reusable, would become valuable as they
could be handed off to others in the team or invoked from within other
Jobs. RunDeck provides an interface to declare and save jobs, both
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
     -   Providing a description will be come helpful to other users to understand the intent and purpose for the Job.
     -   Check the box for "Dispatch to Nodes"
     -   Choose the "Node Exclude Filters" and enter the name of your RunDeck server. This will cause the job to run on just the remote Nodes (eg., centos54 and ubuntu).
     -   Type in and info script 
     -   Save the Workflow step
     -   Press the "Create" button at the bottom of the page.
     ![Simple saved job form](figures/fig0303.png)
1.   After the the job is created, the browser is directed to the Jobs page. The folder structure reflecting the group naming will show one Job.
     -    Press through the folders and then to the job itself
1.   Notice the button bar with controls for editing and running the
job.
     -    Press the green arrow icon to run the Job.
     ![Simple saved job](figures/fig0304.png)

1.   Press the "Run Job Now" button to begin execution.
     -    Output from the script execution from the target Nodes will be displayed on the subsequent page.
     ![Simple saved job output](figures/fig0305.png)


## Scheduled Jobs

Saved jobs can be configured to run on a periodic basis. 
If you want to create a Scheduled Job, select Yes under "Schedule to
run repeatedly?"

![Scheduled job simple form](figures/fig0306.png)

The schedule can be defined in a simple graphical chooser or Unix
crontab format.

To use the simple chooser, choose an hour and minute. You can then
choose "Every Day" (default), or uncheck that option and select
individual days of the week. You can select "Every Month" (default) or
unselect that option and choose specific months of the year:

If the crontab time and date format is preferred, enter a cron
expression.

![Scheduled job crontab form](figures/fig0307.png)

Use the crontab syntax referenced here: [CronExpression](http://www.quartz-scheduler.org/docs/api/1.8.1/org/quartz/CronExpression.html)

After the Job has been updated to include a schedule, a clock icon
will be displayed when the Job is listed:

![Scheduled job icon](figures/fig0308.png)

## Job history

In the Jobs page, you can see the outcome of previous executions of
Jobs by clicking the "Executions" link for the Job.

![Job executions link](figures/fig0309.png)

This returns a filtered history peraining to that Job.  You can click on
any past execution in the list to see the full execution state.

![Job executions matches](figures/fig0310.png)

## Killing Jobs

Jobs that are currently running can be Killed immediately.

WARNING: This feature should be used with caution, as it forcibly
kills the Java Thread that the Job is running on. It may result in the
RunDeck server becoming flaky. It is a deprecated feature of Java that
is not recommended to be used, so do so only when extremely necessary.

From the History view Now Running section, or in the Job execution
follow page, click on the "Kill Job Now" button for the running Job.

When prompted "Really kill this job?" Click the "Yes" button.

The Job will terminate with a "Killed" completion status.

## Deleting Jobs

In the Jobs, click the red "X" icon for the Job you want to delete.

![Job delete button](figures/fig0311.png)

Click "Yes" when it says "Really delete this Job?"

## Updating and copying Jobs

All of the data you set when creating a job can be modified. To edit a
Job, you can click the Pencil icon in the Job list:

![Job edit button](figures/fig0312.png)

Similarly, to Copy a Job definition to a new Job, choose the Copy icon
or the Copy button.

![Job copy button](figures/fig0313.png)

## Exporting Jobs as XML

Job definitions created inside the RunDeck graphical console can be
exported to an XML file format and be used for later import. 

Two methods exist to retrieve the XML definition one inside RunDeck's
graphical interface and the other using the <code>rd-jobs</code> shell tool.

From RunDeck's Job page navigate to the Job you wish to export. 
Locate the icon with an XML symbol in the toolbar. It is labeled
"Download XML"  in the mouse tool tip. 

![Job export button](figures/fig0314.png)

Press this button to initiate the file download to your
browser. Depending on your browser, it will be stored in your
downloads directory.

If you prefer the command line open a shell on the RunDeck server.
Run the <code>rd-jobs</code> command to write it to disk. By default,
rd-jobs will dump all Job definitions to one file. To limit it to just
a single Job specify its name:

    rd-jobs -n "job-name" -f job.xml

This will store the results in the "job.xml" file (job-v20(5)).

Consult the "rd-jobs(1)" manual page for additional command usage.

## Importing Jobs as XML

If you have a "job.xml" file (See above) and want to upload it via
the GUI web interface, you can do so.

Click on the New Job" button in the Job list.

In the "Create New Job" form, click on the button that says "Uplaod Definition..." on the right side:

![Job import button](figures/fig0315.png)

Click the Choose File button and choose your job.xml file to upload.

![Job import form](figures/fig0316.png)

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

After reading this chapter, you should be familiar with RunDeck Jobs
and able to find and run them. You should understand how to create
temporary and saved jobs and understand how to find thier history.
Finally, you should be aware of how to export and import Job
definitions as XML documents.

Next, we'll cover how to create multi-step procedures using Job
Workflows.

