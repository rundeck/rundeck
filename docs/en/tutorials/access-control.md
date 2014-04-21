## Access control

Access to jobs and commands is managed in access control
policies defined using the [aclpolicy] document format.
The aclpolicy file contains rules describing what user
groups are allowed to perform which actions. The
[Access control policy] chapter in the Administration Guide
covers this in detail.

The administrator will use aclpolicy to define two levels of
access. The first level, has limited privilege and allows for just
running jobs. The second level, is administrative and can modify job
definitions.

Policies can be organized into more than one file to help organize
access by group or pattern of use. The normal Rundeck install will
define two user groups: "admin" and "noc" and have a generated a policy
for the "admin" group. 

The Acme administrator decides to create a policy that allows users in
the "noc" group to run commands just in the 
"web" Job group. We can employ the "noc" login and group as
it was also included in the normal install.

To create the aclpolicy file for the "noc" group:


File listing: noc.aclpolicy

~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~{.yaml .numberLines}
description: Acess for noc user.
context:
  project: '.*'
for:
  resource:
    - allow: read
  job:
    - match:
        group: 'web'
        name: '.*'
      allow: [run,read]
by:
  group: noc
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

> Note, It is not required to restart Rundeck after changes to .aclpolicy files are made.

Login as the "noc" user (the password is probably "noc"). 
Just the Jobs in the "web" group are
displayed in the Jobs page. The "noc" user is not allowed to access
jobs outside of "web" group.

Notice the absence of the "New Job" button that would be displayed if
logged in as "admin". Job creation is an action not granted to
"noc". 
Notice also, listed Jobs do not include an icon for editing the Job.

## Command ACL policy

File listing: noc.aclpolicy

~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~{.yaml .numberLines}
description: Acess for noc users.
context:
  project: '.*'
for:
  resource:
    - allow: read
  job:
    - match:
        group: 'web'
        name: '.*'
      allow: [run,read]
  adhoc:
    - allow: [read,run,kill]
  node:
    - match:
        nodename: '.*'
      allow: [read]
    - contains:
        tags: www
      allow: [read,run]
by:
  group: noc
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

The users in the "dev" group are granted access
to run the "Status" Job but are not allowed to
run the Restart job. They are allowed to view the
definition of the Restart job.

File listing: dev.aclpolicy

~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~{.yaml .numberLines}
description: Acess for dev users.
context:
  project: '.*'
for:
  resource:
    - allow: read
  job:
    - equals:
        group: 'web'
        name: 'Status'
      allow: [run,read]
    - equals:
        group: 'web'
        name: 'Restart'
      allow: [read]
  node:
    - allow: [read,run] 
by:
  group: dev
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~




[aclpolicy]: ../man5/aclpolicy.html
[Access control policy]: ../administration/access-control-policy.html
