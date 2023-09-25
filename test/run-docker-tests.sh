#!/bin/bash
#/ trigger local ci test run

set -euo pipefail
IFS=$'\n\t'
readonly ARGS=("$@")
DOCKER_DIR=$PWD/test/docker
if [ ${#ARGS[@]} -gt 0 ] ; then
    DOCKER_DIR=$1
fi

usage() {
      grep '^#/' <"$0" | cut -c4- # prints the #/ lines above as usage info
}
die(){
    echo >&2 "$@" ; exit 2
}
copy_jar(){
    local FARGS=("$@")
    local DIR=${FARGS[0]}
    
    local buildJar=( $PWD/rundeckapp/build/libs/*[A-Z0-9].war )
    echo "Testing against ${buildJar[0]}"
    test -d $DIR || mkdir -p $DIR
    cp ${buildJar[0]} $DIR/rundeck-launcher.war
}

copy_jar "$DOCKER_DIR"  || die "Failed to copy jar"
cd "$DOCKER_DIR"
bash "$DOCKER_DIR"/test.sh

