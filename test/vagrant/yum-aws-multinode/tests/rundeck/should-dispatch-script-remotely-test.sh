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
describe "project: dispatch script remote node"


it_should_dispatch_script_remotely() {
    # Parse lines into array elements.
    IFS=$'\n\t'

    # Run the uname command across the nodes tagged 'adhoc'. Should be two nodes.
    su - $RUNDECK_USER -c "dispatch -p $RUNDECK_PROJECT -f -F '$REMOTE_NODE' -s /tests/rundeck/test-dispatch-script.sh -- arg1 arg2" > test.output

    cat >expected.output <<END
This is test-dispatch-script.sh
On node $REMOTE_NODE $REMOTE_NODE
With tags: remote remote
With args: arg1 arg2
END
    set +e
    diff expected.output test.output
    set -e
    result=$?
    rm expected.output test.output
    if ! $result ; then
        echo "FAIL: output differed from expected"
        exit 1
    fi
}

it_should_dispatch_url_remotely() {
    # Parse lines into array elements.
    IFS=$'\n\t'

    # Run the uname command across the nodes tagged 'adhoc'. Should be two nodes.
    su - $RUNDECK_USER -c "dispatch -p $RUNDECK_PROJECT -f -F '$REMOTE_NODE' -u file:/tests/rundeck/test-dispatch-script.sh -- arg1 arg2" > test.output

    cat >expected.output <<END
This is test-dispatch-script.sh
On node @node.name@ $REMOTE_NODE
With tags: @node.tags@ remote
With args: arg1 arg2
END
    set +e
    diff expected.output test.output
    set -e
    result=$?
    rm expected.output test.output
    if ! $result ; then
        echo "FAIL: output differed from expected"
        exit 1
    fi
}