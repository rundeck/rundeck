% Access Control Policy

Based on the [Authentication](authenticating-users.html) mechanism,
the Container provides Rundeck
with a list of "group" or "role" names
that the user belongs to.
Rundeck uses this list to determine what access rights the user has.
(For more about the role list,
refer to [Authenticating Users - Container authentication and authorization][1].)

[1]: authenticating-users.html#container-authentication-and-authorization


A Rundeck *access control policy* grants users
and user groups certain
privileges to perform actions against rundeck resources
like projects, jobs, nodes, commands and API.
Every action requested by a user is evaluated by the
Rundeck authorization system and logged for
reporting and auditing purposes.

Since Rundeck respects the policy definition, you can define role-based
authorization to restrict users to only a subset of actions. This
enables a self-service type interface, where some users have
access to a limited set of executable actions.


Two dimensions of information dictate authorization inside Rundeck:

* *group* memberships assigned to a *user* login.  
* access control policy that grants access to one or more *policy
  action*s to a *group*  or *user*.

The remainder of this section will describe how to use the access
control policy.

## Access control policy

Access to running or modifying Jobs is managed in an access control
policy defined using the aclpolicy YAML document.
This file contains a number of policy elements that describe what user
group is allowed to perform which actions.

Please read over this document for information on how to define it, and how to
grant access for certain actions to certain resources:

*  [aclpolicy](../man5/aclpolicy.html)

Policies can be organized into more than one file to help organize
access by group or pattern of use. The normal Rundeck install will
have generated a policy for the "admin" group. Not all users will need
to be given "admin" access level to control and modify all Jobs. More
typically, a group of users will be given access to just a subset of
Jobs.

### Policy File Locations

Rundeck loads ACL Policy definitions from these locations:

