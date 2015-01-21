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
describe "project: dispatch script remote node using storage key"

it_should_dispatch_script_remotely_stored_key() {
    # Run the script file on the remote node
    su - $RUNDECK_USER -c "dispatch -p $RUNDECK_PROJECT -f -F ${REMOTE_NODE}-stored -s /tests/rundeck/test-dispatch-script.sh" > test.output
    test "$(head -n1 test.output)" = "Succeeded queueing adhoc"
    tail -n +3 test.output > test2.output

    # diff with expected
    cat >expected.output <<END
This is test-dispatch-script.sh
On node ${REMOTE_NODE}-stored ${REMOTE_NODE}-stored
With tags: remote-stored remote-stored
With args: 
END
    set +e
    diff expected.output test2.output
    result=$?
    set -e
    rm expected.output test.output test2.output
    if [ 0 != $result ] ; then
        echo "FAIL: output differed from expected"
        exit 1
    fi
}

it_should_dispatch_script_remotely_dos_lineendings_stored_key() {
    # Run the script file on the remote node
    su - $RUNDECK_USER -c "dispatch -p $RUNDECK_PROJECT -f -F ${REMOTE_NODE}-stored -s /tests/rundeck/test-dispatch-script-dos.sh" > test.output
    test "$(head -n1 test.output)" = "Succeeded queueing adhoc"
    tail -n +3 test.output > test2.output

    # diff with expected
    cat >expected.output <<END
This is test-dispatch-script-dos.sh
On node ${REMOTE_NODE}-stored ${REMOTE_NODE}-stored
With tags: remote-stored remote-stored
With args: 
END
    set +e
    diff expected.output test2.output
    result=$?
    set -e
    rm expected.output test.output test2.output
    if [ 0 != $result ] ; then
        echo "FAIL: output differed from expected"
        exit 1
    fi
}

it_should_dispatch_script_remotely_with_args_stored_key() {
    su - $RUNDECK_USER -c "dispatch -p $RUNDECK_PROJECT -f -F '${REMOTE_NODE}-stored' -s /tests/rundeck/test-dispatch-script.sh -- arg1 arg2"> test.output
    test "$(head -n1 test.output)" = "Succeeded queueing adhoc"
    tail -n +3 test.output > test2.output

    # diff with expected
    cat >expected.output <<END
This is test-dispatch-script.sh
On node ${REMOTE_NODE}-stored ${REMOTE_NODE}-stored
With tags: remote-stored remote-stored
With args: arg1 arg2
END
    set +e
    diff expected.output test2.output
    result=$?
    set -e
    rm expected.output test.output test2.output
    if [ 0 != $result ] ; then
        echo "FAIL: output differed from expected"
        exit 1
    fi
}

it_should_dispatch_url_remotely_stored_key() {
    su - $RUNDECK_USER -c "dispatch -p $RUNDECK_PROJECT -f -F '${REMOTE_NODE}-stored' -u file:/tests/rundeck/test-dispatch-script.sh -- arg1 arg2"> test.output
    test "$(head -n1 test.output)" = "Succeeded queueing adhoc"
    tail -n +3 test.output > test2.output

    # diff with expected
    cat >expected.output <<END
This is test-dispatch-script.sh
On node @node.name@ ${REMOTE_NODE}-stored
With tags: @node.tags@ remote-stored
With args: arg1 arg2
END
    set +e
    diff expected.output test2.output
    result=$?
    set -e
    rm expected.output test.output test2.output
    if [ 0 != $result ] ; then
        echo "FAIL: output differed from expected"
        exit 1
    fi
}