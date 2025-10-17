#!/bin/bash

set -e

#TODO: use roundup or something

test_status(){
	echo "test_status"
	/etc/rc.d/init.d/rundeckd status | grep 'is running' || { echo "FAILED" ; exit 2 ; }
	echo "OK"
}

test_start_twice(){
	echo "test_start_twice"
	/etc/rc.d/init.d/rundeckd start | grep 'Already started' || { echo "FAILED" ; exit 2 ; }

	echo "OK"
}

test_all(){
	test_status
	test_start_twice
}