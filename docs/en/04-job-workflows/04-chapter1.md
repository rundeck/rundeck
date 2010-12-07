# Job Workflows

The Job's most basic feature is its ability to execute one or more
commands across a set of nodes. This sequence of commands is called a
_workflow_, and each step in the workflow is defined as an invocation
to a command. 

The steps of the Job workflow are displayed when viewing a Job's
detail from a Job listing or within the Job editor form.

## Workflow definition

Workflows can be defined within the RunDeck graphical console or as an
XML document that is loaded to the server.

The graphical console provides an authoring environment where steps
can be added, edited, removed or reordered.

Users prefering to define Jobs in XML should read the "job-v20(5)"
manual page. 

It is also possible to author Jobs inside the graphical console
and then export the definiton as an XML file using the
<code>rd-jobs</code> shell tool (man "rd-jobs(1)").

## Workflow control settings

Workflow execution is controlled by two important settings: *Keepgoing*
and *Strategy*.

*Keepgoing*: This manages what to do if a step incurs and error:

*   No: Fail immediately (default)
*   Yes: Continue to next step

The default is to fail immediately but depending on the procedure at
hand you can choose to have the execution continue.

*Strategy*: Controls the order of execution of steps and command
dispatch to nodes: *Node-oriented* and *Step-oriented*.

*   *Node-oriented*: Executes the full workflow on each node before the
    next node. (default)
*   *Step-oriented*: Executes each step on all nodes before the next
     node.

The following illustrations contrast the strategies showing how three
steps proceed across two nodes.

Node-oriented flow illustrated:

~~~~~~~~~~~~~~~~~~~~~
1.   NodeA    step#1
2.     "      step#2
3.     "      step#3
4.   NodeB    step#1
5.     "      step#2
6.     "      step#3
~~~~~~~~~~~~~~~~~~~~~

Step-oriented flow illustrated:

~~~~~~~~~~~~~~~~~~~~~
1.   NodeA    step#1
2.   NodeB      "
3.   NodeA    step#2
4.   NodeB      "
5.   NodeA    step#1
6.   NodeB      "
~~~~~~~~~~~~~~~~~~~~~

The process you are automating will determine which strategy is
correct, though the node-oriented flow is more commonplace.

## Workflow steps

The following sections describe how to construct a workflow as a set
of steps that call commands of different types.

When creating a new Job definition, the Workflow form will be set with
defaults and have no workflow steps defined. The workflow editor will
have a form open asking to enter a shell command as the first step. 

To add new steps simply press the "Add a step" link inside the workflow
editor form. This will prompt you with a dialog asking which kind of
workflow step you would like to add. Each kind of step has its own
form. When you are done filling out the form, press "Save" to add it
to the sequence. Pressing "Cancel" will close the form and leave the
sequence unchanged.

New steps are always added to the end of the sequence. See
[Reordering steps](#reordering-steps) for directions on modifying the
step order.

The next several sections describe the specification of each kind of
command step.

### Command step

Use the command step to call system commands. This is the default type
of workflow step when creating a Job. Enter any command string you
would type at the terminal on the remote hosts.

This is similar to calling the command with <code>dispatch</code>:

    dispatch [filter-options] -- command

### Script step

Execute the supplied shell script content. Optionally, can pass an
argument to the script specified in the lower text field.

This is similar to calling the command with <code>dispatch</code>:

    dispatch [filter-options] --stdin -- args <<EOF 
    script content here 
    EOF

### Script file step

Executes the script file local to the sever to the filtered Node
set. Arguments can be passed to the script by specifying them in the
lower text field.

This is similar to calling the script file with <code>dispatch</code>:

    dispatch [filter-options] -s scriptfile -- args

### Job reference step

To call another saved Job, create a Job Reference step. Enter the name
of the Job and its group. 

The Job Reference form provides a Job browser to make it easier to
select from the existing set of saved Jobs. 
Click the "Choose A Job..." link and navigate to the desired Job.

Finally, if the Job defines Options, you can specify them in the
commandline arguments text field.

This is simililar to calling the other Job with <code>run</code>:

    run [filter-options] -j group/jobname
    
## Reordering steps

The order of the Workflow steps can be modified by hovering over any
step and then clicking and dragging the double arrow icon to the
desired position. A blue horizontal bar helps highlight the position
where the Job will land.

After releasing the select Job, it will land in the desired position
and the step order will be updated.

If you wish to Undo the step reordering, press the "Undo" link above
the steps. 

The "Redo" button can be pressed to reapply the last undone change.

Press the "Revert All Changes" button to go back to the original step order.

## Save the changes

Once the Workflow steps have been defined and order, changes are
permanently saved after pressing the "Create" button if new or the
"Update" button if the Job is being modified.

## Summary

At this point you should understand what a Job workflow is, the kinds
of steps they can contain and how to define a workflow.

Next, we'll cover more about RunDeck's Job Option features.
