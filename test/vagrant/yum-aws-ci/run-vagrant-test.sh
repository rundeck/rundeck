#!/bin/bash


#Note: due to a bug/issue with vagrant: https://github.com/mitchellh/vagrant-aws/issues/72
#   the vagrant up step might fail, when cloud-init doesn't finish before vagrant rsync starts.
#   The sleep and subsequent vagrant provision should then succeed

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