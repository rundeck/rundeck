#
# Overrides functions for testing with local circle cli.

echo "-> Overriding functions with local versions <-"

rundeck_docker_push() {
    echo "!!! [rundeck_docker_push] ran but is DISABLED !!!"
}

testdeck_push_rdtest() {
    echo "!!! [testdeck_push_rdtest] ran but is DISABLED !!!"
}

testdeck_pull_rdtest() {
    echo "!!! [testdeck_pull_rdtest] ran but OVERRIDEN locally !!!"
    mkdir -p "${RUNDECK_WAR_DIR}"
    cp -pv /home/circleci/rlibs/* "${RUNDECK_WAR_DIR}"

}

testdeck_pull_rundeck() {
    echo "!!! [testdeck_pull_rdtest] ran but OVERRIDEN locally !!!"
    docker tag "${ECR_REPO}:${ECR_IMAGE_TAG}" rundeck/testdeck
}


fetch_ci_shared_resources() {
    echo "!!! [fetch_ci_shared_resources] ran but OVERRIDEN locally !!!"

    # install gpg keys.
    mkdir -p "${HOME}/.gnupg"
    cp -pv /home/circleci/ciresources/* "${HOME}/.gnupg/"
    chmod -R 700 "${HOME}/.gnupg"

    ls -la "${HOME}/.gnupg/"

    ls -la /home/circleci/.gnupg/

}

packaging_setup() {
    echo "!!! [packaging_setup] ran but OVERRIDEN locally !!!"
    mkdir -p "${RUNDECK_WAR_DIR}"
    cp -pv /home/circleci/rlibs/* "${RUNDECK_WAR_DIR}"

    fetch_ci_shared_resources

    # Setup submodule repository
#    sudo sed -i 's/git@github.com:/https:\/\/github.com\//' .gitmodules

#    git submodule update --init --recursive --remote

}
