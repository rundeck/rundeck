#!/usr/bin/env roundup
#
# Let's get started
# -----------------

# Helpers
# ------------

#. ./include.sh


# The Plan
# --------
describe "project: login using pam credentials"
#export RD_DEBUG=3

it_should_login_with_unixuser_creds() {

    # load job file
    export RD_USER=pamlogintest
    export RD_PASSWORD=pampwd123

    unset RD_TOKEN
    env | grep RD_
    bash -c "rd projects list"

}

it_should_fail_login_with_bad_password_creds() {

    # load job file
    export RD_USER=pamlogintest
    export RD_PASSWORD=badpassword

    unset RD_TOKEN
    env | grep RD_
    set +e
    bash -c "rd projects list"  > test.output 2>&1
    local val=$?
    set -e
    grep -q 'Password Authentication failed' test.output
    [ "$val" -ne 0 ]
}

it_should_fail_login_with_non_existing_user() {

    # load job file
    export RD_USER=ghostuser
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
