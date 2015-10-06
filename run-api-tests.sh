#!/bin/bash
#/ trigger local ci test run


. rd_versions.sh


set -euo pipefail
IFS=$'\n\t'
readonly ARGS=("$@")
TESTS=yes
WAIT=no
CLEAN=yes
SINGLE=no
TEST_NAME=""
START=yes

usage() {
      grep '^#/' <"$0" | cut -c4- # prints the #/ lines above as usage info
}
die(){
    echo >&2 "$@" ; exit 2
}

check_args(){
   : example to check args length

   if [ ${#ARGS[@]} -gt 0 ] ; then
	   for arg in "${ARGS[@]}"
	   do
	   		case $arg in
			-notest)
				TESTS=no
				;;
			-nostart)
				START=no
				;;
			-wait)
				WAIT=yes
				;;
			-noclean)
				CLEAN=no
				;;
			test-*)
				SINGLE=yes
				TEST_NAME="$arg"
				;;
			esac
	   done
   fi
}
wait_for(){
	local FARGS=("$@")
	local FILE=${FARGS[0]}
	local MATCH=${FARGS[1]}
	local count=0

    while true ; do
        if ! grep "$MATCH" $FILE
        then  printf >&2 "." ; # progress output.
        else  break; # successful message.
        fi
        count=$(( $count + 1 ));# increment attempts
        [ $count -eq 18 ] && {
            die "FAIL: Execeeded max attempts "
        }
        sleep 10
    done
}
start_rundeck(){
	local FARGS=("$@")
	local launcherJar=${FARGS[0]}
	java -Xmx1024m -XX:MaxPermSize=256m -jar $launcherJar 
	# ( bash -c 'sleep 30; echo done; sleep 600' > $outfile 2>&1 ) &
	
}
stop_rundeck(){
	local FARGS=("$@")
	local PID=${FARGS[0]}
	echo "stopping pid $PID..."
	kill -9 $PID || echo "PID already stopped $PID"
	trap "" EXIT SIGINT SIGTERM ERR
}
clean_dir(){
	local FARGS=("$@")
	local DIR=${FARGS[0]}

	if [ $CLEAN == "yes" -a -d $DIR ] ; then
		echo "Clean out existing test dir $DIR."
		rm -rf $DIR
	else
		echo "Not cleaning dir $DIR."
	fi

	mkdir -p $DIR
}
copy_jar(){
	local FARGS=("$@")
	local DIR=${FARGS[0]}
	local -a VERS=( $( rd_get_version ) )
	local JAR=rundeck-launcher-${VERS[0]}-${VERS[2]}.jar
	local buildJar=$PWD/rundeck-launcher/launcher/build/libs/$JAR
	test -f $buildJar || die "Jar file not found $buildJar"
	mkdir -p $DIR
	cp $buildJar $DIR/
	echo $DIR/$JAR
}
run_tests(){
	local FARGS=("$@")
	local DIR=${FARGS[0]}
	local SRC=${FARGS[1]}

	local HOST=$(hostname)
	if [ $SINGLE == "yes" ] ; then
		export TEST_NAME=$TEST_NAME
	fi
	RDECK_BASE=$DIR PATH=$PATH:$DIR/tools/bin  \
		 bash -c  $SRC/test/src/test.sh http://$HOST:4440 admin admin
}
run_ci_test(){
	local FARGS=("$@")

	local SRC=$PWD
	local DIR=$PWD/build/citest

	clean_dir $DIR

	local launcherJar=$( copy_jar $DIR )

	cd $DIR

	echo "Start Rundeck"
	local RDPID
	if [ $START == "yes" ] ; then

		# start rundeck

		(java -Xmx1024m -XX:MaxPermSize=256m -jar $launcherJar > $DIR/rundeck.out 2>&1 ) &
		RDPID=$!

		trap "{ kill -9 $RDPID ; echo '---Rundeck Killed---' ; cat $DIR/rundeck.out ; exit 255; }" EXIT SIGINT SIGTERM ERR

		echo "Rundeck process PID: $RDPID"
	fi

	wait_for $DIR/rundeck.out 'Started SelectChannelConnector@'

	echo "Rundeck started."

	if [ $TESTS == "yes" ] ; then
		echo "Start tests"

		run_tests $DIR $SRC

		echo "Tests complete."
	else
		echo "Tests skipped."
	fi

	if [ $WAIT == "yes" ] ; then
		echo "Rundeck is now running on http://$(hostname):4440 . Type ctrl-c to stop."
		wait $RDPID
	else
		echo "Stop Rundeck"
		stop_rundeck $RDPID
		echo "Rundeck stopped."
	fi
}


main() {
    check_args
    run_ci_test
}
main

