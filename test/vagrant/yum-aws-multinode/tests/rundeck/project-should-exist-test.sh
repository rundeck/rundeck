#!/usr/bin/env roundup
#

: ${RUNDECK_USER?"environment variable not set."} 
: ${RUNDECK_PROJECT?"environment variable not set."}
: ${REMOTE_NODE?"environment variable not set."}

# Let's get started
# -----------------

# Helpers
# ------------

#. ./include.sh 


# The Plan
# --------
describe "project: rundeck project should exist: $RUNDECK_PROJECT"


it_should_list_project_nodes() {
    su - $RUNDECK_USER -c "dispatch -p $RUNDECK_PROJECT"
}

it_should_contain_remote_node() {

    # Parse lines into array elements.
    IFS=$'\n\t'

    # Run the uname command across the nodes tagged 'adhoc'. Should be two nodes.
    rawout=($(su - $RUNDECK_USER -c "dispatch -p $RUNDECK_PROJECT -I tags=remote"))

    test "${rawout[0]}" = $REMOTE_NODE

    # Count the lines in the raw output to strip off the queue and follow link lines.
    size=${#rawout[@]}

    # Create an array by slicing the lines with the command ouput.
    eval cmdout=( ${rawout[@]:2:$size} )

    # There should be two lines, one for each uname response.
    if ! test 1 = ${#cmdout[*]}
    then
        echo "FAIL: output did not contain 1 node. Contained ${#cmdout[*]}"
        echo cmdout was: ${cmdout[@]}
        exit 1
    fi
    
}