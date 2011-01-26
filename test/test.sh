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
$RDECK_BASE/tools/bin/rd-project -p test -a create  
if [ 0 != $? ] ; then
	echo Failed to create test project : $!
	exit 2
fi

#copy jobs file to replace template
cp $DIR/test.jobs.xml $DIR/test.jobs.expanded.xml

sed -i '' "s#@DIRNAME@#$DIR#" $DIR/test.jobs.expanded.xml

#load jobs
$RDECK_BASE/tools/bin/rd-jobs load -f $DIR/test.jobs.expanded.xml > $DIR/load.out
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

$RDECK_BASE/tools/bin/run -i 1 
if [ 0 != $? ] ; then
	echo Failed to run job id 1: $!
	exit 2
fi

# try dispatch
echo "using --noqueue option"
$RDECK_BASE/tools/bin/dispatch --noqueue -- uptime
if [ 0 != $? ] ; then
	echo Failed to dispatch uptime via cli : $!
	exit 2
fi

$RDECK_BASE/tools/bin/dispatch -Q -- uptime > $DIR/exec.out 
if [ 0 != $? ] ; then
	echo Failed: dispatch -Q -- uptime : $!
	exit 2
fi
grep 'Succeeded queueing' -q $DIR/exec.out 
if [ 0 != $? ] ; then
	echo Failed to queue execution: $!
	exit 2
fi

rm $DIR/exec.out

#try loading yaml jobs

echo "Loading some jobs via yaml"
cp $DIR/test.jobs.yaml $DIR/test.jobs.expanded.yaml

sed -i '' "s#@DIRNAME@#$DIR#" $DIR/test.jobs.expanded.yaml

$RDECK_BASE/tools/bin/rd-jobs load -F yaml -f $DIR/test.jobs.expanded.yaml > $DIR/load.out
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
rm $DIR/test.jobs.expanded.yaml

egrep 'https://' $RDECK_BASE/etc/framework.properties > /dev/null
if [ 0 = $? ] ; then
    # call testweb and use -k curl option to ignore server certificate
    sh $DIR/testweb.sh "https://localhost:4443" -k
    #################
    # alternate args to curl to use a pem formatted cert to verify server cert:
    #sh $DIR/testweb.sh "https://localhost:4443" "--cacert $RDECK_BASE/etc/rundeck.server.pem"
    ################
else
    sh $DIR/testweb.sh "http://localhost:4440"
fi

if [ 0 != $? ] ; then
	echo Failed to run testweb.sh : $!
	exit 2
fi
