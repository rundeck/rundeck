#!/bin/bash


wait_for_start(){
	local LOGFILE="${1:-/var/log/rundeck/service.log}"

	echo "started rundeck"
	
	# Wait for server to start
	local SUCCESS_MSG="Grails application running"
	local MAX_ATTEMPTS=30
	local SLEEP=10
	echo "Waiting for Rundeck to start. This will take about 2 minutes... "
	declare -i count=0
	while (( count <= MAX_ATTEMPTS ))
	do
	    if ! [ -f "$LOGFILE" ] 
	    then  echo "Waiting. hang on..."; # output a progress character.
	    elif ! grep "${SUCCESS_MSG}" "$LOGFILE" ; then
	      echo "Still working. hang on..."; # output a progress character.
	    else  break; # found successful startup message.
	    fi
	    (( count += 1 ))  ; # increment attempts counter.
	    (( count == MAX_ATTEMPTS )) && {
	        echo >&2 "FAIL: Reached max attempts to find success message in logfile. Exiting."
	        exit 1
	    }
	    tail -n 5 "$LOGFILE"
	    service rundeckd status || {
	        echo >&2 "FAIL: rundeckd is not running. Exiting."
	        exit 1
	    }
	    sleep "$SLEEP"; # wait before trying again.

	done
	echo "Rundeck started successfully!!"
	
}
entry_start(){
	service rundeckd start
	wait_for_start /var/log/rundeck/service.log


	if [ "$1" = "-test" ] ; then
		shift
		. /init-tests.sh
		test_all
	fi

	exec "$@"
}