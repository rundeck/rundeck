#!/bin/bash

export DOCKER_COMPOSE_SPEC=docker-compose-multinode-test.yml
export RUNDECK_VERSION=${RUNDECK_VERSION:-2.6.9}
export LAUNCHER_URL=${LAUNCHER_URL:-http://dl.bintray.com/rundeck/rundeck-maven/rundeck-launcher-${RUNDECK_VERSION}.jar}

if [ -f rundeck-launcher.jar ] ; then
	#LAUNCHER_URL=file:/home/rundeck/rundeck-launcher.jar
    mv rundeck-launcher.jar dockers/rundeck/data/
fi


# tickle installer for it to rebuild
#date > dockers/rundeck/rundeckpro-installer/build_control

# clean up docker env
docker-compose -f $DOCKER_COMPOSE_SPEC down --volumes --remove-orphans

set -e
# re-build docker env
docker-compose -f $DOCKER_COMPOSE_SPEC build


# run docker
docker-compose -f $DOCKER_COMPOSE_SPEC up -d

echo "up completed, running tests..."

set +e

docker-compose -f $DOCKER_COMPOSE_SPEC exec -T --user rundeck rundeck1 bash scripts/run_tests.sh

EC=$?
echo "run_tests.sh finished with: $EC"

# Stop and clean all
docker-compose -f $DOCKER_COMPOSE_SPEC down --volumes --remove-orphans

exit $EC
