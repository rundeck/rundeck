#!/bin/bash
#/ This runs the test in directory specifed 
#/ 
#/ `usage: <vagrant-test-dir>`
#/ 
#/ If a "run-vagrant-test.sh" script exists in that directory, it is used instead of the default behavior.
#/ 
#/ Default behavior: 
# 
#/ 1. invoke "vagrant up"
#/ 2. wait for completion
#/ 3. invoke "vagrant destroy -f"
#/ 4. exit with completion exit code

set -euo pipefail
IFS=$'\n\t'
readonly VAGRANT='vagrant'
readonly ARGS=("$@")

usage() {
    grep '^#/' <"$0" | cut -c4- # prints the #/ lines above as usage info
}

# run the subdirectory's test script if it exists
run_specific_script(){
    local FARGS=("$@")
    local dir=${FARGS[0]}
    if [ -f $dir/run-vagrant-test.sh ] ; then
        cd $dir 
        set -- "${FARGS[@]}"
        shift
        exec bash ./run-vagrant-test.sh "$@"
    fi
}

# run vagrant up, force destroy, then exit with its result code
run_vagrant(){
    local dir=$1
    cd $dir
    
    set +e
    $VAGRANT up
    local xit=$?
    local reason=$!
    set -e
    
    if [ $xit != 0 ] ; then
        echo "FAIL: Vagrant test $dir failed: $reason"
    else
        echo "OK: $dir"
    fi
    $VAGRANT destroy -f

    exit $xit
}
check_args(){
    if [ ${#ARGS[@]} -lt 1 ] ; then
        usage
        exit 2
    fi
}

main() {
    check_args

    local dir=${ARGS[0]}

    run_specific_script  "${ARGS[@]}"

    run_vagrant "${ARGS[@]}"
}
main

