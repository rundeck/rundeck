#!/bin/bash
set -e


testdeck_build_rdtest() {

    copy_rundeck_war test/docker/rundeck-launcher.war

    # Run build.
    (
        set -e
        cd test/docker/
        source common.sh
        build_rdtest_docker
    )

    docker tag rdtest:latest "${ECR_TEST_REPO}:${ECR_IMAGE_TAG}"

}

testdeck_push_rdtest() {

    # Tag and push to be used as cache source in later test builds
    docker_ecr_login
    docker push "${ECR_TEST_REPO}:${ECR_IMAGE_TAG}"

}

testdeck_pull_rdtest() {
   docker_ecr_login
   docker pull "${ECR_TEST_REPO}:${ECR_IMAGE_TAG}"
   docker tag "${ECR_TEST_REPO}:${ECR_IMAGE_TAG}" rdtest:latest
}


testdeck_pull_rundeck() {
    docker_ecr_login

    docker pull "${ECR_REPO}:${ECR_IMAGE_TAG}"
    docker tag "${ECR_REPO}:${ECR_IMAGE_TAG}" rundeck/testdeck
}

testdeck_selenium_test() {
    bin/deck test -s selenium \
      -u http://localhost:4440 \
      --headless \
      --provision \
      -t TEST_TOKEN \
      --clusterConfig ./lib/compose/single \
      --image rundeck/testdeck \
      -j="--verbose --colors"

}