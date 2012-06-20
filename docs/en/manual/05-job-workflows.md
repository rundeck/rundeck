% Job Workflows
% Alex Honor; Greg Schueler
% November 20, 2010

The Job's most basic feature is its ability to execute one or more
commands across a set of nodes. This sequence of commands is called a
_workflow_, and each step in the workflow is defined as an invocation
to a command. 

The steps of the Job workflow are displayed when viewing a Job's
detail from a Job listing or within the Job editor form.

## Workflow definition

Workflows can be defined within the Rundeck graphical console or as an
XML or YAML document that is loaded to the server.

The graphical console provides an authoring environment where steps
can be added, edited, removed or reordered.

Users preferring to define Jobs in a text format should refer to the two format definitions:

* XML:  [job-v20(5)](../manpages/man5/job-v20.html)
* YAML: [job-yaml-v12(5)](../manpages/man5/job-yaml-v12.html)

It is also possible to author Jobs inside the graphical console
and then export the definition as a file using the
`rd-jobs` shell tool ([rd-jobs(1)](../manpages/man1/rd-jobs.html)).

See [Exporting Job definitions][1] and [Importing Job definitions][2].

[1]: #exporting-job-definitions
[2]: #importing-job-definitions

## Workflow control settings

Workflow execution is controlled by two important settings: *Keepgoing*
and *Strategy*.

![Workflow controls](../figures/fig0401.png)

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

![Add a step](../figures/fig0402.png)

To add new steps simply press the "Add a step" link inside the workflow
editor form. This will prompt you with a dialog asking which kind of
workflow step you would like to add. Each kind of step has its own
form. When you are done filling out the form, press "Save" to add it
to the sequence. Pressing "Cancel" will close the form and leave the
sequence unchanged.

![Workflow step types](../figures/fig0403.png)

