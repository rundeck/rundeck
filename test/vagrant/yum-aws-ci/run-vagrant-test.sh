#!/bin/bash

set -x

vagrant up --provider aws --no-provision

xit=$?
if [ $xit != 0 ] ; then
    echo "Failed vagrant up, sleeping 30 to attempt provision"
    sleep 30
fi

vagrant provision
xit=$?

vagrant destroy -f

exit $xit