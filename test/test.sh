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


###############
# load jobs with failures
###############
#
#copy jobs file to replace template

#load jobs
$RDECK_BASE/tools/bin/rd-jobs load -f $DIR/test.jobs2.xml > $DIR/load.out
if [ 1 != $? ] ; then
	echo Should have failed to load a job: $!
	exit 2
fi
cat $DIR/load.out

grep -q Failed $DIR/load.out 
if [ 1 == $? ] ; then
	echo Should have failed to load a job: $!
	exit 2
fi

rm $DIR/load.out


# try to run job id 1

$RDECK_BASE/tools/bin/run -i 1 
if [ 0 != $? ] ; then
	echo Failed to run job id 1: $!
	exit 2
fi

# try to run job by name and project

$RDECK_BASE/tools/bin/run -j 'test/simple script test' -p test
if [ 0 != $? ] ; then
	echo Failed to run job by name: $!
	exit 2
fi


# try dispatch
echo "using --noqueue option"
$RDECK_BASE/tools/bin/dispatch -p test --noqueue -- uptime
if [ 0 != $? ] ; then
	echo Failed to dispatch uptime via cli : $!
	exit 2
fi

$RDECK_BASE/tools/bin/dispatch -p test -Q -- uptime > $DIR/exec.out 
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


echo "Listing jobs"

$RDECK_BASE/tools/bin/rd-jobs list -p test  > $DIR/list.out
if [ 0 != $? ] ; then
	echo Failed to list jobs: $!
	exit 2
fi
cat $DIR/list.out

grep -q Failed $DIR/list.out
if [ 0 == $? ] ; then
	echo Failed to list some job : $!
	exit 2
fi

rm $DIR/list.out

echo "Listing jobs in XML"

$RDECK_BASE/tools/bin/rd-jobs list -p test -f $DIR/list.out --format xml
if [ 0 != $? ] ; then
	echo Failed to list jobs: $!
	exit 2
fi

if [ ! -s $DIR/list.out ] ; then
	echo Failed to export XML: file DNE or is zero length
	exit 2
fi

# modify to point to xmlstarlet
XMLSTARLET=xml

$XMLSTARLET val $DIR/list.out
if [ 0 != $? ] ; then
	echo Failed to validate XML output: $!
	exit 2
fi

rm $DIR/list.out

echo "Listing jobs in Yaml"

$RDECK_BASE/tools/bin/rd-jobs list -p test -f $DIR/list.out --format yaml
if [ 0 != $? ] ; then
	echo Failed to list jobs: $!
	exit 2
fi

if [ ! -s $DIR/list.out ] ; then
	echo Failed to export Yaml: file DNE or is zero length
	exit 2
fi

grep -q 'simple file test' $DIR/list.out
if [ 0 != $? ] ; then
	echo Failed to export Yaml: $!
	exit 2
fi

rm $DIR/list.out

echo "Test rd-queue"

$RDECK_BASE/tools/bin/rd-queue list -p test  > $DIR/list.out
if [ 0 != $? ] ; then
	echo Failed to list queue: $!
	exit 2
fi
cat $DIR/list.out
rm $DIR/list.out

# create secondary project and assert rd-queue requires -p parameter

#create test project
$RDECK_BASE/tools/bin/rd-project -p test2 -a create
if [ 0 != $? ] ; then
	echo Failed to create test 2project : $!
	exit 2
fi

echo "Test rd-queue requires -p argument"

$RDECK_BASE/tools/bin/rd-queue list   > $DIR/list.out
if [ 0 == $? ] ; then
	echo Expected failure to rd-queue: $!
    cat $DIR/list.out
	exit 2
fi
rm $DIR/list.out

rm -rf $RDECK_BASE/projects/test2

if [ -d "$RDECK_BASE/projects/test2" ] ; then
	echo Failed to remove test2 project : $RDECK_BASE/projects/test2 exists
	exit 2
fi

egrep 'https://' $RDECK_BASE/etc/framework.properties > /dev/null
https=$?

if [ 0 = $https ] ; then
    # call api/testall.sh and use -k curl option to ignore server certificate
    sh $DIR/api/testall.sh "https://localhost:4443" -k
    #################
    # alternate args to curl to use a pem formatted cert to verify server cert:
    #sh $DIR/testweb.sh "https://localhost:4443" "--cacert $RDECK_BASE/etc/rundeck.server.pem"
    ################
else
    sh $DIR/api/testall.sh "http://localhost:4440"
fi

if [ 0 != $? ] ; then
	echo Failed to run testall.sh : $!
	exit 2
fi
