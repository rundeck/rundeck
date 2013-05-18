#!/bin/bash

if [ $# -lt 1 ] ; then
    echo "Usage: $0 <vagrant-test-name>"
    exit 2
fi

DIR=$1
cd $DIR

if [ -f ./run-vagrant-test.sh ] ; then
    exec sh ./run-vagrant-test.sh
fi

vagrant up
xit=$?
if [ $xit != 0 ] ; then
    echo "FAIL: Vagrant test $DIR failed: $!"
else
    echo "OK: $DIR"
fi
vagrant destroy -f

exit $xit