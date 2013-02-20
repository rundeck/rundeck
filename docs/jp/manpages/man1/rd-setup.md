% rd-setup(1) | Version ${VERSION}
% Alex Honor
% November 20, 2010

# NAME

rd-setup - Setup the Rundeck software after the distribution has been extracted into its installation directory.

# SYNOPSIS

rd-setup [-fv] -n nodename [-N hostname] [-s serverhostname] [ \--key=value ]...

# DESCRIPTION

The rd-setup command is used to setup the Rundeck software after the distribution has been extracted into its installation directory.

During the first time installation or if the -f flag is set rd-setup
will create a new instance of the framework and generate all the
configuration files found in $RDECK_BASE/etc.

# OPTIONS

-h
: displays the usage information presented above.

-v
: run verbose.

-f
: Force re-generation of configuration files

-n
: The nodename

-N
: The hostname

\--key=value
: Override the default values during the installation by specifying one or more \--key=value combinations.

# ENVIRONMENT VARIABLES #

The following environment variables are assumed during the execution
of rd-setup:

* JAVA_HOME
:Java installation directory

* RDECK_BASE
: Rundeck framework instance directory

# EXECUTION #

*Examples*

Execute the rd-setup command defining the hostname setting as
"adminhost":

    rd-setup -n adminhost

Execute the rd-setup command defining the framework hostname as
"localhost" and specifies the framework.ssh.keypath value

    rd-setup -n localhost --framework.ssh.keypath=/path/to/keyfile

# FILES #

The rd-setup command generates a working configuration from a set of
template files found in: $RDECK_BASE/lib/templates/etc. Additionally,
initial configuration default values are maintained in
rd-defaults.properties but these can be overridden using
\--key=value flags.

# SEE ALSO

[`rd-project` (1)](rd-project.html).

The Rundeck source code and all documentation may be downloaded from
<https://github.com/dtolabs/rundeck/>.
