#!/usr/bin/env bash
#/ run all tests
#/ usage: dir
set -euo pipefail
IFS=$'\n\t'
readonly ARGS=("$@")
usage() {
      grep '^#/' <"$0" | cut -c4- # prints the #/ lines above as usage info
}

check_args(){
    if [ ${#ARGS} -lt 1 ] ; then
        usage
        exit 2
    fi
}

option_check() {
    if (( "$1" < 2 ))
    then echo >&2 "USAGE: option requires argument: ${2:-}" ; return 1 ;
    else return 0
    fi
}

option_parse() {
    while (( "$#" > 0 ))
    do
        OPT="$1"
        case "$OPT" in

            --remote-node) option_check $# $1; export REMOTE_NODE=$2 ; shift ;;
            --rdeck-base) option_check $# $1; export RDECK_BASE=$2 ; shift ;;
            --rundeck-user) option_check $# $1; export RUNDECK_USER=$2 ; shift ;;
            --rundeck-project) option_check $# $1; export RUNDECK_PROJECT=$2 ; shift ;;

            # help option
            -|--*?)
                echo >&2 "USAGE: test-all --rundeck-ipaddr <> ...."
                exit 2
                ;;
            # end of options, just arguments left
            *)
              break
        esac
        shift
    done

    : ${RDECK_BASE?" option not set."}
    : ${RUNDECK_USER?" option not set."}
    : ${REMOTE_NODE?" option not set."}
    : ${RUNDECK_PROJECT?" option not set."}
}


run_roundup(){
    cd /tests/rundeck
    ROUNDUP=/tests/roundup
    $ROUNDUP
    exit $?
}
main(){
    echo  "========================================================="
    echo  " >>> System Test Plans"
    echo  "========================================================="
    option_parse "$@"
    run_roundup
}
main "$@"