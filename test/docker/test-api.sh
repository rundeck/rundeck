#!/bin/bash

set -euo pipefail

. common.sh

# Sets the compose file to use for test run
# Different compose files used for different environments
export DOCKER_COMPOSE_SPEC=${DOCKER_COMPOSE_SPEC:-docker-compose-api-test.yml}
export SETUP_TEST_PROJECT=test

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
docker-compose -f $DOCKER_COMPOSE_SPEC up -d

echo "up completed, running tests..."

set +e

docker-compose -f $DOCKER_COMPOSE_SPEC exec -T --user rundeck rundeck1 \
	bash scripts/run_api_tests.sh /home/rundeck/api_test

EC=$?
echo "run_tests.sh finished with: $EC"

docker-compose -f $DOCKER_COMPOSE_SPEC logs

# Stop and clean all
docker-compose -f $DOCKER_COMPOSE_SPEC down --volumes --remove-orphans

rm -rf dockers/rundeck/api_test/src dockers/rundeck/api_test/api

exit $EC
