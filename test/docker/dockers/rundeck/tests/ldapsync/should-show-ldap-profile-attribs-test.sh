#!/usr/bin/env roundup
#
# Let's get started
# -----------------

# Helpers
# ------------

#. ./include.sh 


# The Plan
# --------
describe "project: check ldap attribs show up in user info"
#export RD_DEBUG=3

it_should_login_and_show_user_info() {

    # load job file
    export RD_USER=syncer
    export RD_PASSWORD=syncer
    
    unset RD_TOKEN
    env | grep RD_
    set +e
    bash -c "rd users info"  > test.output 2>&1
    set -e
    grep -q 'Login: \[syncer\]' test.output
    grep -q 'First Name: \[Syncer\]' test.output
    grep -q 'Last Name: \[Swim\]' test.output
    grep -q 'Email: \[syncer\@swim.com\]' test.output
    exit 0
}