* All `*.aclpolicy` files found in the rundeck `etc` dir, which is either `/etc/rundeck` (rpm and debian install defaults),
or `$RDECK_BASE/etc` (launcher/war configuration).
* System level policies created via the [System ACLs API](../api/index.html#acls)
* Project level policies created via the [Project ACLs API](../api/index.html#project-acls), limited only to project context policies for a specific project.



### Lifecycle

The Rundeck server does not need to be restarted for changes to aclpolicy files to take effect.

The files are loaded at startup and are cached.
When an authorization request occurs, the policies may be reloaded if the file was modified.
A file's contents are cached for at least 60 seconds before checking if they need to be reloaded.
Also, the etc directory is only re-scanned for new/removed files after a 60 second delay.

If an authorization request occurs in the context of a specific Project
(e.g. "does a user have Run access for a specific Job in this project?")
then the Project-level policies created via the API area also used to evaluate the authorization request.

Otherwise, only the policies on the filesystem, and uploaded ot the System ACLs API are evaluated for the request.

### rd-acl

Added in Rundeck 2.5, the [rd-acl](../man1/rd-acl.html) tool
can help to create, test, and validate your policy files.

### Example

File listing: admin.aclpolicy example

~~~~~~ {.yaml .numberLines}
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
    - allow: [read,run,runAs,kill,killAs] # allow running/killing adhoc jobs
  job:
    - allow: [create,read,update,delete,run,runAs,kill,killAs] # allow create/read/write/delete/run/kill of all jobs
  node:
    - allow: [read,run] # allow read/run for nodes
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
      allow: [read,enable_executions,disable_executions,admin] # allow read of system info, enable/disable all executions
    - equals:
        kind: system_acl
      allow: [read,create,update,delete,admin] # allow modifying system ACL files
    - equals:
        kind: user
      allow: [admin] # allow modify user profiles
  project:
    - match:
        name: '.*'
      allow: [read,import,export,configure,delete,admin] # allow full access of all projects or use 'admin'
  project_acl:
    - match:
        name: '.*'
      allow: [read,create,update,delete,admin] # allow modifying project-specific ACL files
  storage:
    - allow: [read,create,update,delete] # allow access for /ssh-key/* storage content

by:
  group: admin
~~~~~~~~~~

The example policy document above demonstrates the access granted to
the users in group "admin".

Both `username` and `group` can use regular expressions to match multiple users or groups.

Two separate policies define two levels of access control.  The first is the "project"
context, which allows access to actions on resources within a specific project.
The second is the "application" level context, which allows access to things
like creating projects, access to projects, managing users, and access to system
information.

## Specific Resources and Resource Types

As described in the [aclpolicy-v10(5)](../man5/aclpolicy.html) definition, access
is granted or denied to specific "resources". Resources can take two forms:

* A specific resource, with a type and properties
* Resource types, which applies to all resources of a specific type or "kind"

For example, you might want to restrict access to a job or jobs within a certain
group. This corresponds to specific "job" resources with a "group" property
matching a certain pattern.

You might also want to restrict who can create *new* jobs. Since a new job does
not exist yet, you cannot create a rule for this action to apply to an existing
job.  Which means this corresponds to a generic resource with a "kind" called "job".

## Special API Token Authentication group

Clients of the [Web API](../api/index.html) may use the [Token Authentication](../api/index.html#token-authentication) method.  These clients are
placed in the special authorization group called `api_token_group`.

`api_token_group`
~   Special role given to all [API Token](../api/index.html#token-authentication) authenticated access.

## Rundeck resource authorizations

Rundeck declares a number of actions that can be referenced inside the access control policy document.

The actions and resources are divided into project scope and application scope:

### Application Scope Resources and Actions

You define application scope rules in the aclpolicy, by declaring this context:

    context:
      application: 'rundeck'


These are the Application scope actions that can be allowed or denied via the
aclpolicy:

* Creating Projects (`create` action on a resource type with kind 'project')
* Reading system information (`read` action on a resource type with kind 'system')
* Managing System level ACL Policies (actions on a resource type with kind 'system_acl')
    * Reading `read`
    * Creating `create`
    * Updating `update`
    * Deleting `delete`
    * Full access `admin`
* Disabling executions (`disable_executions` action on a resource type with kind 'system')
* Managing executions
    * Enabling executions (`enable_executions` action on a resource type with kind 'system')
    * Disabling executions (`disable_executions` action on a resource type with kind 'system')
    * Full control (`admin` action on a resource type with kind 'system')
* Administering user profiles (`admin` action on a resource type of kind 'user')
* Accessing SSH Keys (`create`,`update`,`read`, or `delete` action on a specific path within the storage 'storage' type)
* Actions on specific projects by name (actions on a `project` type)
    * Reading `read`
    * Deleting `delete`
    * Configuring `configure`
    * Importing archives `import`
    * Exporting archives `export`
    * Deleting executions `delete_execution`
    * Full access `admin`
* Managing Project level ACL Policies on specific projects by name (actions on a `project_acl` type)
    * Reading `read`
    * Creating `create`
    * Updating `update`
    * Deleting `delete`
    * Full access `admin`

The following table summarizes the generic and specific resources and the
actions you can restrict in the application scope:

Type       Resource Kind     Properties   Actions               Description
------     --------------    -----------  --------              ------------
`resource` `project`         none         `create`              Create a new project
"          `system`          none         `read`                Read system information
"          "                 none         `enable_executions`   Enable executions
"          "                 none         `disable_executions`  Disable executions
"          "                 none         `admin`               Enable or disable executions
"          `system_acl`      none         `read`                Read system ACL policy files
"          "                 none         `create`              Create system ACL policy files
"          "                 none         `update`              Update system ACL policy files
"          "                 none         `delete`              Delete system ACL policy files
"          "                 none         `admin`               All access to system ACL policy files
"          `user`            none         `admin`               Modify user profiles
"          `job`             none         `admin`               Manage job schedules
----------------------------

Table: Application scope generic type actions

Type          Properties    Actions                Description
-----         -----------   --------               ------------
`project`     "name"        `read`                 View a project in the project list
"             "             `configure`            View and modify project configuration
"             "             `delete`               Delete project
"             "             `import`               Import archive contents to the project
"             "             `export`               Export the project as an archive
"             "             `delete_execution`     Delete executions
"             "             `admin`                Full access to project
`project_acl` "name"        `read`                 Read project ACL Policy files
"             "             `create`               Create project ACL Policy files
"             "             `update`               Update project ACL Policy files
"             "             `delete`               Delete project ACL Policy files
"             "             `admin`                All access to project ACL Policy files
`storage`     "path","name" `create`               Create files in the storage facility
"             "             `update`               Modify files in the storage facility
"             "             `read`                 Read files and list directories in the storage facility
"             "             `delete`               Delete files in the storage facility
----------------------------

Table: Application scope specific resource actions


### Project Scope Resources and Actions

You define project scope rules in the aclpolicy by declaring this context:

    context:
      project: "(regex)"

The regex can match all projects using ".*", or you can simply put the project
name.

Note that for projects not matched by an aclpolicy, *no* actions will be granted
to users.

Also note that to hide projects completely from users, you would need to grant
or deny the "read" access to the project in the [Application Scope](#application-scope-resources-and-actions).

These are the Application scope actions that can be allowed or denied via the
aclpolicy:

* Create Jobs ('create' action on a resource type with kind 'job')
* Delete Jobs ('delete' action on a resource type with kind 'job')
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
"          "                 `delete`  Delete jobs
"          `node`            `read`    Read node information
"          "                 `create`  Create new node entries
"          "                 `update`  Modify node entries
"          "                 `refresh` Refresh node entry from a URL
"          `event`           `read`    Read history event information
"          "                 `create`  Create arbitrary history event entries
----------------------------

Table: Project scope generic type actions

Type      Properties                         Actions            Description
-----     -----------                        --------           ------------
`adhoc`                                      `read`             Read adhoc execution output
"                                            `run`              Run an adhoc execution
"                                            `runAs`            Run an adhoc execution as another user
"                                            `kill`             Kill an adhoc execution
"                                            `killAs`           Kill an adhoc execution as another user
`job`     "name","group"                     `read`             View a Job and its executions
"                                            `update`           Modify a job
"                                            `delete`           Delete a job
"                                            `run`              Run a job
"                                            `runAs`            Run a job as another user
"                                            `kill`             Kill a running job
"                                            `killAs`           Kill a running job as another user
"                                            `create`           Create the matching job
"                                            `toggle_schedule`  Enable/disable the job's schedule
"                                            `toggle_execution` Enable/disable the job for execution
`node`    "rundeck_server", "nodename", ...  `read`             View the node in the UI (see [Node resource properties](#node-resource-properties))
"                                            `run`              Run jobs/adhoc on the node
----------------------------

Table: Project scope specific resource actions

*Note*: see [Node resource properties](#node-resource-properties) for more node resource properties for authorization.

*Note*: `runAs` and `killAs` actions only apply to certain API endpoints, and allow running jobs or adhoc executions or killing executions to be performed with a different username attached as the author of the action.  See [Rundeck API - Running a Job](../api/index.html#running-a-job).

*Note*:
Job deletion requires allowing the 'delete' action
both at the generic type
and specific resource levels.

Recall that defining rules for a generic resource type is done in this way:

~~~~~~~~ {.yaml}
for:
  resource:
    - equals:
        kind: 'project'
      allow: [create]
~~~~~~~~~~~~~~~~~~~~~~~~~~~~

Whereas defining rules for specific resources of a certain type is done in this
way:

~~~~~~~~~ {.yaml}
for:
  job:
    - equals:
        name: bob
      allow: [run]
~~~~~~~~~~~~~~~~~~~~~~~

### Node resource properties

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

### Access control policy actions example

Below is an example policy document demonstrating policy actions
to create limited access for a group of users.
Users in the group "restart_user", are allowed to run three jobs in the "adm"
group, Restart, stop and start. By allowing `run` but not `read`,
the "stop" and "start" jobs will not be visible.

File listing: restart_user.aclpolicy example

~~~~~~ {.yaml .numberLines}
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

---

description: Limited user access for adm restart action.
context:
  application: 'rundeck'
for:
  resource:
    - equals:
        kind: system
      allow: [read] # allow read of system info
  project:
    - match:
        name: '.*'
      allow: [read] # allow view of all projects
by:
  group: [restart_user]
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

## Troubleshooting access control policy

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
3. Use the [rd-acl](../man1/rd-acl.html) tool to test and validate your policy files

For each entry in the audit log, you'll see all decisions leading up to either a
AUTHORIZED or a REJECTED message.  It's not uncommon to see REJECTED
messages followed by AUTHORIZED.  The important thing is to look at
the last decision made.
