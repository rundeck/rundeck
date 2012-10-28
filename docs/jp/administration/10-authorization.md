% Authorization

Two dimensions of information dictate authorization inside Rundeck:

* *group* memberships assigned to a *user* login.  
* access control policy that grants access to one or more *policy
  action*s to a *group*  or *user*.

The chapter on [Authentication](authentication.html) discusses how to
assign group memberships to users.

The remainder of this section will describe how to use the access
control policy.

*Note from the project team*: The authorization layer is an early work
 in progress. Please share your ideas on the IRC or mailing list.

### Access control policy

Access to running or modifying Jobs is managed in an access control
policy defined using the aclpolicy YAML document. 
This file contains a number of policy elements that describe what user
group is allowed to perform which actions.

Please read over this document for information on how to define it, and how to
grant access for certain actions to certain resources:

*  [aclpolicy-v10(5)](../manpages/man5/aclpolicy-v10.html)

Policies can be organized into more than one file to help organize
access by group or pattern of use. The normal Rundeck install will
have generated a policy for the "admin" group. Not all users will need
to be given "admin" access level to control and modify all Jobs. More
typically, a group of users will be given access to just a subset of
Jobs.

File listing: admin.aclpolicy example

    description: Admin project level access control. Applies to resources within a specific project.
    context:
      project: '.*' # all projects
    for:
      resource:
        - equals:
            kind: job
          allow: [create] # allow create jobs
        - equals:
            kind: node
          allow: [read,create,update,refresh] # allow refresh node sources
        - equals:
            kind: event
          allow: [read,create] # allow read/create events
      adhoc:
        - allow: [read,run,kill] # allow running/killing adhoc jobs
      job: 
        - allow: [read,update,delete,run,kill] # allow read/write/delete/run/kill of all jobs
      node:
        - allow: [read,run] # allow read/run for all nodes
    by:
      group: admin
    
    ---
    
    description: Admin Application level access control, applies to creating/deleting projects, admin of user profiles, viewing projects and reading system information.
    context:
      application: 'rundeck'
    for:
      resource:
        - equals:
            kind: project
          allow: [create] # allow create of projects
        - equals:
            kind: system
          allow: [read] # allow read of system info
        - equals:
            kind: user
          allow: [admin] # allow modify user profiles
      project:
        - match:
            name: '.*'
          allow: [read,admin] # allow view/admin of all projects
    by:
      group: admin

The example policy document above demonstrates the access granted to
the users in group "admin".

Two separate policies define two levels of access control.  The first is the "project"
context, which allows access to actions on resources within a specific project.
The second is the "application" level context, which allows access to things 
like creating projects, access to projects, managing users, and access to system
information.

### Specific Resources and Resource Types

As described in the [aclpolicy-v10(5)](../manpages/man5/aclpolicy-v10.html) definition, access
is granted or denied to specific "resources". Resources can take two forms:

* A specific resource, with a type and properties
* Resource types, which applies to all resources of a specific type or "kind"

For example, you might want to restrict access to a job or jobs within a certain 
group. This corresponds to specific "job" resources with a "group" property
matching a certain pattern.

You might also want to restrict who can create *new* jobs. Since a new job does 
not exist yet, you cannot create a rule for this action to apply to an existing 
job.  Which means this corresponds to a generic resource with a "kind" called "job".

### Special API Token Authentication group

