#!/bin/bash
#/ run vagrant tests
#/ usage: [vmname [vname2...]]
set -euo pipefail
IFS=$'\n\t'
readonly VAGRANT='vagrant'
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
vagrant_aws_noprovision(){
    local FARGS=("$@")
    echo "Running $VAGRANT up --provider aws --no-provision ${FARGS[0]}..."
    $VAGRANT up --provider aws --no-provision ${FARGS[0]}
}

vagrant_provision(){
    local FARGS=("$@")
    echo "Running $VAGRANT provision ${FARGS[0]}..."
    $VAGRANT provision ${FARGS[0]}
}

vagrant_destroy(){
    local FARGS=("$@")
    echo "Running $VAGRANT destroy -f ${FARGS[0]}..."
    $VAGRANT destroy -f ${FARGS[0]}
}

#Note: due to a bug/issue with vagrant: https://github.com/mitchellh/vagrant-aws/issues/72
#   the vagrant up step might fail, when cloud-init doesn't finish before vagrant rsync starts.
#   The sleep and subsequent vagrant provision should then succeed

run_vagrant_provision_loop(){
    local FARGS=("$@")
    local vmname=${FARGS[0]}

    sleep 30
    # loop over the provision call until either it works or we've given up (10sec sleep, 12 tries = ~ 2-3 minutes)
    local count=0
    local max=1
    while :
    do
        set +e
        vagrant_provision $vmname
        xit=$?
        set -e
        echo "VAGRANT provision exited with $xit"

        if [ $xit -eq 0 ]; then
            vagrant_destroy $vmname
            exit 0
        fi
        let count=count+1
        if [ $count -gt $max ]; then
            echo "Reached maximum retry for provisioning step, failing with $xit"
            vagrant_destroy $vmname
            exit $xit
        fi
        sleep 30
    done

    vagrant_destroy $vmname
    exit $xit
}
pre_run(){
    if [ -f ./pre-run-vagrant-test.sh ] ; then
        bash ./pre-run-vagrant-test.sh `pwd`/keys
    fi
}
main(){
    check_args
    pre_run
    local xit=0
    for vmname in "${ARGS[@]}" ; do 
        set +e
        vagrant_aws_noprovision $vmname
        xit=$?
        set -e
    done

    local provxit=0
    for vmname in "${ARGS[@]}" ; do 
        set +e
        vagrant_provision $vmname
        xit=$?
        set -e
        if [ $xit -ne 0 ]; then
            echo "vagrant provision failed, retrying in 30"
            sleep 30
            set +e
            vagrant_provision $vmname
            xit=$?
            set -e
        fi
        if [ $xit -ne 0 ]; then
            echo "vagrant provision failed for $vmname"
            provxit=$xit
        fi
    done
    local skip=${SKIP_DESTROY:-false}
    if [ "${skip}" != "true" ] ; then
        for vmname in "${ARGS[@]}" ; do 
            set +e
            vagrant_destroy $vmname
            set -e
        done
    else
        echo "Skipping ec2 instance destroy for ${ARGS[@]}"
    fi
    exit $provxit
}
main