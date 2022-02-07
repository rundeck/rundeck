#!/bin/bash
#/ trigger local ci test run

set -euo pipefail
IFS=$'\n\t'
readonly ARGS=("$@")
DOCKER_DIR=$PWD/test/docker

usage() {
      grep '^#/' <"$0" | cut -c4- # prints the #/ lines above as usage info
}
die(){
    echo >&2 "$@" ; exit 2
}

check_args(){
    if [ ${#ARGS[@]} -gt 0 ] ; then
        DOCKER_DIR=$1
    fi
}

copy_jar(){
    local FARGS=("$@")
    local DIR=${FARGS[0]}
    local buildJar=( $PWD/rundeckapp/build/libs/*.war )
    echo "Testing against ${buildJar[0]}"
    test -d $DIR || mkdir -p $DIR
    cp ${buildJar[0]} $DIR/rundeck-launcher.war
}

run_tests(){
    local FARGS=("$@")
    local DIR=${FARGS[0]}

    cd $DIR
    bash $DIR/test-api.sh
}

run_docker_test(){
    local FARGS=("$@")
    local DIR=${FARGS[0]}
    ( copy_jar $DIR ) || die "Failed to copy jar"
    run_tests $DIR
}


main() {
    check_args
    run_docker_test  $DOCKER_DIR
}
main

