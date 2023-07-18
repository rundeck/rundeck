#!/bin/bash

 echo "test path"
 pwd
 echo "----"

 set -euo pipefail

 . common.sh

  # Sets the compose file to use for test run
  # Different compose files used for different environments
 export DOCKER_COMPOSE_SPEC=${DOCKER_COMPOSE_SPEC:-docker-compose-plugin-blocklist.yaml}
 export SETUP_TEST_PROJECT=test
 DEBUG_RD_SERVER=${DEBUG_RD_SERVER:-''}

 if [ -f rundeck-launcher.war ] ; then
  mv rundeck-launcher.war dockers/rundeck/data/
 fi

 if [ -f rd.deb ] ; then
 	mv rd.deb dockers/rundeck/data/
 fi

 # Most test images require rdtest:lastest as a base image
 build_rdtest_docker

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

 docker-compose -f $DOCKER_COMPOSE_SPEC exec -T --user rundeck rundeck1 \
 	bash scripts/run-plugin-blocklist-api-tests.sh api_test blocklist-*.sh

 EC=$?

 docker-compose -f $DOCKER_COMPOSE_SPEC logs

 if [ "$DEBUG_RD_SERVER" != true ] ; then
   # Stop and clean all
   docker-compose -f $DOCKER_COMPOSE_SPEC down --volumes --remove-orphans

   rm -rf dockers/rundeck/api_test/src dockers/rundeck/api_test/api
 else
   echo "Skipping Containers removal for debug..."
 fi

 echo "run_tests.sh finished with: $EC"
 exit $EC