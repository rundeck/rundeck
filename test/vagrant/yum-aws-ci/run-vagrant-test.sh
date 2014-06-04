#!/bin/bash


#Note: due to a bug/issue with vagrant: https://github.com/mitchellh/vagrant-aws/issues/72
#   the vagrant up step might fail, when cloud-init doesn't finish before vagrant rsync starts.
#   The sleep and subsequent vagrant provision should then succeed

set -x

vagrant up --provider aws --no-provision

xit=$?
sleep 30
# loop over the provision call until either it works or we've given up (10sec sleep, 12 tries = ~ 2-3 minutes)
count=0
max=1
while :
do
    vagrant provision
    xit=$?
    if [ $xit -eq 0 ]; then
        vagrant destroy --force
        exit 0
    fi
    sleep 30
    let count++
    if [ $count -gt $max ]; then
        vagrant destroy --force
        exit $xit
    fi
done

vagrant destroy -f

exit $xit
