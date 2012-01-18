% Upgrade Guide
% Greg Schueler
% September 12, 2011

## Important Changes in Rundeck 1.4.1

In Rundeck 1.4.0.x, the new ACL Policy format used an incorrect property key ("job") to check Job Authorizations by name.  The correct key was used in all documentation, but not in the underlying code.  The correct key is "name".

This issue has been fixed in Rundeck 1.4.1, however if you were using the incorrect key previously, you will have to change your aclpolicy files to the correct key.

The [Project Scope Resources and Actions](../administration/authorization.html#project-scope-resources-and-actions) section shows the correct way to authorize Job resources by name:

    for:
      job:
        - equals:
            name: bob
          allow: [run]


## Important Changes in Rundeck 1.4

These changes in version 1.4 are important to note if you are upgrading an
existing Rundeck installation.

1. [Database table and field name changes to support Mysql & Oracle](#database-changes)
2. [ACL Policy file format and behavior changes](#acl-policy-changes)

**Note:** before upgrading, it is a good idea to back up your Rundeck installation using the instructions here: [Backup and Recovery](../administration/backup-and-recovery.html).

## Upgrade procedure

### System packaging

If you are using RPM or Debian packaging, refer to the basic [Install Instructions](http://rundeck.org/downloads.html), and simply upgrade the package.

### Launcher Jar

For the Rundeck Launcher jar, you can follow this basic procedure:

1. Stop the currently running Rundeck java process
2. Copy the new `rundeck-launcher-1.4x.jar` file either to your `$RDECK_BASE` directory, or wherever you had your previous jar
3. Remove these directories from your previous `$RDECK_BASE`:
    * `$RDECK_BASE/server/lib`
    * `$RDECK_BASE/server/exp`
    * `$RDECK_BASE/tools`
4. You can now start Rundeck with the new launcher jar, either specifying the `-b basedir` option, or leaving it off to use the directory the launcher jar is in.

## Database changes

Unfortunately, Rundeck 1.3 and earlier used domain class names that conflicted with some reserved words in Mysql and Oracle, specifically "user" and "option".

To fix this, we have changed the table/field mappings to "rduser" and "rdoption"
for the domain classes that used these names.

The new table/field names are only used if a new config value is set to `true`
in the rundeck-config.properties file:

    rundeck.v14.rdbsupport=true

This value is set to `false` by default on new installations, so if you have a previous
1.3 installation using the file-based datasource, upgrading to 1.4 should not
cause any issues.

To configure a relational database backend, you must set this to "true".  See the section in the Rundeck Guide Administration chapter: [Enable rdbsupport](../administration/relational-database.html#enable-rdbsupport).

## ACL Policy changes

NOTE: If you are installing Rundeck 1.4 from scratch, your installation will come with default aclpolicy files will get you up and running, this document is merely a guide for people upgrading from Rundeck 1.3 or earlier who have customized their aclpolicy files.

### Shortest Path

The simplest way to upgrade is to add or replace the "admin.aclpolicy" file with
the [Example admin.aclpolicy](#example-admin.aclpolicy) at the end of this document. 

This will give full authorization to 'admin' role users.

### Authorization and Acl Policy changes

The authorization system in Rundeck 1.4 has been updated.  Previously some authorizations only for Job-related actions were declared in the "*.aclpolicy" files located in your `etc` directory for Rundeck, and some GUI layer authorizations were defined as "Mapped Roles" in your `rundeck-config.properties` file.

We've removed the "Mapped Roles" completely, and revamped the ACL Policy code to support authorization of these types of Application-layer actions, as well as improved the ACL policy layer to support restricting access to resources other than just Jobs.

The consequence of these changes is that if you upgrade from Rundeck 1.3 or earlier, your authorization configuration will have to change.  If you don't modify your configuration, you will log into Rundeck and likely be told you don't have authorization to see certain resources or perform certain actions.

These are the changes you will have to make when you upgrade

* convert your old aclpolicy Job authorization rules to the new format
* add new authorizations rules for the new types of authorization checks in Rundeck 1.4
* translate any custom "Mapped Role" definitions into .aclpolicy files

Practically, this means you will have to:

* convert any XML formatted .aclpolicy files to YAML
* update any YAML .aclpolicy files 
* add new authorization rules for the new authorization checks
* add new authorization rules for the old "Mapped Roles"

Highlights of the benefits of the new authorization changes:

* Acccess control on resources other than Jobs can now be declared
* Project level access control is now supported
* "Deny" rules can now be declared
* Application level access control is also supported, replacing the Role mapping
* The Rundeck server no longer uses role-mapping and instead defers to the aclpolicy for all authorizations

### Managing ACL Policy files

The `*.aclpolicy` files live in the Rundeck `etc` dir.

Each file can contain multiple policy definitions, and there can be multiple files in the directory.

So when upgrading you have a few options for how to manage the transition from old to new file formats:

2. modify your files in place to convert them, and add the new authorizations to them
1. leave your old files where they are, and create new aclpolicy files in the new format for job authorizations as well as all of the new authorizations
3. modify your file simply to convert it to the new format, and add new files to support the new authorizations

Leaving your old aclpolicy files in place will not cause any problems, because even though they are read by the authorization code they will simply not grant the necessary authorizations until converted to the new format.

Note: to add multiple policy definitions to a single file, use the YAML document separator "---" on a line by itself between the definitions.

### Format

The updated ACL Policy file format lets you allow and deny actions on particular resources for certain users and in certain contexts.

It is a more expressive language than the previous formats, although this adds some complexity. 

The important new features are:

`Context`

:    You now declare access control within a particular project or at the Application level. You specify this in the `context` section.

`Actions` and `Resources`

:   Actions are allowed or denied for a particular Resource.  If an action is to be restricted on *all* resources of a certain type, or for example on creating *any* resource of a certain type, then we use a Generic Resource Type as the Resource.

`Specific Resources` 

:   An example of this is: allow "run" action on a particular Job `[type: job, name: "Test Job", group: "my/group/path"]`. All Resources have a particular "type" and some associated identifying properties. 

`Generic Resource Type`

:   An example of this is: allow "create" action for jobs in general. The resource that would be tested is: `[type: resource, kind: job]`. In this example, the type is genericized as "resource", and the identifying property is the "kind" which is "job".

`Resource Patterns`

:   The Resource for the authorization request is matched against the Resource Patterns defined in the ACL Policy to find Rules defined for it. Each Resource Pattern is specified first by the value of the "type" of the resource, and subsequently by different matching patterns on the properties of the resource. All matching Resource Patterns are applied to the request, and depending on the rules, the specific action is allowed, denied or rejected.

`Rules`

:   Each Resource Pattern can declare the set of Actions that it allows and/or denies. If a matching resource pattern rule allows an action, the action is marked temporarily as "allowed", but subsequent matching Resource Patterns and rules are still applied. If any matching rule Denies an action, the action is immediately denied. If no Resource Patterns match a resource, or no matching Patterns have a rule that allows the action, then the action is also denied.

`Subject`

:   The subject is the user or account to authorize the action for. They can be identified by name, or by group (role) membership in the `by` section.

### Action names

Action names have changed from the previous formats.

Instead of `workflow_X` and `event_Y` type action names, the actions have been simplified to this set, although not all actions are used by every kind of resource:

* `create`
* `read`
* `update`
* `delete`
* `run`
* `kill`
* `admin`
* `refresh`

The actions are now specified directly on a resource or type in the aclpolicy definition.

### Converting XML aclpolicy files

You may have the old XML format in your current installation.

You can convert the XML to yaml:

    <policies>
      <policy description="Administrative group that has access to execute all actions.">
        <context project="*">
          <command group="*" job="*" actions="*"/>
        </context>
        <by>
          <group name="admin"/>
        </by>
      </policy>
    </policies>

This would convert to:

    description: Administrative group that has access to execute all actions.
    context:
      project: '.*'
    for:
      job:
        - equals:
            group: '.*'
            name: '.*'
          allow: '*'
    by:
      group: admin

However, this YAML document merely allows access to certain Jobs, and it doesn't allow any access to application level resources, or other project level resources besides jobs. You must add that access as you see fit.

Here is a [XMLStarlet](http://xmlstar.sourceforge.net/) command to convert your xml to the supported yaml format:

    FILE=$RDECK_BASE/etc/role.aclpolicy
    NEWFILE=$RDECK_BASE/etc/role-new.aclpolicy
    xmlstarlet sel -t --match '//policy' -o '---' -n -o 'description: ' -v '@description' -n \
        -o 'context: ' -n -o '  project: &quot;' -v 'context/@project' -o '&quot;' -n \
        -o 'for:' -n \
        -o '  job:' -n \
        --match 'context/command' \
        -o '    - equals: ' -n \
        -o '        group: &quot;' -v '@group' -o '&quot;' -n \
        -o '        name: &quot;' -v '@job' -o '&quot;' -n \
        -o '      allow: &quot;' -v '@actions' -o '&quot;' -n \
        -b \
        -m 'by/group' \
        -o 'by:' -n \
        -o '  group: &quot;' -v '@name' -o '&quot;' -n $FILE > $NEWFILE

### Converting older YAML files

Your yaml aclpolicy file may be out of date, and look like this:

    description: Yaml Policy 1
    rules:
      ^$:
        actions: 'foobar'
      /groupa/.*:
        actions: 'exact_match'
      .*/job1: 
        actions: pattern_match
      /listAction/.*:
        actions: [action_list_1,action_list_2]
    by:
        username: 'yml_usr_1'
        group: 'yml_group_1'

You can convert each "rules:" entry to a job resource pattern. For example the rule value ".*/job1" matches only jobs named "job1":

    for:
      job:
        - equals: #use "equals" to match exactly
            name: 'job1' # compare name property only, ignore group
          allow: [read]

And a rule value of "/groupa/.*" matches any jobs in group "matcha" or a subgroup. The equivalent is:

    for:
      job:
        - match: # use "match" to match via regular expression
            group: '^groupa/.*$' # compare group property, ignore name
          allow: [read,run]
          deny: [delete,update,kill]

Note, if you need to authorize actions on adhoc executions, use the 'adhoc' resource type and allow/deny the "run" and "kill" actions:

    for:
      adhoc:
        - allow: [run,kill]

### Adding the new Authorizations

The old ACL Policies only defined authorizations on Job actions and some adhoc execution actions.  If you converted an old file as described above you will now have give access to some actions on Jobs in one or more projects, but this is not sufficient to use all features of Rundeck.

Authorizations you need to grant to run jobs:

* 'read' access to some of the projects, at the Application context
  * This determines what projects a user can see, and is necessary for any access to jobs
* 'read' access to node resources in a project context
  * this allows the user to view the nodes for a project, and is necessary to run jobs or adhoc executions
* 'read' and 'run' actions to specific nodes in a project context
  * this allows the user to view specific nodes, and 'run' allows executing jobs or adhoc executions on the node

Other authorizations you may want to grant:

* 'read' access to events in the project context, to allow viewing the execution history of jobs and acho executions
* 'create' access to generic resource type 'job' in the project context, to allow creating new jobs

Note: you must separate Project and Application context policies into separate policy definitions.

For Application context: To grant read access to certain projects:

    description: Allow 'user' group access to all projects
    context:
      application: 'rundeck'
    for:
      project:
        - match:
            name: '.*'
          allow: [read] # allow view/admin of all projects
          deny: [admin] # explicitly deny project configuration changes
    by:
      group: 'user'

The following are all at Project context:

To grant read access generic Node resources:

    description: read access to nodes
    context:
      project: '.*' # all projects
    for:
      resource:
        - equals:
            kind: node
          allow: [read] # view the nodes  
          deny: [create,update,refresh] # deny modification of nodes
    by:
      group: 'user'

To grant read and run access to all nodes:

    description: read access to nodes
    context:
      project: '.*' # all projects
    for:
      node:
        - allow: [read,run] # view the nodes  
    by:
      group: 'user'
      
To grant read access to history events:

    description: read access to history
    context:
      project: '.*' # all projects
    for:
      resource:
        - equals:
            kind: event
          allow: [read] # view history events
    by:
      group: 'user'

To allow Job creation:

    description: grant job creation ability
    context:
      project: '.*' # all projects
    for:
      resource:
        - equals:
            kind: job
          allow: [create] # allow create jobs
    by:
      group: 'user'

You can add any or all of these policies, or combine them with your job policies.

### Converting Mapped Roles

Mapped roles defined in `rundeck-config.properties` were used to authorize actions in the Rundeck GUI in Rundeck 1.3 and earlier.

Here is the list of old "application roles", and how to translate them into the new aclpolicy format:

`mappedRoles.admin=[role list]`

:   "Admin" is no longer a special role given any special access other than the access granted to it in aclpolicy.  

`mappedRoles.user_admin=[role list]`

:   The "admin" action can be granted on generic resources of kind "user" at the application context level.

`mappedRoles.workflow_create=[role list]`

:   The "create" action can be granted on generic resources of kind "job" at the project level.

`mappedRoles.workflow_\*=[role list]`

:   For actions other than "create", ("read", "update", "delete", "run", "kill"), you can grant them to specific "job" resources within a project context.

`mappedRoles.events_read=[role list]`
`mappedRoles.events_create=[role list]`

:   You can grant "read" and "create" to generic resources of kind "event" at the project level.

`mappedRoles.resources_read=[role list]`
`mappedRoles.resources_create=[role list]`
`mappedRoles.resources_update=[role list]`
`mappedRoles.resources_delete=[role list]`

:   You can grant ("read", "create", "update") to generic resources of kind "node" at the project level.  You can also grant "refresh", which allows refreshing the Node resources from a predefined URL.

`mappedRoles.job_view_unauthorized=[role list]`

:   This is equivalent to allowing "read" action on a "job" resource.  If "read" is not allowed for a job it is not shown in the GUI.  If "run" is not allowed for a job but "read" is, it will be shown but not be runnable.
    
### Example admin.aclpolicy

This file grants all authorizations to 'admin' role, and explicitly enumerates the actions granted for each resource.  It could be simplified into a much shorter file by allowing '*' and not explicitly matching the resources.

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
        - allow: [run,kill] # allow running/killing adhoc jobs
      job: 
        - allow: [create,read,update,delete,run,kill] # allow create/read/write/delete/run/kill of all jobs
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
