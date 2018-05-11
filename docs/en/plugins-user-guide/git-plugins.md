% SCM Git Plugin

Rundeck provides SCM Export and SCM Import providers for Git.

This plugin allows Source Code Management of the jobs, versioning, exporting or importing their definitions using a remote Git repository.


## Configuring Git Export

### Commiter Configuration

**Committer Name** and **Committer Email** are mandatory fields, the recommended configuration is to use the default `${user.fullName}` and `${user.email}`. The email and name of the current user can be set on the Profile page.

### Git Repository Configuration

**The Base Directory** is a local folder on the server node used to clone the git repository.

**Git url** and **Branch** are the common repository settings.

**Fetch automatically** automatize the fetch command to be called in background.

### Job Source Files Configuration

**Export UUID Behavior** can be one of these values: `preserve`, `original` or `remove`.

* `preserve` - Write the Job UUID into exported Jobs, and as `${job.id}` in the **File Path Template**
* `original` - Write the imported Source UUID into exported Jobs, and use it as the `${job.sourceId}` in the **File Path Template**. This value is different from the job UUID.
* `remove` - Do not write a UUID into the exported Jobs.

Changing this value modifies the file definition and files need to be pushed again to the repository.

**File Path Template** is the path template for storing a Job to a file within the base dir. It works using these patterns:

* `${job.name}` - the job name
* `${job.group}` - blank, or path/
* `${job.project}` - project name
* `${job.id}` - job UUID
* `${job.sourceId}` - Original Job UUID, this is a random UUID different from `${job.id}` (See above `original` UUID Behavior)
* `${config.format}` - Serialization format `xml` or `yaml`.


**Format** store files using `xml` or `yaml` format.

### Authentication Configuration

**SSH: Strict Host Key Checking**: If yes, require remote host SSH key is defined in the `~/.ssh/known_hosts` file, otherwise do not verify.

**SSH Key Storage Path** (Optional): A Storage Key path containing the private key to be used with git authentication.

**Password Storage Path** (Optional): A password stored in the Key Storage to be used on the ssh or https git authentication.


## Git Import Configuration

### Git Repository Configuration

**The Base Directory** is a local folder on the server node used to clone the git repository.

**Git url** and **Branch** are the common repository settings.

**Fetch automatically** automatize the fetch command to be called in background.

**Pull automatically** automatically pull remote changes on automatic fetch. If false, you can always perform it manually.

### Job Source Files Configuration

**Import UUID Behavior** how to handle UUIDs from imported Job source files

* `preserve` - Preserve the Source UUID as the Job UUID
* `archive` - Remove the Source UUID but keep it for use in Export. Allows you to use `${job.sourceId}` in the **File Path Template** instead of `${job.id}`.
* `remove` - Remove the source UUID.


**File Path Template** is the path template for storing a Job to a file within the base dir. It works using these patterns:

* `${job.name}` - the job name
* `${job.group}` - blank, or path/
* `${job.project}` - project name
* `${job.id}` - job UUID
* `${job.sourceId}` - Original Job UUID, this is a random UUID different from `${job.id}` (See above `archive` UUID Behavior)
* `${config.format}` - Serialization format `xml` or `yaml`.

### Authentication Configuration

**SSH: Strict Host Key Checking**: If yes, require remote host SSH key is defined in the `~/.ssh/known_hosts` file, otherwise do not verify.

**SSH Key Storage Path** (Optional): A Storage Key path containing the private key to be used with git authentication.

**Password Storage Path** (Optional): A password stored in the Key Storage to be used on the ssh or https git authentication.


### Setup Configuration

You can set **Match a Regular Expression?** to `yes` to enter a regular expression that is going to be checked to match all paths that match the regular expression to be imported.
If you set it to `no` on the next step you are going to be asked to select one by one the files to be imported.

## Advanced configurations

### Use the same repo for multiple projects.

There is more than one way to use a single repository for multiple projects.

You can use different branches of the same repository for each project or you can use the same branch but using folders in the repository.

This is an example of use folders inside the same repository and branch.

### Export Configuration

On the first project, called *project-a* in this example:
Set **Export UUID Behavior** to `original`.
Set **File Path Template** to `project-a/${job.group}${job.name}-${job.sourceId}.${config.format}`. 

In another project, called *project-b* on this example, use the same configuration, just change the **File Path Template** to `project-b/${job.group}${job.name}-${job.sourceId}.${config.format}`. 

### Import Configuration

On the first project, to import jobs from *project-a* in the last example:
Set **Import UUID Behavior** to `archive`.
Set **File Path Template** to `project-a/${job.group}${job.name}-${job.sourceId}.${config.format}`.
Set **Match a Regular Expression?** to `yes` and **Regular Expression** to `project-a/.*\.xml` or `project-a/.*\.yaml`.

On the other project, to import *project-b*:
Set **Import UUID Behavior** to `archive`.
Set **File Path Template** to `project-b/${job.group}${job.name}-${job.sourceId}.${config.format}`.
Set **Match a Regular Expression?** to `yes` and **Regular Expression** to `project-b/.*\.xml` or `project-b/.*\.yaml`.
