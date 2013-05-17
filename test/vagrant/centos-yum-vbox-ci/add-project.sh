#!/usr/bin/env bash

die() {
   [[ $# -gt 1 ]] && { 
	    exit_status=$1
        shift        
    } 
    local -i frame=0; local info= 
    while info=$(caller $frame)
    do 
        local -a f=( $info )
        [[ $frame -gt 0 ]] && {
            printf >&2 "ERROR in \"%s\" %s:%s\n" "${f[1]}" "${f[2]}" "${f[0]}"
        }
        (( frame++ )) || :; #ignore increment errors (i.e., errexit is set)
    done

    printf >&2 "ERROR: $*\n"

    exit ${exit_status:-1}
}

trap 'die $? "*** add-project failed. ***"' ERR
set -o nounset -o pipefail


# Create an example project and 
rd-project -a create -p example


# Run simple commands to double check.
# Print out the available nodes.
# Fire off a command.
dispatch -p example
dispatch -p example -f -- whoami

