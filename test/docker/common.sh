#!/bin/bash

set -euo pipefail

export CLI_VERS=${CLI_VERS:-2.2.1-1}

# Builds the rdtest:latest image which is required as a base image by many test images.
# In CI test jobs the image is pre-built by the Build job; this is a no-op when the image exists
# unless RDTEST_DOCKER_BUILD_OPTS is set (e.g., --no-cache in the Build job where DLC may restore
# a stale rdtest:latest from an earlier pipeline).
build_rdtest_docker(){
  if [[ -z "${RDTEST_DOCKER_BUILD_OPTS:-}" ]] && docker image inspect rdtest:latest >/dev/null 2>&1; then
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
		${RDTEST_DOCKER_BUILD_OPTS:---cache-from rdtest:latest} \
		--build-arg CLI_VERS=$CLI_VERS \
		$rdeck_dock_img_dir
}

# Builds the rundeckansible:latest image (on top of rdtest:latest) used by docker-compose-ansible-test.yaml.
# In CI test jobs the image is pulled from the Build job, so the build is skipped when it exists
# unless RDTEST_DOCKER_BUILD_OPTS is set (e.g., --no-cache in the Build job). Outside CI it is
# always rebuilt, so local edits to dockers/rundeckansible are picked up.
build_rundeckansible_docker(){
  if [[ -z "${RDTEST_DOCKER_BUILD_OPTS:-}" && "${CIRCLECI:-}" == "true" ]] && docker image inspect rundeckansible:latest >/dev/null 2>&1; then
    echo "rundeckansible:latest already pulled — skipping rebuild"
    return 0
  fi

  docker build \
		-t rundeckansible:latest \
		${RDTEST_DOCKER_BUILD_OPTS:-} \
		dockers/rundeckansible/
}