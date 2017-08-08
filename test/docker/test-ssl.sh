#!/bin/bash

. common.sh

export DOCKER_COMPOSE_SPEC=docker-compose-ssl-test.yml

if [ -f rundeck-launcher.jar ] ; then
	mv rundeck-launcher.jar dockers/rundeck/data/
fi

if [ -f rd.deb ] ; then
	mv rd.deb dockers/rundeck/data/
fi

# tickle installer for it to rebuild
#date > dockers/rundeck/rundeckpro-installer/build_control

# create base image for rundeck
docker build \
	-t rdtest \
	--build-arg LAUNCHER_URL=$LAUNCHER_URL \
	--build-arg CLI_DEB_URL=$CLI_DEB_URL \
	--build-arg CLI_VERS=$CLI_VERS \
	dockers/rundeck

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
	bash scripts/run_tests.sh /tests/ssltests /tests/run-tests.sh testproj1

EC=$?
echo "run_tests.sh finished with: $EC"

docker-compose -f $DOCKER_COMPOSE_SPEC logs

# Stop and clean all
docker-compose -f $DOCKER_COMPOSE_SPEC down --volumes --remove-orphans

exit $EC
