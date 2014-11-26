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

    # There should be 1 line for the node.
    if ! test 1 = ${size}
    then
        echo "FAIL: output did not contain 1 node. Contained ${#rawout[*]}"
        echo rawout was: ${rawout[@]}
        exit 1
    fi
    
}