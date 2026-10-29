#
# Overrides functions for testing with local circle cli.

echo "-> Overriding functions with local versions <-"

rundeck_docker_push() {
    echo "!!! [rundeck_docker_push] ran but is DISABLED !!!"
}

rundeck_test_images_build_push() {
    echo "!!! [rundeck_test_images_build_push] ran but is DISABLED !!!"
}

# No registry locally: the test images are built on demand (testdeck_build_rdtest / compose build:).
rundeck_pull_rdtest_images() {
    echo "!!! [rundeck_pull_rdtest_images] ran but is DISABLED, images are built locally !!!"
}

rundeck_pull_ansible_image() {
    echo "!!! [rundeck_pull_ansible_image] ran but is DISABLED, image is built locally !!!"
}

rundeck_pull_oss_image() {
    echo "!!! [rundeck_pull_oss_image] ran but is DISABLED, image is built locally !!!"
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
