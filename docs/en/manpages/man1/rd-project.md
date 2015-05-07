% rd-project
% Greg Schueler
% August 17, 2011

# NAME

rd-project - Create projects.

# SYNOPSIS

`rd-project [-vh] -a create -p projectname [ --property=value ]...`

# DESCRIPTION

The rd-project command is used to create projects.

The command will create the necessary directory structure and configuration files, and can be used to configure the project's properties.

# OPTIONS

`-h`

:    displays the usage information presented above.

`-v`

:    run verbose.

`-a`

:    The action name. Only "create" is supported.

`-p`

:    The project name, required.

`--property=value`

:    Specify property values for the project's configuration file. Include one or more \--property=value combinations.

# ENVIRONMENT VARIABLES #

The following environment variables are assumed during the execution
of rd-setup:

* JAVA_HOME
:Java installation directory

* RDECK_BASE
: Rundeck install directory

# EXECUTION #

*Examples*

Create a new project named "production":

~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~ {.bash}
    rd-project -a create -p production
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~ 

Create a new project and specify the "project.ssh-keypath" value as well as a path for the nodes data.

~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~ {.bash}
    rd-project -a create -p production \
       --project.ssh-keypath=/path/to/keyfile \
       --resources.source.1.config.file=/path/to/resources
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~ 


# SEE ALSO

[`rd-setup`](rd-setup.html).

