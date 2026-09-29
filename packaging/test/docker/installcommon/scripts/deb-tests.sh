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
	service rundeckd start | grep 'already running' || { echo "FAILED" ; exit 2 ; }

	echo "OK"
}

test_encryption_password(){
	echo "test_encryption_password"
	local CONFIG=/etc/rundeck/rundeck-config.properties
	grep -q 'default\.encryption\.password' "$CONFIG" && { echo "FAILED" ; exit 2 ; }
	local PASS
	PASS=$(grep -E '^rundeck\.storage\.converter\.[0-9]+\.config\.password=' "$CONFIG" | head -1 | cut -d= -f2)
	echo "$PASS" | grep -qE '^[0-9a-f]{32}$' || { echo "FAILED" ; exit 2 ; }
	echo "OK"
}

test_all(){
	test_status
	test_start_twice
	test_encryption_password
}