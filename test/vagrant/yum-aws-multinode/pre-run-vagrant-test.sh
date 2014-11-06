#!/bin/bash
#/ prepare tests
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
gen_ssh_key(){
    local keysdir=$1
    local keyname=$2
    ssh-keygen -t rsa -N '' -f $keysdir/$keyname
    echo $keysdir/$keyname
}

main(){
    check_args
    local keysdir=${ARGS[0]}
    if [ -d $keysdir ] ; then
        rm -rf $keysdir
    fi
    mkdir -p $keysdir
    gen_ssh_key $keysdir "id_rsa"
}

main