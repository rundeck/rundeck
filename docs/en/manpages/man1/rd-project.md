% rd-project(1) | Version ${VERSION}
% Greg Schueler
% August 17, 2011

# NAME

rd-project - Create projects.

# SYNOPSIS

rd-project [-vh] -a create -p projectname [ \--property=value ]...

# DESCRIPTION

The rd-project command is used to create projects in the Rundeck base directory.

The command will create the necessary directory structure and configuration files, and can be used to configure the project's properties.

# OPTIONS

`-h`

:    displays the usage information presented above.

`-v`

:    run verbose.

`-a`

:    The action name. Only "create" is supported.

`-p`

:    The project name

`--property=value`

:    Specify property values for the project's configuration file. Include one or more \--property=value combinations.

# ENVIRONMENT VARIABLES #

The following environment variables are assumed during the execution
of rd-setup:

* JAVA_HOME
:Java installation directory

* RDECK_BASE
: Rundeck framework instance directory

# EXECUTION #

*Examples*

Create a new project named "production":

    rd-project -a create -p production

Create a new project and specify the "project.ssh-keypath" value as well as a URL for the "project.resources.url"

    rd-project -a create -p production --project.ssh-keypath=/path/to/keyfile \
    --project.resources.url=http://example.com/nodes


# SEE ALSO

[`rd-setup` (1)](rd-setup.html).

The Rundeck source code and all documentation may be downloaded from
<https://github.com/dtolabs/rundeck/>.
