#!/bin/bash

set -euo pipefail

. common.sh

TOMCAT_TAG="${1}"

# Sets the compose file to use for test run
# Different compose files used for different environments
export DOCKER_COMPOSE_SPEC=${DOCKER_COMPOSE_SPEC:-docker-compose-tomcat-test.yml}
export SETUP_TEST_PROJECT=test
DEBUG_RD_SERVER=${DEBUG_RD_SERVER:-''}

if [ -f rundeck-launcher.war ] ; then
	mv rundeck-launcher.war dockers/tomcat/data/
fi

if [ -f rd.deb ] ; then
	mv rd.deb dockers/tomcat/data/
fi

# setup test dirs
cp -r ../src dockers/tomcat/api_test/
cp -r ../api dockers/tomcat/api_test/

# tickle installer for it to rebuild
#date > dockers/rundeck/data/build_control

# create base image for rundeck
rdeck_dock_img_dir='dockers/tomcat'
echo "Building Docker image from: $(pwd)/${rdeck_dock_img_dir}"
docker build \
    -t rd-tomcat:latest \
    --build-arg TOMCAT_TAG=$TOMCAT_TAG \
    --build-arg CLI_VERS=$CLI_VERS \
   "$rdeck_dock_img_dir"

# clean up docker env
docker-compose -f $DOCKER_COMPOSE_SPEC down --volumes --remove-orphans

set -e
# re-build docker env
docker-compose -f $DOCKER_COMPOSE_SPEC build


# run docker
echo "Running compose file: $(pwd)/${DOCKER_COMPOSE_SPEC}"
docker-compose -f $DOCKER_COMPOSE_SPEC up -d

echo "up completed, running tests..."

set +e

docker-compose -f $DOCKER_COMPOSE_SPEC exec -T rundeck1 \
	bash /scripts/run_api_tests.sh /api_test

EC=$?

docker-compose -f $DOCKER_COMPOSE_SPEC logs

if [ "$DEBUG_RD_SERVER" != true ] ; then
  # Stop and clean all
  docker-compose -f $DOCKER_COMPOSE_SPEC down --volumes --remove-orphans

  rm -rf dockers/tomcat/api_test/src dockers/tomcat/api_test/api
else
  echo "Skipping Containers removal for debug..."
fi

echo "run_tests.sh finished with: $EC"
exit $EC