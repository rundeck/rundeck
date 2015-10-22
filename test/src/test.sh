#!/bin/bash

SRC_DIR=$(cd `dirname $0` && pwd)
DIR=${TMP_DIR:-$SRC_DIR}

RDECK_ETC=${RDECK_ETC:-$RDECK_BASE/etc}
# modify to point to xmlstarlet
XMLSTARLET=${XMLSTARLET:-xmlstarlet}


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
rd-project -p test -a create  
if [ 0 != $? ] ; then
	echo Failed to create test project : $!
	exit 2
fi

#copy jobs file to replace template
sed "s#@DIRNAME@#$DIR#" $SRC_DIR/test.jobs.xml > $DIR/test.jobs.expanded.xml

#load jobs
rd-jobs load -p test -f $DIR/test.jobs.expanded.xml > $DIR/load.out
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

#purge the jobs
rd-jobs purge -p test -g test > $DIR/load.out
if [ 0 != $? ] ; then
	echo Failed to purge jobs: $!
	exit 2
fi
cat $DIR/load.out



rd-jobs load -p test -f $DIR/test.jobs.expanded.xml > $DIR/load.out
if [ 0 != $? ] ; then
	echo Failed to load jobs: $!
	exit 2
fi
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

# Jobs should fail due to missing project
#load jobs
rd-jobs load -p thisProjectDNE -f $SRC_DIR/test.jobs2.xml > $DIR/load.out
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

run -i 1 
if [ 0 != $? ] ; then
	echo Failed to run job id 1: $!
	exit 2
fi

# try to run job by name and project

run -j 'test/simple script test' -p test
if [ 0 != $? ] ; then
	echo Failed to run job by name: $!
	exit 2
fi



dispatch -p test -- uptime > $DIR/exec.out 
if [ 0 != $? ] ; then
	echo Failed: dispatch -- uptime : $!
	cat $DIR/exec.out
	exit 2
fi
grep 'Succeeded queueing' -q $DIR/exec.out 
if [ 0 != $? ] ; then
	echo Failed to queue execution: $!
	cat $DIR/exec.out
	exit 2
fi

rm $DIR/exec.out

echo "dispatch --follow -- command"
dispatch -p test --follow -- echo dispatch test \; uptime > $DIR/exec.out
if [ 0 != $? ] ; then
	echo Failed to dispatch uptime and follow : $!
	cat $DIR/exec.out
	exit 2
fi
grep 'dispatch test' -q $DIR/exec.out 
if [ 0 != $? ] ; then
	echo Failed to see follow output for dispatch: $!
	cat $DIR/exec.out
	exit 2
fi

echo "dispatch -s scriptfile"
cat > $DIR/dispatch-test.sh <<END
#!/bin/bash
echo this is a test of dispatch -s 
echo args are \$@
END
dispatch -p test -s $DIR/dispatch-test.sh > $DIR/exec.out
if [ 0 != $? ] ; then
	echo Failed to dispatch scriptfile via cli : $!
	cat $DIR/exec.out
	exit 2
fi
grep 'Succeeded queueing' -q $DIR/exec.out 
if [ 0 != $? ] ; then
	echo Failed to queue scriptfile: $!
	cat $DIR/exec.out
	exit 2
fi

echo "dispatch -s scriptfile -- args"
dispatch -p test -s $DIR/dispatch-test.sh -- arg1 arg2 > $DIR/exec.out
if [ 0 != $? ] ; then
	echo Failed to dispatch scriptfile via cli : $!
	cat $DIR/exec.out
	exit 2
fi
grep 'Succeeded queueing' -q $DIR/exec.out 
if [ 0 != $? ] ; then
	echo Failed to queue scriptfile: $!
	cat $DIR/exec.out
	exit 2
fi

echo "dispatch --follow -s scriptfile"
dispatch -p test --follow -s $DIR/dispatch-test.sh  > $DIR/exec.out
if [ 0 != $? ] ; then
	echo Failed to follow scriptfile via cli : $!
	cat $DIR/exec.out
	exit 2
fi
grep 'this is a test of dispatch -s' -q $DIR/exec.out 
if [ 0 != $? ] ; then
	echo Failed to see follow output for dispatch scriptfile: $!
	cat $DIR/exec.out
	exit 2
fi

echo "dispatch --follow -s scriptfile -- arg1 arg2"
dispatch -p test --follow -s $DIR/dispatch-test.sh -- arg1 arg2  > $DIR/exec.out
if [ 0 != $? ] ; then
	echo Failed to follow scriptfile via cli : $!
	cat $DIR/exec.out
	exit 2
fi
grep 'this is a test of dispatch -s' -q $DIR/exec.out 
if [ 0 != $? ] ; then
	echo Failed to see follow output for dispatch scriptfile: $!
	cat $DIR/exec.out
	exit 2
