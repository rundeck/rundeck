#!/bin/bash

set -euo pipefail

. common.sh

export DOCKER_COMPOSE_SPEC=${DOCKER_COMPOSE_SPEC:-docker-compose-pam-test.yaml}
export CLI_VERS=1.0.29-1
DEBUG_RD_SERVER=${DEBUG_RD_SERVER:-''}

if [ -f rundeck-launcher.war ] ; then
	mv rundeck-launcher.war dockers/rundeck/data/
fi

if [ -f rd.deb ] ; then
	mv rd.deb dockers/rundeck/data/
fi

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
	bash scripts/run_tests.sh /tests/pamtests

EC=$?

docker-compose -f $DOCKER_COMPOSE_SPEC logs

if [ "$DEBUG_RD_SERVER" != true ] ; then
  # Stop and clean all
  docker-compose -f $DOCKER_COMPOSE_SPEC down --volumes --remove-orphans
else
  echo "Skipping Containers removal for debug..."
fi

echo "run_tests.sh finished with: $EC"
exit $EC
