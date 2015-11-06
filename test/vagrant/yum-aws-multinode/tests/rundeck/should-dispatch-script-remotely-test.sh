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
    # Run the script file on the remote node
    su - $RUNDECK_USER -c "dispatch -p $RUNDECK_PROJECT -f -F $REMOTE_NODE -s /tests/rundeck/test-dispatch-script.sh" > test.output
    test "$(head -n1 test.output)" = "Succeeded queueing adhoc"
    tail -n +3 test.output > test2.output

    # diff with expected
    cat >expected.output <<END
This is test-dispatch-script.sh
On node $REMOTE_NODE $REMOTE_NODE
With tags: remote remote
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

it_should_dispatch_script_utf8_remotely() {
    # Run the script file on the remote node
    su - $RUNDECK_USER -c "dispatch -p $RUNDECK_PROJECT -f -F $REMOTE_NODE -s /tests/rundeck/test-dispatch-script-utf8.sh" > test.output
    test "$(head -n1 test.output)" = "Succeeded queueing adhoc"
    tail -n +3 test.output > test2.output

    # diff with expected
    cat >expected.output <<END
This is test-dispatch-script-utf8.sh
UTF-8 Text: 你好
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

it_should_dispatch_script_remotely_dos_lineendings() {
    # Run the script file on the remote node
    su - $RUNDECK_USER -c "dispatch -p $RUNDECK_PROJECT -f -F $REMOTE_NODE -s /tests/rundeck/test-dispatch-script-dos.sh" > test.output
    test "$(head -n1 test.output)" = "Succeeded queueing adhoc"
    tail -n +3 test.output > test2.output

    # diff with expected
    cat >expected.output <<END
This is test-dispatch-script-dos.sh
On node $REMOTE_NODE $REMOTE_NODE
With tags: remote remote
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

it_should_dispatch_script_remotely_with_args() {
    su - $RUNDECK_USER -c "dispatch -p $RUNDECK_PROJECT -f -F '$REMOTE_NODE' -s /tests/rundeck/test-dispatch-script.sh -- arg1 arg2"> test.output
    test "$(head -n1 test.output)" = "Succeeded queueing adhoc"
    tail -n +3 test.output > test2.output

    # diff with expected
    cat >expected.output <<END
This is test-dispatch-script.sh
On node $REMOTE_NODE $REMOTE_NODE
With tags: remote remote
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

it_should_dispatch_url_remotely() {
    su - $RUNDECK_USER -c "dispatch -p $RUNDECK_PROJECT -f -F '$REMOTE_NODE' -u file:/tests/rundeck/test-dispatch-script.sh -- arg1 arg2"> test.output
    test "$(head -n1 test.output)" = "Succeeded queueing adhoc"
    tail -n +3 test.output > test2.output

    # diff with expected
    cat >expected.output <<END
This is test-dispatch-script.sh
On node @node.name@ $REMOTE_NODE
With tags: @node.tags@ remote
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