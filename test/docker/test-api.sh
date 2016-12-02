#!/bin/bash

export DOCKER_COMPOSE_SPEC=docker-compose-api-test.yml
export RUNDECK_VERSION=${RUNDECK_VERSION:-2.6.9}
export LAUNCHER_URL=${LAUNCHER_URL:-http://dl.bintray.com/rundeck/rundeck-maven/rundeck-launcher-${RUNDECK_VERSION}.jar}
export CLI_DEB_URL=${CLI_DEB_URL:-https://dl.bintray.com/rundeck/rundeck-deb}
export CLI_VERS=${CLI_VERS:-0.1.27-1}
export SETUP_TEST_PROJECT=test

if [ -f rundeck-launcher.jar ] ; then
	mv rundeck-launcher.jar dockers/rundeck/data/
fi

if [ -f rd.deb ] ; then
	mv rd.deb dockers/rundeck/data/
fi

# setup test dirs
mkdir dockers/rundeck/api_test
cp -r ../src dockers/rundeck/api_test/
cp -r ../api dockers/rundeck/api_test/

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

docker-compose -f $DOCKER_COMPOSE_SPEC exec -T --user rundeck rundeck1 \
	bash scripts/run_api_tests.sh /home/rundeck/api_test $TEST_NAME

EC=$?
echo "run_tests.sh finished with: $EC"

docker-compose -f $DOCKER_COMPOSE_SPEC logs

# Stop and clean all
docker-compose -f $DOCKER_COMPOSE_SPEC down --volumes --remove-orphans

rm -rf dockers/rundeck/api_test/src dockers/rundeck/api_test/api

exit $EC
