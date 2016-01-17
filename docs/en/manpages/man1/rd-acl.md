% rd-acl
% Greg Schueler; Alex Honor
% April 6, 2015

## Name

rd-acl - Test and generate Rundeck ACL policy files.

## Synopsis

`rd-acl [*command*] [*command options*]...`

## Description

The rd-acl command can test aclpolicy files to check whether they will allow a certain action request or not.

It can also list the results for all defined access tests for a user or group.

It can also generate the correct policy for a certain action. This can be done in a semi-interactive mode
by specifying each [Common Option](#common-options) in turn, allowing it to prompt you with a set
of next possible options.

It can also take as input the output from the Rundeck AUDIT log, and generate ACL policy definitions 
to allow any disallowed actions.

It can also validate one or more files.


The tool works in these *COMMAND* modes:

`test`
:   test one or more aclpolicy files to determine if an action would be allowed.

`list`
:   list all access tests for a user or group for one or more aclpolicy files.

`create`
:   generate the correct ACL policy YAML definition for a specific action, or to allow a previously rejected 
    action as logged in the Rundeck AUDIT log.

`validate`
:   Validate a file or directory of files, and print messages about problems with the files.

## Related

See:

* [ACL Policy syntax](../man5/aclpolicy.html)
* [Access Control Policy](../administration/access-control-policy.html)

## Options


`-h, --help`
:   Print usage message.

`-v, --verbose`
:   Run verbosely.

### Test and Validate Command Options

The `test` and `validate` commands take these input options. 

`-d, --dir <dir>`
:   Directory. Load all policy files in the specified directory.

`-f, --file <file>`
:   File path. Load the specified aclpolicy file.

`-v`
:   If the tested action is not allowed, print the necessary ACL policy to allow it (as per the `create` action.)

One of `--dir` or `--file` is required for the `test` command. If the rdeck.base system property is defined, then
the Rundeck "etc" dir will be used as for the `--dir` option by default.

In addition,
the `test` command also takes the [Common Options](#common-options) and these options:

`-V, --validate`
:   Validate all input files, and exit with non-zero exit code if validation fails. (`test` and `list` actions.)

### Create Command Options

In addition to the [Common Options](#common-options), the `create` command takes these input options.

`-i, --input <file | ->`
:   Parse the Rundeck AUDIT log input, and for any REJECTED decisions, generate the appropriate aclpolicy 
    to allow the action.  If the value of the `--input` option is `-` (dash character), then the STDIN is read.
    If `--input` is used, then the Common Options are ignored.

`-r, --regex`
:   Match the resource using regular expressions. (create command).

### Common Options

These options define the Context, Subject, Action, and Resource,
which are used both to define an Access Request (for the `test` command),
and to define a rule in the ACL Policy (for the `create` command).

**Context options:**

`-c,--context <application | project>`
:   Context: either 'project' or 'application'. For project context, the `-p,--project` option is required.

**Subject options:**

`-g,--groups <group,...>`
:   Subject Groups names. Comma-separated list of user groups to
    validate (test command) or for by: clause (create command).

`-u,--user <user,...>`
:   Subject User name. Comma-separated list of user names to
    validate (test command) or for by: clause (create command).

**Action options:**

`-a,--allow <action,...>`
:   Actions to test are allowed (test command) or to allow (create command).

`-D,--deny <action,...>`
:   Actions to test are denied (test command) or to deny (create command).

**Resource Options:**

Resources are characterized as either "specific resources", or "resource types" 
(see [Specific Resources and Resource Types](../administration/access-control-policy.html#specific-resources-and-resource-types)).  You can specify "resource types" using the `-G, --generic <kind>` option. All specific resources can 
be specified directly using one of the options, or by type using `-R, --resource <type>` in combination with `-b, --attributes <attr=val ...>`.

`-G,--generic <kind>` 
:   Generic resource kind.

`-R,--resource <type>`
:   Resource type name. 

`-b,--attributes <key=value ...>`
:   Attributes for the resource. A sequence of key=value pairs, multiple pairs can follow with a space. Use a value of '?' to see suggestions.

The following define [Project scope resources](../administration/access-control-policy.html#project-scope-resources-and-actions):

`-A,--adhoc`
:   Adhoc execution (project context)

`-j,--job <group/name>`
:   Job group/name. (project context)

`-n,--node <nodename>`
:   Node name. (project context)

`-t,--tags <tag,..>`
:   Node tags. If specified, the resource match will be defined using 'contains'. (project context)

The following define [Application scope resources](../administration/access-control-policy.html#application-scope-resources-and-actions):

`-s,--storage <path/file>`
:   Storage path/name. (application context)

`-p,--project <project>`
:   Name of project, used in project context or for application resource.

### List Command Options

The `list` command uses these [Test Comand Options](#test-command-options) to set the aclpolicy files to be evaluated: `-f,--file` and `-d,--dir`.

It also allows these options from the [Common Options](#common-options):

The Subject options: `-g,--groups` and `-u,--user`

These Resource options:

* `-p,--project` name of a project to test
* `-s,--storage` Storage path/name
* `-n,--node` or `-t,--tags` name or tags of a node to tests
* `-j,--job` Job group/name.

## Test Command

The Test command loads the specified aclpolicy file or directory of files, and evaluates the Access Request defined 
by the [Common Options](#common-options), and emits the decision of whether the request is allowed, disallowed, or denied.
If it is allowed, then `rd-acl` will exit with a 0 exit code, otherwise it will exit with 2 exit code.

If the `-v, --verbose` flag is enabled, and the decision was not "allowed", 
then it will additionally automatically invoke the [Create Command](#create-command) 
to generate an aclpolicy definition to allow the requested action. 

**Note:** If the decision was "denied", then that indicates a specific DENY rule matches the Access Request.
A DENY rule that matches will override all ALLOW rules that match.

*Examples*

Test all aclpolicy files in the Rundeck "etc" directory by default, with an allowed result for `read` action:

~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~ 
rd-acl test --context application --groups test --storage keys/test1.pem --allow read
Using configured Rundeck etc dir: /etc/rundeck
The result was: allowed
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

Test all aclpolicy files in the Rundeck "etc" directory by default, with a rejected result for `create` action:

~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~ 
rd-acl test --context application --groups test --storage keys/key1.pem --allow create
Using configured Rundeck etc dir: /etc/rundeck
Result: REJECTED
The result was: not allowed
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~ 

The same as above, using the `--verbose` flag

~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~ 
rd-acl test --context application --groups test --storage keys/key1.pem --allow create -v
Using configured Rundeck etc dir: /etc/rundeck
Result: REJECTED
Decision for: res<name:key1.pem, path:keys, type:storage> subject<Username:user Group:test> action<create> env<http://dtolabs.com/rundeck/env/application:rundeck>: authorized: false: REJECTED, reason: REJECTED, evaluations: None
The result was: not allowed
Policies to allow the requested actions:
## create or append this to a .aclpolicy file
---
for:
  storage:
  - allow: create
    equals:
      name: key1.pem
      path: keys
description: generated
context:
  application: rundeck
by:
  group: test
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

## List Command

The List command loads the specified aclpolicy file or directory of files, 
and evaluates all access requests possible for the specified Group or User.
It lists the decision of whether each request is allowed, disallowed, or denied.

Further evaluations can be done by supplying some concrete values to identify a Project, Nodes, Jobs, and a Storage path.

If the `-v, --verbose` flag is enabled, and the decision was "denied", 
then it will print the decision details including the file name of the policy rule that denies the request.

In the output, a line starting with `-` indicates the action was rejected, 
`+` indicates the action was allowed, and `!` indicates the action was denied.

The evaluation lines are in the form:

    <result> <action>: <resource> [<reason>]

*Examples*

List results for just a group name:

~~~~
rd-acl list -g api_token_group
Using configured Rundeck etc dir: /etc/rundeck
# Application Context access for group api_token_group


(No project (-p) specified, skipping Application context actions for a specific project.)


(No storage path (-s) specified, skipping Application context actions for a specific storage path.)

- create: project [REJECTED]
+ read: system
- admin: job [REJECTED]
- admin: user [REJECTED]

(No project (-p) specified, skipping Project context listing.)
~~~~

Add a `-p` to specify a project:

~~~~
rd-acl list  -g api_token_group -p test
Using configured Rundeck etc dir: /etc/rundeck
# Application Context access for group api_token_group

- admin: project named "test" [REJECTED]
- delete_execution: project named "test" [REJECTED]
- import: project named "test" [REJECTED]
+ read: project named "test"
- export: project named "test" [REJECTED]
- configure: project named "test" [REJECTED]
- delete: project named "test" [REJECTED]

(No storage path (-s) specified, skipping Application context actions for a specific storage path.)

- create: project [REJECTED]
+ read: system
- admin: job [REJECTED]
- admin: user [REJECTED]

# Project "test" access for group api_token_group

- runAs: Adhoc executions [REJECTED]
+ kill: Adhoc executions
+ run: Adhoc executions
+ read: Adhoc executions
- killAs: Adhoc executions [REJECTED]

(No job (-j) specified, skipping Project context actions for a specific job.)


(No node (-n) or tags (-t) specified, skipping Project context actions for a specific node or node tags.)

+ read: node
+ refresh: node
+ create: node
+ update: node
+ create: event
+ read: event
+ create: job
+ delete: job
~~~~

Include `-j`, `-n` and `-s`:

~~~~
rd-acl list  -g api_token_group -p test -s keys/boingo.pem -j test/job -n mynode
Using configured Rundeck etc dir: /etc/rundeck
# Application Context access for group api_token_group

- configure: project named "test" [REJECTED]
- import: project named "test" [REJECTED]
- export: project named "test" [REJECTED]
- delete: project named "test" [REJECTED]
- delete_execution: project named "test" [REJECTED]
+ read: project named "test"
- admin: project named "test" [REJECTED]
+ read: storage path "keys/boingo.pem"
+ update: storage path "keys/boingo.pem"
! delete: storage path "keys/boingo.pem" [REJECTED_DENIED]
+ create: storage path "keys/boingo.pem"
- create: project [REJECTED]
+ read: system
- admin: job [REJECTED]
- admin: user [REJECTED]

# Project "test" access for group api_token_group

- runAs: Adhoc executions [REJECTED]
- killAs: Adhoc executions [REJECTED]
+ kill: Adhoc executions
+ run: Adhoc executions
+ read: Adhoc executions
+ run: Job "test/job"
- runAs: Job "test/job" [REJECTED]
+ create: Job "test/job"
+ read: Job "test/job"
+ delete: Job "test/job"
+ update: Job "test/job"
- killAs: Job "test/job" [REJECTED]
+ kill: Job "test/job"
+ read: Node "mynode"
+ run: Node "mynode"
+ update: node
+ refresh: node
+ read: node
+ create: node
+ create: event
+ read: event
+ delete: job
+ create: job
~~~~

Using the `-v` verbose flag shows more detail about the REJECTED_DENIED result:

~~~~
rd-acl list  -g api_token_group -p test -s keys/boingo.pem -v
Using configured Rundeck etc dir: /etc/rundeck
# Application Context access for group api_token_group

...snip
! delete: storage path "keys/boingo.pem" [REJECTED_DENIED]
  REJECTED, reason: REJECTED_DENIED, evaluations:   /etc/rundeck/apitoken.aclpolicy[2][rule: 1: {match={path=(keys|keys/.*)}, allow=*, deny=delete}] for actions: [delete] => REJECTED_DENIED
...snip

~~~~
   
## Create Command
   
The Create Command can generate ACL Policy YAML definitions based on the [Common Options](#common-options).
It can also parse the output from an authorization evaluation, as logged in the Rundeck AUDIT log, and generate
the policy necessary to allow any REJECTED evaluations.  In the case of DENIED evaluations, it will still
generate a policy to allow the access request, but note that the DENY rule would need to be removed to 
actually allow the specified action.

Create has a "semi-interactive" behavior. If you enter only some components of the 
Subject, Context, Action and Resource necessary to define a rule, then it will prompt with some possible values
for the next component.

*Examples*

Begin by typing the `create` command with no options

~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
rd-acl create 
-c/--context is required.
Choose one of: 
  -c application
    Access to projects, users, storage, system info.
  -c project
    Access to jobs, nodes, events, within a project.
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~ 

Add a `-c project` option:

~~~
rd-acl create -c project
-p/--project is required.
Choose the name of a project, or .*: 
  -p myproject
  -p '.*'
~~~

Add a `-p` option:

~~~
rd-acl create -c project -p '.*'
-g/--groups <group,...> or -u/--user <user,...> are required
  -u user1,user2... 
  -g group1,group2... 
    Groups control access for a set of users, and correspond
    to authorization roles.
~~~

Add a `-g` option:

~~~
rd-acl create -c project -p '.*' -g test
Project-context resource option is required.
Possible options:
  Job: -j/--job <group/name>
    View, modify, create*, delete*, run, and kill specific jobs.
    * Create and delete also require additional -G/--generic <kind> level access.
  Adhoc: -A/--adhoc
    View, run, and kill adhoc commands.
  Node: -n/--node <nodename>
      : -t/--tags <tag,..>
    View and run on specific nodes by name or tag.
  Resource: -R/--resource <type>
    Specify the resource type directly. -b/--attributes <key=value ...> should also be used.
    resource types in this context: 
    node
    job
    adhoc
  Generic: -G/--generic <kind>
    Create and delete jobs.
    View and manage nodes.
    View events.
    generic kinds in this context: 
    node
    event
    job
~~~

Here we see several options to specify the resource. the `Job`, `Adhoc` and `Node` options define both
the resource type, and some resource attributes.  The `-R` option can set the resource type directly, 
and then the `-b` attributes option can be used to define the attributes.  
Otherwise the `-G` option can be used to match a resource kind.

We will specify node tags using `-t`:

~~~
rd-acl create -c project -p '.*' -g test -t prod,www
-a/--allow or -D/--deny is required.
  -a action1,action2,...
  -D action1,action2,...
Possible actions in this context: 
  *
  read
  run
~~~

Now we are prompted to choose actions to allow or deny for the rule, and the ACL policy
definition is printed.

~~~
rd-acl create -c project -p '.*' -g test -t prod,www -a read -D run
## create or append this to a .aclpolicy file
---
for:
  node:
  - allow: read
    deny: run
    contains:
      tags:
      - prod
      - www
description: generated
context:
  project: .*
by:
  group: test
~~~

## Validate Command

The Validate command loads the specified aclpolicy file or directory of files, and validates the aclpolicy definitions.
It prints any errors found and it will exit with a 0 exit code if no errors are found, otherwise it will exit with 2 exit code.

*Examples*

Validate all aclpolicy files in the Rundeck "etc" directory by default:

~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~ 
$ rd-acl validate
Using configured Rundeck etc dir: /etc/rundeck
The validation passed
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

Validate a specific file:

~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~ 
$ rd-acl validate -f bad5.aclpolicy
/Users/greg/rundeck25/bad5.aclpolicy[1]:
  Context section is not valid: {xproject=asdf}, it should contain only 'application:' or 'project:'
The validation failed
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~



## See Also

[`aclpolicy`](../man5/aclpolicy.html)

[Access Control Policy](../administration/access-control-policy.html)

