#!/bin/bash

DIR=$(cd `dirname $0` && pwd)

#assert RDECK_BASE
if [ -z "$RDECK_BASE" ] ; then
    echo "RDECK_BASE not set"
    exit 2
fi

cd $RDECK_BASE 
if [ 0 != $? ] ; then
	echo Failed to cd to $RDECK_BASE : $!
	exit 2
fi

#create test project
$RDECK_BASE/tools/bin/run-project -p test -a create  
if [ 0 != $? ] ; then
	echo Failed to create test project : $!
	exit 2
fi

#copy jobs file to replace template
cp $DIR/test.jobs.xml $DIR/test.jobs.expanded.xml

sed -i '' "s#@DIRNAME@#$DIR#" $DIR/test.jobs.expanded.xml

#load jobs
$RDECK_BASE/tools/bin/run-jobs load -f $DIR/test.jobs.xml > $DIR/load.out
if [ 0 != $? ] ; then
	echo Failed to load jobs: $!
	exit 2
fi
cat $DIR/load.out

grep -q Failed $DIR/load.out 
if [ 0 == $? ] ; then
	echo Failed to load some job : $!
	exit 2
fi

rm $DIR/load.out
rm $DIR/test.jobs.expanded.xml

# try to run job id 1

$RDECK_BASE/tools/bin/run-run -i 1 
if [ 0 != $? ] ; then
	echo Failed to run job id 1: $!
	exit 2
fi

# try run-exec

$RDECK_BASE/tools/bin/run -- uptime 
if [ 0 != $? ] ; then
	echo Failed to run uptime via cli : $!
	exit 2
fi

$RDECK_BASE/tools/bin/run -Q -- uptime > $DIR/exec.out 
if [ 0 != $? ] ; then
	echo Failed: run -Q -- uptime : $!
	exit 2
fi
grep 'Succeeded queueing' -q $DIR/exec.out 
if [ 0 != $? ] ; then
	echo Failed to queue execution: $!
	exit 2
fi

rm $DIR/exec.out

sh $DIR/testweb.sh
if [ 0 != $? ] ; then
	echo Failed to run testweb.sh : $!
	exit 2
fi
