% rd-options(1) | Version ${VERSION}
% Greg Schueler; Alex Honor
% November 20, 2010

# NAME

rd-options - introduction to rundeck dispatcher options

# DESCRIPTION

Both the dispatch and run commands can use the node-dispatch feature
to execute commands on remote nodes.

Finally the base set is filtered based on the filtering options
specified on the command line, as described below.

# FILTERING OPTIONS

Both run and dispatch can use the commandline options -I (include, aka
\--nodes) and -X (exclude, aka \--xnodes) to specify which nodes to
include and exclude from the base set of nodes. 

You can specify a single value, a list of values, or a regular
expression as the argument to these options.

When only inclusion filters (-I) are specified, then only the nodes
from the base set that match those filters are used.

*Examples*

    dispatch -I dev01 -- ps
    dispatch -I dev01,dev02 -- ps
    dispatch -I dev.* -- ps

This executes the ps command using various selector
types. The first matches only the node with hostname dev01, the second
two nodes with hostnames dev01 and dev02, and the third all nodes with
hostnames that match the regular expression dev.\*.

When only exclusion filters (-X) are specified, then only the nodes
from the base set that do not match those filters are used.

*Examples*

    dispatch -X web01 -- ps
    dispatch -X web01,web02 -- ps
    dispatch -X "web.*" -- ps

This executes the 'ps' command by excluding nodes based on hostname
selectors, thus it will execute on all nodes that are not matched by
the exclusion filters.

By default, the -I and -X options are used to filter based on the node
hostname property, as it is specified in the nodes.properties file.

However, keywords can be used to specify other properties to filter on
besides hostname.

# KEYWORDS

Keywords are specified by using one of the filter options with
"key=value" as the argument to the option.

*Example*

    dispatch -I os-name=Linux -- ps

This executes on all nodes with an operating system named "Linux".

Notice that the argument to -I specifies os-name= and then the value
Linux. The keyword used is os-name, and so the filter will match the
"os-name" property in nodes.properties.

The available keywords are:

*  hostname 
: hostname of the node [default keyword]

*  name 
: resource name of the node, which may be different than hostname

*  type 
: type name of the node, typically "Node"

*  tags 
: a set of user defined tags

*  os-name :
 operating system name, e.g. "Linux", "Macintosh OS X"

*  os-family 
: operating system family, e.g. "windows","unix"

*  os-arch 
: operating system CPU architecture, e.g. "x86", "x386"

*  os-version 
: operating system version number

All of the keywords can accept a single value, a comma-separated list,
or a regular expression.

# TAGS

In addition, the tags keyword can use the boolean operator + to
represent logical AND. The comma used to separate lists serves as
logical OR. The following example matches all nodes tagged with both
"devel" AND "secure", OR with "server":


    dispatch -I tags=devel+secure,server -- ps

## Combining Options

All keywords can be combined by specifying the -I or -X options
multiple times on the command line.

The following example includes all nodes tagged with both "devel" and
"secure", as well as all nodes with hostnames matching web.*:

    dispatch -I tags=devel+secure -I web.* -- ps

The following includes all nodes tagged with both "devel" and
"secure", as well as all nodes with hostnames matching web.*, and
excludes the node with hostname "web01":

    dispatch -X web01 -I tags=devel+secure -I web.* -- ps

Note, however, that you cannot specify the same keyword multiple times
for the same type of filter:

WRONG:

    dispatch -X web01 -X dev01 -I tags=devel+secure -I web.* -- ps

This example will not exclude the node "web01", because there are two
-X options with the same keyword. Both -X web01 and -X dev01 default
to the "hostname" keyword, and the second entry will override the
first. To exclude both of those nodes, you must combine them for the
option -X web01,dev01:

CORRECT:

    dispatch -X web01,dev01 -I tags=devel+secure -I web.* -- ps

The last example brings up the issue of Precedence. The command line
first specifies -X web01, then later specifies "-I web.\*". 
However, the hostname "web01" would be matched by the regular expression
"web.\*". So is the node with hostname "web01" included in the set of
nodes to execute on, or is it excluded?

# Attributes

Arbitrary attributes can be used in node filtering as well. If you add
Setting resources or other Resources to the Node object, any
Attributes exported by those resources are available for Node
filtering.

Simply specify the attribute name as a filter with one of the -I/-X
arguments:

    dispatch -I my-attribute=true -- ps

This will execute ps on any node with an attribute named my-attribute
with a value of "true".

# Precedence

Precedence is the issue of whether a node should be included in the
result set when it matches both an exclusion filter and an inclusion
filter.

