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
describe "project: copy multiple files remotely"


it_should_copy_recursive_dir_files_remotely() {

    # load job file
    bash -c "rd jobs load -p $RUNDECK_PROJECT --format yaml -f /tests/rundeck/test-filecopy-job1.yaml"
    JOBID="3b09625a-8371-4d6c-9c04-9e8e90084547"


    # Parse lines into array elements.
    IFS=$'\n\t'

    SRCDIR=/tests/rundeck/files1
    DESTDIR=/tmp/test-files1/
    
    # Run the uname command across the nodes tagged 'adhoc'. Should be two nodes.
    bash -c "rd run -i $JOBID -p $RUNDECK_PROJECT -f -F '$REMOTE_NODE' -- -sourcedir '$SRCDIR' -destdir '$DESTDIR' | grep -v ^#"

    # list the remote dir
    cmdout=($(bash -c "rd adhoc -p $RUNDECK_PROJECT -f -F '$REMOTE_NODE' -- find $DESTDIR | grep -v ^# | sort "))
    expout=(${DESTDIR} ${DESTDIR}afile.txt ${DESTDIR}btest.txt ${DESTDIR}files2 ${DESTDIR}files2/cfile.xml)
    # There should be one line for the uname response.
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

it_should_copy_recursive_pattern_files_remotely() {

    # load job file
    bash -c "rd jobs load -p $RUNDECK_PROJECT --format yaml -f /tests/rundeck/test-filecopy-job3.yaml"
    JOBID="3b09625a-8371-4d6c-9c04-9e8e90084549"


    # Parse lines into array elements.
    IFS=$'\n\t'

    SRCDIR=/tests/rundeck/files1/
    DESTDIR=/tmp/test-files3/
    PATTERN=**/*file.*
    # Run the uname command across the nodes tagged 'adhoc'. Should be two nodes.
    bash -c "rd run -i $JOBID -p $RUNDECK_PROJECT -f -F '$REMOTE_NODE' -- -sourcedir '$SRCDIR' -pattern '$PATTERN' -destdir '$DESTDIR' | grep -v ^#"

    # list the remote dir
    cmdout=($(bash -c "rd adhoc -p $RUNDECK_PROJECT -f -F '$REMOTE_NODE' -- find $DESTDIR | grep -v ^# | sort"))
    expout=($DESTDIR ${DESTDIR}afile.txt ${DESTDIR}files2 ${DESTDIR}files2/cfile.xml)
    echo "${cmdout[@]}"
    # There should be one line for the uname response.
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


it_should_copy_recursive_pattern_dirs_remotely() {

    # load job file
    bash -c "rd jobs load -p $RUNDECK_PROJECT --format yaml -f /tests/rundeck/test-filecopy-job3.yaml"
    JOBID="3b09625a-8371-4d6c-9c04-9e8e90084549"


    # Parse lines into array elements.
    IFS=$'\n\t'

    SRCDIR=/tests/rundeck/files1/
    DESTDIR=/tmp/test-files4/
    PATTERN=**/files2
    # Run the uname command across the nodes tagged 'adhoc'. Should be two nodes.
    bash -c "rd run -i $JOBID -p $RUNDECK_PROJECT -f -F '$REMOTE_NODE' -- -sourcedir '$SRCDIR' -pattern '$PATTERN' -destdir '$DESTDIR' | grep -v ^#"

    # list the remote dir
    cmdout=($(bash -c "rd adhoc -p $RUNDECK_PROJECT -f -F '$REMOTE_NODE' -- find $DESTDIR | grep -v ^# | sort"))
    expout=($DESTDIR ${DESTDIR}files2 ${DESTDIR}files2/cfile.xml)
    # There should be one line for the uname response.
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

it_should_copy_pattern_files_remotely() {

    # load job file
    bash -c "rd jobs load -p $RUNDECK_PROJECT --format yaml -f /tests/rundeck/test-filecopy-job2.yaml"
    JOBID="3b09625a-8371-4d6c-9c04-9e8e90084548"


    # Parse lines into array elements.
    IFS=$'\n\t'

    SRCDIR=/tests/rundeck/files1/
    DESTDIR=/tmp/test-files2/
    PATTERN=*.txt
    # Run the uname command across the nodes tagged 'adhoc'. Should be two nodes.
    bash -c "rd run -i $JOBID -p $RUNDECK_PROJECT -f -F '$REMOTE_NODE' -- -sourcedir '$SRCDIR' -pattern '$PATTERN' -destdir '$DESTDIR' | grep -v ^#"

    # list the remote dir
    cmdout=($(bash -c "rd adhoc -p $RUNDECK_PROJECT -f -F '$REMOTE_NODE' -- find '$DESTDIR' | grep -v ^# | sort"))
    expout=($DESTDIR ${DESTDIR}afile.txt ${DESTDIR}btest.txt)
    # There should be one line for the uname response.
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