#!/bin/bash

set -euo pipefail

export CLI_VERS=${CLI_VERS:-2.0.10-1}

# Builds the rdtest:latest image which is required as a base image by many test images.
# In CI the image is pre-built by a dedicated step; this is a no-op when the image exists.
build_rdtest_docker(){
  if docker image inspect rdtest:latest >/dev/null 2>&1; then
    echo "rdtest:latest already exists — skipping rebuild"
    return 0
  fi

  local rdeck_dock_img_dir='dockers/rundeck'
	if [ -f rundeck-launcher.war ] ; then
		mv rundeck-launcher.war dockers/rundeck/data/
	fi

	if [ -f rd.deb ] ; then
		mv rd.deb dockers/rundeck/data/
	fi

  echo "Building Docker image from: $(pwd)/${rdeck_dock_img_dir}"
	# create base image for rundeck
	docker build \
		-t rdtest:latest \
		--cache-from rdtest:latest \
		--build-arg CLI_VERS=$CLI_VERS \
		$rdeck_dock_img_dir
}