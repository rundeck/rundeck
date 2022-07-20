#!/bin/bash

set -euo pipefail

export RUNDECK_VERSION=${RUNDECK_VERSION:-2.9.2}
export CLI_VERS=${CLI_VERS:-1.4.3-1}

# Builds the rdtest:latest image which is required as a base image by many test images
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
		--cache-from rdtest:latest \
		--build-arg CLI_VERS=$CLI_VERS \
		dockers/rundeck
}