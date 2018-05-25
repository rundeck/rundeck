#!/usr/bin/env roundup
#

: ${RUNDECK_USER?"environment variable not set."} 
: ${RUNDECK_PROJECT?"environment variable not set."}

# Let's get started
# -----------------

# Helpers
# ------------

#. ./include.sh 


# The Plan
# --------
describe "project: rundeck project should exist: $RUNDECK_PROJECT"


it_should_list_project_nodes() {
    bash -c "rd nodes list -p $RUNDECK_PROJECT"
}
it_should_get_system_imfo() {
    out=( $(bash -c "RD_FORMAT=json rd system info | jq -r .executions.executionMode") )
    test "$out" = 'active'
}
