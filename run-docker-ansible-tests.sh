#!/bin/bash
#/ trigger local ci test run


. rd_versions.sh


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
	local -a VERS=( $( rd_get_version ) )
	local JAR=rundeck-launcher-${VERS[0]}-${VERS[2]}.jar
	local buildJar=$PWD/rundeck-launcher/launcher/build/libs/$JAR
	test -f $buildJar || die "Jar file not found $buildJar"
	mkdir -p $DIR
	cp $buildJar $DIR/rundeck-launcher.jar
	echo $DIR/$JAR
}
run_tests(){
	local FARGS=("$@")
	local DIR=${FARGS[0]}

	cd $DIR
	bash $DIR/test-ansible.sh
}
run_docker_test(){
	local FARGS=("$@")
	local DIR=${FARGS[0]}
	local launcherJar=$( copy_jar $DIR ) || die "Failed to copy jar"
	run_tests $DIR
}


main() {
    check_args
    run_docker_test  $DOCKER_DIR
}
main

