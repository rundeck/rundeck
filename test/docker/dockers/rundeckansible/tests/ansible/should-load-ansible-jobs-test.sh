#!/usr/bin/env roundup
#
set -e
: ${RUNDECK_USER?"environment variable not set."} 
: ${RUNDECK_PROJECT?"environment variable not set."}

# Let's get started
# -----------------

# Helpers
# ------------

#. ./include.sh 


# The Plan
# --------
describe "load jobs: valid use of ansible plugin"
JOBID="4fd7f51b-ed22-479e-8785-8393411c0106"
RUN_MODULE_JOB="$JOBID"
JOBID2="72d84cce-0eac-49dc-9a16-1515453e3604"
RUN_PLAYBOOK_JOB="$JOBID2"
JOBID3="df5fa3e8-5891-456e-a8da-7d3f81f7b644"
RUN_SCRIPT_JOB="$JOBID3"

it_should_list_ansible_nodes() {

    
    bash -c "rd nodes list -p $RUNDECK_PROJECT "
    
}
it_should_have_test_ansible_node() {

    # Parse lines into array elements.
    IFS=$'\n\t'
    
   cmdout=($(bash -c "rd nodes list -p $RUNDECK_PROJECT -F test-ansible-node | grep -v '^#' "))
   expout=( test-ansible-node )
    echo "${cmdout[@]}"
    if ! test ${#expout[*]} = ${#cmdout[*]}
    then
        echo "FAIL: command output did not contain ${#expout[*]} lines. Contained ${#cmdout[*]}: ${cmdout[*]}"
        echo cmdout was: ${cmdout[@]}
        exit 1
    fi
    test "${cmdout[@]}" = "${expout[@]}"
}
it_should_have_two_ansible_nodes() {

    # Parse lines into array elements.
    IFS=$'\n\t'
    
   cmdout=($(bash -c "rd nodes list -p $RUNDECK_PROJECT -F tags:ansible-nodes  | grep -v '^#' "))
   expout=( $RUNDECK_NODE test-ansible-node )
    echo "${cmdout[@]}"
    if ! test ${#expout[*]} = ${#cmdout[*]}
    then
        echo "FAIL: command output did not contain ${#expout[*]} lines. Contained ${#cmdout[*]}: ${cmdout[*]}"
        echo cmdout was: ${cmdout[@]}
        exit 1
    fi
    let max=$(( ${#cmdout[*]} - 1 ))
    for i in $(seq 0 $max) ; do
        test "${cmdout[$i]}" = "${expout[$i]}"
    done   
}
it_should_load_ansible_jobs() {

    # load job file
    bash -c "rd jobs load -p $RUNDECK_PROJECT --format xml -f $HOME/atest/ansible-jobs.xml"


    # verify jobs exist

    bash -c "rd jobs list -p $RUNDECK_PROJECT -i $JOBID -% '%id'"
    bash -c "rd jobs list -p $RUNDECK_PROJECT -i $JOBID2 -% '%id'"
    bash -c "rd jobs list -p $RUNDECK_PROJECT -i $JOBID3 -% '%id'"
}

it_should_run_ansible_module_step(){
   bash -c "rd run -p $RUNDECK_PROJECT -i $RUN_MODULE_JOB -f | grep -v ^#" > test.output
    # diff with expected
    cat >expected.output <<END

Using /etc/ansible/ansible.cfg as config file
test-ansible-node | SUCCESS => {
    "changed": false,
    "ping": "pong"
}
rundeck1 | SUCCESS => {
    "changed": false,
    "ping": "pong"
}
END
    # set +e
    # diff expected.output test.output
    # result=$?
    # set -e
    cat test.output
    grep   'test-ansible-node | SUCCESS => {' test.output || ( echo "Expected output not seen" && exit 2 )
    grep   "$RUNDECK_NODE | SUCCESS => {" test.output || ( echo "Expected output not seen" && exit 2 )
}
it_should_run_ansible_playbook_step(){
     bash -c "rd run -p $RUNDECK_PROJECT -i $RUN_PLAYBOOK_JOB -f | grep -v ^#"  > test.output
    # diff with expected

    cat test.output
    grep   'PLAY \[An example playbook\]' test.output || ( echo "Expected output not seen" && exit 2 )
    grep   'ok: \[test-ansible-node\]' test.output || ( echo "Expected output not seen" && exit 2 )
    grep   "ok: \[$RUNDECK_NODE\]" test.output || ( echo "Expected output not seen" && exit 2 )
    grep   "TASK \[print the current date and time\]" test.output || ( echo "Expected output not seen" && exit 2 )
    grep   "changed: \[$RUNDECK_NODE\]" test.output || ( echo "Expected output not seen" && exit 2 )
    grep   "changed: \[test-ansible-node\]" test.output || ( echo "Expected output not seen" && exit 2 )
    
}
it_should_run_ansible_script_step(){
    bash -c "rd run -p $RUNDECK_PROJECT -i $RUN_SCRIPT_JOB -f --outformat '%node: %log' | grep -v ^#" > test.output
    # diff with expected
    
    grep "$RUNDECK_NODE: DATE" test.output || ( echo "Expected output not seen" && exit 2 )
    grep "$RUNDECK_NODE: UNAME" test.output || ( echo "Expected output not seen" && exit 2 )
    grep "test-ansible-node: DATE" test.output || ( echo "Expected output not seen" && exit 2 )
    grep "test-ansible-node: UNAME" test.output || ( echo "Expected output not seen" && exit 2 )

}