Clients of the [Web API](../api/index.html) may use the [Token Authentication](../api/index.html#token-authentication) method.  These clients are
placed in the special authorization group called `api_token_group`.

`api_token_group`
~   Special role given to all [API Token](../api/index.html#token-authentication) authenticated access.

### Rundeck resource authorizations

Rundeck declares a number of actions that can be referenced inside the access control policy document.

The actions and resources are divided into project scope and application scope:

#### Application Scope Resources and Actions

You define application scope rules in the aclpolicy, by declaring this context:

    context:
      application: 'rundeck'

These are the Application scope actions that can be allowed or denied via the
aclpolicy:

* Creating Projects ('create' action on a resource type with kind 'project')
* Reading system information ('read' action on a resource type with kind 'project')
* Administering user profiles ('admin' action on a resource type of kind 'user')
* Reading specific projects ('read' action on a project with a specific name)
* Administering specific projects ('admin' action on a project with a specific name

The following table summarizes the generic and specific resources and the 
actions you can restrict in the application scope:

Type       Resource Kind     Properties   Actions  Description
------     --------------    -----------  -------- ------------
`resource` `project`         none         `create` Create a new project
`resource` `system`          none         `read`   Read system information
`resource` `user`            none         `admin`  Modify user profiles
----------------------------

Table: Application scope generic type actions

Type      Properties   Actions  Description
-----     -----------  -------- ------------
`project` "name"       `read`   View a project in the project list
`project` "name"       `admin`  Modify project configuration
----------------------------

Table: Application scope specific resource actions


#### Project Scope Resources and Actions

You define project scope rules in the aclpolicy by declaring this context:

    context:
      project: "(regex)"

The regex can match all projects using ".*", or you can simply put the project
name.

Note that for projects not matched by an aclpolicy, *no* actions will be granted
to users.

Also note that to hide projects completely from users, you would need to grant
or deny the "read" access to the project in the [Application Scope](authorization.html#application-scope-resources-and-actions).

These are the Application scope actions that can be allowed or denied via the
aclpolicy:

* Create Jobs ('create' action on a resource type with kind 'job')
* Read Node data ('read' action on a resource type with kind 'node')
* Update/Refresh node data ('create','update','refresh' action on a resource type with kind 'node')
* Read history events ('read' action on a resource type with kind 'event')
* Create history events ('create' action on a resource type with kind 'event')
* Run adhoc jobs ('run' action on 'adhoc' resources)
* Kill adhoc jobs ('kill' action on 'adhoc' resources)
* Any Action on Jobs (actions on 'job' resources, see below)

The following table summarizes the generic and specific resources and the 
actions you can restrict in the project scope:

Type       Resource Kind     Actions   Description
------     --------------    --------  ------------
`resource` `job`             `create`  Create a new Job
"          `node`            `read`    Read node information
"          "                 `create`  Create new node entries
"          "                 `update`  Modify node entries
"          "                 `refresh` Refresh node entry from a URL
"          `event`           `read`    Read history event information
"          "                 `create`  Create arbitrary history event entries
----------------------------

Table: Project scope generic type actions

Type      Properties                         Actions  Description
-----     -----------                        -------- ------------
`adhoc`                                      `read`   Read adhoc execution output
"                                            `run`    Run an adhoc execution
"                                            `kill`   Kill an adhoc execution
`job`     "name","group"                     `read`   View a Job and its executions
"                                            `update` Modify a job
"                                            `delete` Delete a job
"                                            `run`    Run a job
"                                            `kill`   Kill a running job
"                                            `create` Create the matching job
`node`    "rundeck_server", "nodename", ...  `read`   View the node in the UI (see [Node resource properties](authorization.html#node-resource-properties))
"                                            `run`    Run jobs/adhoc on the node
----------------------------

Table: Project scope specific resource actions

Note: see [Node resource properties](authorization.html#node-resource-properties) for more node resource properties for authorization.

Recall that defining rules for a resource type is done in this way:

    for:
      resource:
        - equals:
            kind: 'project'
          allow: [create]

Whereas defining rules for specific resources of a certain type is done in this
way:

    for:
      job:
        - equals:
            name: bob
          allow: [run]

#### Node resource properties

The properties available are the attributes that are defined on the node, so you can apply authorizations based on tag, osName, hostname, etc. The special `rundeck_server` property will be set to "true" for the Rundeck server node only, and "false" for all other nodes.

Any custom attributes can be used as well.

Name             Description
-----            -----------
`nodename`       Name of the node
`username`       Authentication username
`hostname`       Hostname of the node
`description`    Description of the node
`tags`           Set of tags.  Can use with the `contains:` filter.
`osName`         Operating System name
`osFamily`       Operating System family, e.g. "unix" or "windows"
`osVersion`      Operating System version
`osArch`         Operating System architecture
`rundeck_server` A value set to "true" if the node is the Rundeck server node
----------------------------

Table: Pre-defined Node resource properties for authorization filters

#### Access control policy actions example

Below is an example policy document demonstrating policy actions
to create limited access for a group of users.
Users in the group "restart_user", are allowed to run three jobs in the "adm"
group, Restart, stop and start. By allowing `run` but not `read`,
the "stop" and "start" jobs will not be visible.

File listing: restart_user.aclpolicy example

    description: Limited user access for adm restart action
    context:
      project: '.*'
    for:
      job:
        - equals:
            group: 'adm'
            name: 'Restart'
          allow: [run,read]
        - equals:
            group: 'adm'
            name: 'stop'
          allow: [run]
        - equals:
            group: 'adm'
            name: 'start'
          allow: [run]
    by:
      group: [restart_user]

### Troubleshooting access control policy

After defining an aclpolicy file to grant access to a particular group
of users, you may find them getting "unauthorized" messages or
complaints that certain actions are not possible. 

To diagnose this, begin by checking two bits:

1. The user's group membership. This can be done by going to the
   user's profile page in Rundeck. That page will list the groups the
   user is a member. 
2. Read the messages inside the `rundeck.audit.log` log file. The
   authorization facility generates fairly low level messages describing
   how the policy is matched to the user context.

For each entry in the audit log, you'll see all decisions leading up to either a
AUTHORIZED or a REJECTED message.  It's not uncommon to see REJECTED
messages followed by AUTHORIZED.  The important thing is to look at
the last decision made.

### Authorization caveats

* aclpolicy changes do not require a restart.
