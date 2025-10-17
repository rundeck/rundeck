#!/bin/bash

set -e

#TODO: use roundup or something

test_status(){
	echo "test_status"
	/etc/init.d/rundeckd status | grep 'is running' || { echo "FAILED" ; exit 2 ; }
	echo "OK"
}

test_start_twice(){
	echo "test_start_twice"
	/etc/init.d/rundeckd start | grep 'Already started' || { echo "FAILED" ; exit 2 ; }

	echo "OK"
}

test_all(){
	test_status
	test_start_twice
}