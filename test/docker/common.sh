#!/bin/bash

set -euo pipefail

export RUNDECK_VERSION=${RUNDECK_VERSION:-2.9.2}
export CLI_VERS=${CLI_VERS:-2.0.7-1}

# Builds the rdtest:latest image which is required as a base image by many test images
build_rdtest_docker(){
  local rdeck_dock_img_dir='dockers/rundeck'
	if [ -f rundeck-launcher.war ] ; then
		mv rundeck-launcher.war dockers/rundeck/data/
	fi

	if [ -f rd.deb ] ; then
		mv rd.deb dockers/rundeck/data/
	fi

	# tickle installer for it to rebuild
	#date > dockers/rundeck/data/build_control

  echo "Building Docker image from: $(pwd)/${rdeck_dock_img_dir}"
	# create base image for rundeck
	docker build \
		-t rdtest:latest \
		--cache-from rdtest:latest \
		--build-arg CLI_VERS=$CLI_VERS \
		$rdeck_dock_img_dir
}