Take a simplified example:

    dispatch -X web01 -I web.* -- ps
    
The intent is to exclude "web01" while including all other nodes
matching the regular expression "web.*". Depending on which filter
takes precedence, the exclusion filter or the inclusion filter, the
result may be different.

When inclusion has precedence, nodes that match both filters will be
included. When exclusion has precedence, nodes that match both filters
will be excluded. So which filter has precedence?

The first filter specified on a command line takes precedence.
This means that if you specify any -X option before a -I option, then
exclusion will take precedence, and vice versa.

So in the example above, the -X takes precedence (it is first), and so
the node with hostname "dev01" is excluded from the result set.

If you change the order of the options:

    dispatch -I web.* -X web01 -- ps

Then the node with hostname "web01" will be included in the results.

Note: When only one filter is used, either -I or -X, there is no need to
worry about precedence.

In general, a good rule of thumb when trying to determine which
precedence you need is to specify the most restrictive filter first.

For an inverse example, suppose you want to dispatch to all
non-windows nodes, but you want to include any nodes tagged with
"development". You might try this at first:

WRONG:

    dispatch -X os-family=windows -I tags=development -- ps

This will not return the correct result set, because the -X takes
precedence as it is the first filter on the line. So any nodes that
have both os-family=windows and tag=development will be excluded.

CORRECT:

    dispatch -I tags=development -X os-family=windows -- ps

Here since the -I is specified first, the inclusion filter has
precedence, and any nodes that have both os-family=windows and
tag=development will be included in the result.

# Explicit Precedence using \--filter-exclude-precedence

Using the --filter-exclude-precedence command-line option, the
precedence can be set explicitly. The argument is "true" or
"false". When the argument is "true" then the exclusion filter takes
precedence, regardless of the order of the filter options. When the
argument is "false" then the inclusion filter takes precedence.

    dispatch -I web.* -X web01 --filter-exclude-precedence true -- ps

This command-line correctly excludes the "web01" node because the
--filter-exclude-precedence option is set to "true".

# Retrying on failed nodes

When the -K option is specified to run or dispatch, then the command
will be executed on all matched nodes, even if some nodes fail during
the process. The list of which nodes failed will be printed at the end
of the sequence.

    Command failed: Execution failed on the following nodes: [calculon,centos5]

If you simply execute a command with some node filters and the -K
option, then a message is printed echoing the same commandline that
you executed, but with the list of failed nodes inserted as the node
filters:

    $ dispatch -I tags=something -K -p demo -s myscript.sh
    ....
    Command failed: Execution failed on the following nodes: [calculon,centos5]
    Execute this command to retry on the failed nodes:
        dispatch -I name=calculon,centos5 -K -p demo -s myscript.sh

You can copy and paste the printed command to retry the same command
only on the list of failed nodes.

## Storing the failed node list in a file:

Jobs and dispatch have an option that stores the list of nodes where
the command failed into a file, which can then be specified again to
re-execute the command on only those failed nodes.

Use the "failednodes" option:

    -F,--failednodes Filepath to store failed nodes

When you specify a set of Node filters, as well as the -K option, also
include the -F option with the path to a file.

E.g.:

    $ dispatch -K -F /home/ctier/tempnodes -I tags=mynodes -p demo -- ps

If the execution fails on some nodes, that list is stored in the file,
and an additional message is printed:

    error: Execute this command to retry on the failed nodes:    
    dispatch -K -F /home/ctier/tempnodes -p demo -- ps

Notice that this command specifies the same filepath as originally
specified, but not the Node filtering options. The list of nodes will
be read from the file.

When all the executions succeed on the nodes, any file at the
specified path will be deleted. This means that you can have some
looping logic in a shell script to re-try the execution if the
specified file exists:

    #!/bin/bash
    COMMAND=...
    NODEFILE=/home/ctier/tempnodes
    dispatch -K -F $NODEFILE -I tags=mynodes -p demo -- $COMMAND
    if [ -f $NODEFILE ] ; then
       # since the node file exists, some nodes failed, retry.
      dispatch -K -F $NODEFILE -p demo -- $COMMAND   
    fi
     
    if [ -f $NODEFILE ] ; then
      # if the file still exists, then some nodes failed again
     echo "Some nodes failed after retry, aborting..."
     exit 1
    fi

# SEE ALSO

[`dispatch` (1)](dispatch.html), [`run` (1)](run.html), [`rd-jobs` (1)](rd-jobs.html). 

The Rundeck source code and all documentation may be downloaded from
<https://github.com/dtolabs/rundeck/>.
