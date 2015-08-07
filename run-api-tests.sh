#!/bin/bash
#/ trigger local ci test run


. rd_versions.sh


set -euo pipefail
IFS=$'\n\t'
readonly ARGS=("$@")

usage() {
      grep '^#/' <"$0" | cut -c4- # prints the #/ lines above as usage info
}
die(){
    echo >&2 "$@" ; exit 2
}

check_args(){
   : example to check args length
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
	local outfile=${FARGS[1]}
	(java -Xmx1024m -XX:MaxPermSize=256m -jar $launcherJar > $outfile 2>&1 ) &
	# ( bash -c 'sleep 30; echo done; sleep 600' > $outfile 2>&1 ) &
	local RDPID=$!

	echo $RDPID
}
stop_rundeck(){
	local FARGS=("$@")
	local PID=${FARGS[0]}
	echo "stopping pid $PID..."
	kill -9 $PID || echo "PID already stopped $PID"
	trap "" EXIT SIGINT SIGTERM ERR
}
run_ci_test(){
	local FARGS=("$@")
	local -a VERS=( $( rd_get_version ) )
	local JAR=rundeck-launcher-${VERS[0]}-${VERS[2]}.jar
	local buildJar=$PWD/rundeck-launcher/launcher/build/libs/$JAR
	test -f $buildJar || die "Jar file not found $buildJar"
	local SRC=$PWD
	local DIR=$PWD/build/citest
	mkdir -p $DIR
	cp $buildJar $DIR/
	local launcherJar=$DIR/$JAR
	cd $DIR
	local HOST=$(hostname)

	echo "Start Rundeck"
	# start rundeck
	local RDPID=$( start_rundeck $launcherJar $DIR/rundeck.out )
	trap "{ kill -9 $RDPID ; exit 255; }" EXIT SIGINT SIGTERM ERR
	echo "Rundeck process PID: $RDPID"
	wait_for $DIR/rundeck.out 'Started SelectChannelConnector@'
	# wait_for $DIR/rundeck.out 'done'
	echo "Rundeck started."

	echo "Start tests"
	RDECK_BASE=$DIR PATH=$PATH:$DIR/tools/bin  \
		 bash -c  $SRC/test/src/test.sh http://$HOST:4440 admin admin

	echo "Tests complete."
	echo "Stop Rundeck"
	stop_rundeck $RDPID
	echo "Rundeck stopped."
}


main() {
    check_args
    run_ci_test
}
main

