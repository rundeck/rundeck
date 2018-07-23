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
#export RD_DEBUG=3

it_should_login_with_admin_creds() {

    # load job file
    export RD_USER=admin
    export RD_PASSWORD=admin
    
    unset RD_TOKEN
    env | grep RD_
    bash -c "rd projects list"
    
}

it_should_login_with_build_creds() {

    # load job file
    export RD_USER=build
    export RD_PASSWORD=build
    
    unset RD_TOKEN
    env | grep RD_
    bash -c "rd projects list"

}

it_should_login_with_deploy_creds() {

    # load job file
    export RD_USER=deploy
    export RD_PASSWORD=deploy
    
    unset RD_TOKEN
    env | grep RD_
    bash -c "rd projects list"

}
it_should_login_with_test_creds() {

    # load job file
    export RD_USER=test
    export RD_PASSWORD=test
    
    unset RD_TOKEN
    env | grep RD_
    bash -c "rd projects list"

}

it_should_fail_login_with_wrong_creds() {

    # load job file
    export RD_USER=build
    export RD_PASSWORD=notthepassword
    
    unset RD_TOKEN
    env | grep RD_
    set +e
    bash -c "rd projects list"  > test.output 2>&1 
    local val=$?
    set -e
    grep -q 'Password Authentication failed' test.output
    [ "$val" -ne 0 ]
}
