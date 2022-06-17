#!/bin/bash

set -e

#TODO: use roundup or something

test_status(){
	echo "test_status"
	service rundeckd status | grep 'is running' || { echo "FAILED" ; exit 2 ; }
	echo "OK"
}

test_start_twice(){
	echo "test_start_twice"
	service rundeckd start | grep 'Already started' || { echo "FAILED" ; exit 2 ; }

	echo "OK"
}

test_all(){
	test_status
	test_start_twice
}