New steps are always added to the end of the sequence. See
[Reordering steps](job-workflows.html#reordering-steps) for directions on modifying the
step order.

The next several sections describe the specification of each kind of
command step.

### Command step

Use the command step to call system commands. This is the default type
of workflow step when creating a Job. Enter any command string you
would type at the terminal on the remote hosts.

![Command step type](../figures/fig0404.png)

This is similar to calling the command with <code>dispatch</code>:

    dispatch [filter-options] -- command

### Script step

Execute the supplied shell script content. Optionally, can pass an
argument to the script specified in the lower text field.

![Script step type](../figures/fig0405.png)

This is similar to calling the command with <code>dispatch</code>:

    dispatch [filter-options] --stdin -- args <<EOF 
    script content here 
    EOF

### Script file step

Executes the script file local to the sever to the filtered Node
set. Arguments can be passed to the script by specifying them in the
lower text field.

![Script file step type](../figures/fig0406.png)


This is similar to calling the script file with <code>dispatch</code>:

    dispatch [filter-options] -s scriptfile -- args

### Script URL step

Downloads a script from a URL, and executes it to the filtered Node
set. Arguments can be passed to the script by specifying them in the
lower text field.

![Script URL step type](../figures/fig0406.png)


This is similar to calling the script URL with <code>dispatch</code>:

    dispatch [filter-options] -u URL -- args

The URL can contain [Context Variables](#context-variables) that will be expanded at runtime.

### Job reference step

To call another saved Job, create a Job Reference step. Enter the name
of the Job and its group. 

![Job step type](../figures/fig0407.png)

The Job Reference form provides a Job browser to make it easier to
select from the existing set of saved Jobs. 
Click the "Choose A Job..." link and navigate to the desired Job.

Finally, if the Job defines Options, you can specify them in the
commandline arguments text field and can include variable expansion to pass
any input options for the current job.  Format:

    -optname <value> -optname <value> ...

The format for specifying options is exactly the same as you would pass to the `run` commandline tool, and you can substitute values of input options to the current job. For example:

    -opt1 something -opt2 ${option.opt2}

This would set the value "something" for the Job's "opt1" option, and then pass
the "opt2" option directly from the top-level job to the Job reference.

This is similar to calling the other Job with [run](../manpages/man1/run.html):

    run [filter-options] -j group/jobname -- -opt1 something -opt2 somethingelse

If the Job has required Options that are not specified on the arguments line,
then a "defaultValue" of that option will be used if it is defined.  If a
required option does not have a default value, then the execution will fail
because the option is not specified.

## Reordering steps

The order of the Workflow steps can be modified by hovering over any
step and then clicking and dragging the double arrow icon to the
desired position. A blue horizontal bar helps highlight the position
where the Job will land.

![Job step reorder](../figures/fig0408.png)

After releasing the select Job, it will land in the desired position
and the step order will be updated.

If you wish to Undo the step reordering, press the "Undo" link above
the steps. 

The "Redo" button can be pressed to reapply the last undone change.

Press the "Revert All Changes" button to go back to the original step order.

## Error Handlers

Each step in a Workflow can have an associated "Error Handler" action.  This handler
is a secondary step of any of the available types that will execute if the Workflow
step fails. Error Handler steps can be used to recover the workflow from failure, or
simply to execute a secondary action.

This provides a few different ways to deal with a step's failure:

* Print additional information about a failure
* Roll back a change
* Recover the workflow from failure, and continue normally

When a Workflow step has a failure, the behavior depends on whether it has a Error Handler or not,
and the value of the "keepgoing" setting for the Workflow.

* When a step fails **without an Error Handler**
    1. the Workflow is marked as "failed"
    2. If `keepgoing="false"`
        1. then the entire Workflow stops
    3. Otherwise, the remaining Workflow steps are executed in order
    4. the Workflow ends with a "failed" result status
    
If you define an Error Handler for a step, then the behavior changes. If keepgoing
is "true", then the handler can recover the step's failure by executing successfully.

* When a step fails **with an Error Handler**
    1. the Error Handler is executed
    2. If `keepgoing="false"`
        1. the Workflow is marked as "failed"
        2. then the entire Workflow stops
    3. Otherwise if `keepgoing="true"`
        1. if the Error Handler failed, the Workflow is marked as "failed"
        2. Otherwise, the Workflow is *not* additionally marked
    1. the remaining Workflow steps are executed in order (including other triggered Error Handlers)
    3. when the Workflow ends, its status depends on if it is marked

Essentially, the result status of the Error Handler becomes the result status of its Step, if the Workflow 
has `keepgoing="true"`. If the Error Handler succeeds, then the step is not considered to have failed. This 
includes scripts, commands, job references, etc. (Scripts and commands must have an exit status of `0` to 
return success.)

Note that when `keepgoing="false"`, the Workflow will always stop and end with a "failed" status.

It is a good practice, when you are defining Error Handlers, to **always** have them fail (e.g. scripts/commands return a non-zero exit-code), unless you specifically want them to be used for Recovery.

## Save the changes

Once the Workflow steps have been defined and order, changes are
permanently saved after pressing the "Create" button if new or the
"Update" button if the Job is being modified.

## Context Variables

When a Job step is executed, it has a set of "context" variables that you can access in the Job step. There are several sets of context variables, including: the Job context `job`, the Node context `node`, and the Option context `option`.

Job context variables:

* `job.name`: Name of the Job
* `job.group`: Group of the Job
* `job.id`: ID of the Job
* `job.execid`: ID of the current Execution
* `job.username`: Username of the user executing the Job
* `job.project`: Project name

Node context variables:

* `node.name`: Name of the Node being executed on
* `node.hostname`: Hostname of the Node
* `node.username`: Usernae of the remote user
* `node.description`: Description of the node
* `node.tags`: Comma-separated list of tags
* `node.os-*`: OS properties of the Node: `name`,`version`,`arch`,`family`
* `node.*`: All Node attributes defined on the Node.

Option context variables are referred to as `option.NAME` (more about [Job Options](job-options.html) in the next chapter.)

### Context Variable Usage

Context variables can be used in a few ways in a Job step, with slightly different sytanxes:

* Commands, Script Arguments and Job Reference Arguments

    :     `${ctx.name}`

* Inline Script Content (*see note*)

    :     `@ctx.name@`

    **Note**: The "Inline Script Content" variable expansion is **not** available for "Script File" steps.  The Script File is not rewritten at all when used for execution.

* Environment Variables (*see note*)

    :     `$RD_CTX_NAME`

    The syntax for Environment variables is that all letters become uppercase, punctuation is replaced with underscore, and the name is prefixed with `RD_`.

    **Note**: See the chapter [Administration - SSH - Passing Environment Variables Through Remote Commands](../administration/ssh.html#passing-environment-variables-through-remote-command) for information about requirements of the SSH server.

## Summary

At this point you should understand what a Job workflow is, the kinds
of steps they can contain and how to define a workflow.

Next, we'll cover more about Rundeck's Job Option features.
