#!/usr/bin/env bash

script_block() {
    NAME="${1}"

    # Remove block name from arg array
    shift

    travis_fold start "${NAME}"
        travis_time_start
            eval "${@}"
            ret=$?
        travis_time_finish
    travis_fold end "${NAME}"
    return $RET
}

export -f script_block