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
describe "project: dispatch adhoc command to remote node"


it_should_dispatch_whoami_remotely() {

    su - $RUNDECK_USER -c "dispatch -p $RUNDECK_PROJECT -F $REMOTE_NODE -f -- whoami"

}

it_should_dispatch_uname_remotely() {

    # Parse lines into array elements.
    IFS=$'\n\t'

    # Run the uname command across the nodes tagged 'adhoc'. Should be two nodes.
    rawout=($(su - $RUNDECK_USER -c "dispatch -p $RUNDECK_PROJECT -f -F '$REMOTE_NODE tags: remote' -- uname -n"))

    test "${rawout[0]}" = "Succeeded queueing adhoc"

    # Count the lines in the raw output to strip off the queue and follow link lines.
    size=${#rawout[@]}

    # Create an array by slicing the lines with the command ouput.
    eval cmdout=( ${rawout[@]:2:$size} )

    # There should be one line for the uname response.
    if ! test 1 = ${#cmdout[*]}
    then
        echo "FAIL: command output did not contain two lines. Contained ${#cmdout[*]}"
        echo cmdout was: ${cmdout[@]}
        exit 1
    fi
    
}