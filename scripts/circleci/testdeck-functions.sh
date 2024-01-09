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