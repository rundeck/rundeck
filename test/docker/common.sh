#!/bin/bash

set -euo pipefail

export RUNDECK_VERSION=${RUNDECK_VERSION:-2.9.2}
export LAUNCHER_URL=${LAUNCHER_URL:-http://dl.bintray.com/rundeck/rundeck-maven/rundeck-launcher-${RUNDECK_VERSION}.jar}
export CLI_DEB_URL=${CLI_DEB_URL:-https://dl.bintray.com/rundeck/rundeck-deb}
export CLI_VERS=${CLI_VERS:-1.0.15-1}

build_rdtest_docker(){
	if [ -f rundeck-launcher.war ] ; then
		mv rundeck-launcher.war dockers/rundeck/data/
	fi

	if [ -f rd.deb ] ; then
		mv rd.deb dockers/rundeck/data/
	fi

	# setup test dirs
	cp -r ../src dockers/rundeck/api_test/
	cp -r ../api dockers/rundeck/api_test/
	
	# tickle installer for it to rebuild
	#date > dockers/rundeck/data/build_control

	# create base image for rundeck
	docker build \
		-t rdtest:latest \
		--build-arg LAUNCHER_URL=$LAUNCHER_URL \
		--build-arg CLI_DEB_URL=$CLI_DEB_URL \
		--build-arg CLI_VERS=$CLI_VERS \
		dockers/rundeck

}