fi
grep 'args are arg1 arg2' -q $DIR/exec.out 
if [ 0 != $? ] ; then
	echo Failed to see follow output for dispatch scriptfile: $!
	cat $DIR/exec.out
	exit 2
fi

echo "dispatch -u url"
cat > $DIR/dispatch-test.sh <<END
#!/bin/bash
echo this is a test of dispatch -u url
echo args are \$@
END
dispatch -p test -u file:$DIR/dispatch-test.sh  > $DIR/exec.out
if [ 0 != $? ] ; then
	echo Failed to dispatch url via cli : $!
	cat $DIR/exec.out
	exit 2
fi
grep 'Succeeded queueing' -q $DIR/exec.out 
if [ 0 != $? ] ; then
	echo Failed to queue dispatch url: $!
	cat $DIR/exec.out
	exit 2
fi

echo "dispatch -u url -- args"

dispatch -p test -u file:$DIR/dispatch-test.sh -- arg1 arg2 > $DIR/exec.out
if [ 0 != $? ] ; then
	echo Failed to dispatch url via cli : $!
	cat $DIR/exec.out
	exit 2
fi
grep 'Succeeded queueing' -q $DIR/exec.out 
if [ 0 != $? ] ; then
	echo Failed to queue dispatch url: $!
	cat $DIR/exec.out
	exit 2
fi

echo "dispatch --follow -u url"
dispatch -p test --follow -u file:$DIR/dispatch-test.sh  > $DIR/exec.out
if [ 0 != $? ] ; then
	echo Failed to follow dispatch url via cli : $!
	cat $DIR/exec.out
	exit 2
fi
grep 'this is a test of dispatch -u url' -q $DIR/exec.out 
if [ 0 != $? ] ; then
	echo Failed to see follow output for dispatch url: $!
	cat $DIR/exec.out
	exit 2
fi

echo "dispatch --follow -u url -- args"
dispatch -p test --follow -u file:$DIR/dispatch-test.sh -- argA argB > $DIR/exec.out
if [ 0 != $? ] ; then
	echo Failed to follow dispatch url via cli : $!
	cat $DIR/exec.out
	exit 2
fi
grep 'this is a test of dispatch -u url' -q $DIR/exec.out 
if [ 0 != $? ] ; then
	echo Failed to see follow output for dispatch url: $!
	cat $DIR/exec.out
	exit 2
fi
grep 'args are argA argB' -q $DIR/exec.out 
if [ 0 != $? ] ; then
	echo Failed to see follow output for dispatch url: $!
	cat $DIR/exec.out
	exit 2
fi

rm $DIR/dispatch-test.sh
rm $DIR/exec.out

#try loading yaml jobs

echo "Loading some jobs via yaml"
sed "s#@DIRNAME@#$DIR#" $SRC_DIR/test.jobs.yaml > $DIR/test.jobs.expanded.yaml

rd-jobs load -p test -F yaml -f $DIR/test.jobs.expanded.yaml > $DIR/load.out
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

rd-jobs list -p test  > $DIR/list.out
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

rd-jobs list -p test -f $DIR/list.out --format xml
if [ 0 != $? ] ; then
	echo Failed to list jobs: $!
	exit 2
fi

if [ ! -s $DIR/list.out ] ; then
	echo Failed to export XML: file DNE or is zero length
	exit 2
fi


$XMLSTARLET val $DIR/list.out
if [ 0 != $? ] ; then
	echo Failed to validate XML output: $!
	exit 2
fi

rm $DIR/list.out

echo "Listing jobs in Yaml"

rd-jobs list -p test -f $DIR/list.out --format yaml
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

rd-queue list -p test  > $DIR/list.out
if [ 0 != $? ] ; then
	echo Failed to list queue: $!
	exit 2
fi
cat $DIR/list.out
rm $DIR/list.out

# create secondary project and assert rd-queue requires -p parameter

#create test project
rd-project -p test2 -a create
if [ 0 != $? ] ; then
	echo Failed to create test 2project : $!
	exit 2
fi

echo "Test rd-queue requires -p argument"

rd-queue list   > $DIR/list.out
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

RDECK_URL=$(grep framework.server.url $RDECK_ETC/framework.properties  | cut -d' ' -f3)

egrep 'https://' $RDECK_ETC/framework.properties > /dev/null
https=$?

if [ 0 = $https ] ; then
    # call api/testall.sh and use -k curl option to ignore server certificate
    # 
    bash $SRC_DIR/../api/testall.sh "$RDECK_URL" -k
    #################
    # alternate args to curl to use a pem formatted cert to verify server cert:
    #sh $SRC_DIR/testweb.sh "https://localhost:4443" "--cacert $RDECK_ETC/rundeck.server.pem"
    ################
else
    bash $SRC_DIR/../api/testall.sh "$RDECK_URL"
fi

if [ 0 != $? ] ; then
	echo Failed to run testall.sh : $!
	exit 2
fi
