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

    bash -c "rd adhoc -p $RUNDECK_PROJECT -F $REMOTE_NODE -f -- whoami"

}

it_should_dispatch_uname_remotely() {

    # Parse lines into array elements.
    IFS=$'\n\t'

    # Run the uname command across the nodes tagged 'adhoc'. Should be two nodes.
    cmdout=($(bash -c "rd adhoc -p $RUNDECK_PROJECT -f -F '$REMOTE_NODE tags: remote' -- uname -n | grep -v ^#"))

    # There should be one line for the uname response.
    if ! test 1 = ${#cmdout[*]}
    then
        echo "FAIL: command output did not contain two lines. Contained ${#cmdout[*]}"
        echo cmdout was: ${cmdout[@]}
        exit 1
    fi
    
}