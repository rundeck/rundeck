#!/usr/bin/env roundup
#
# Let's get started
# -----------------

# Helpers
# ------------

#. ./include.sh 


# The Plan
# --------
describe "project: login using ldap credentials"


it_should_login_with_admin_creds() {

    # load job file
    export RD_USER=admin
    export RD_PASSWORD=admin
    # export RD_DEBUG=3
    unset RD_TOKEN
    env | grep RD_
    bash -c "rd projects list"
    
}

it_should_login_with_build_creds() {

    # load job file
    export RD_USER=build
    export RD_PASSWORD=build
    # export RD_DEBUG=3
    unset RD_TOKEN
    env | grep RD_
    bash -c "rd projects list"

}

it_should_login_with_deploy_creds() {

    # load job file
    export RD_USER=deploy
    export RD_PASSWORD=deploy
    # export RD_DEBUG=3
    unset RD_TOKEN
    env | grep RD_
    bash -c "rd projects list"

}
it_should_login_with_test_creds() {

    # load job file
    export RD_USER=test
    export RD_PASSWORD=test
    # export RD_DEBUG=3
    unset RD_TOKEN
    env | grep RD_
    bash -c "rd projects list"

}

it_should_fail_login_with_wrong_creds() {

    # load job file
    export RD_USER=build
    export RD_PASSWORD=notthepassword
    # export RD_DEBUG=3
    unset RD_TOKEN
    env | grep RD_
    set +e
    bash -c "rd projects list"
    local val=$?
    set -e
    [ "$val" -ne 0 ]
}
