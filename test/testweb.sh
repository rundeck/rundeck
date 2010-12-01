#!/bin/bash

errorMsg() {
   echo "$*" 1>&2
}

DIR=$(cd `dirname $0` && pwd)

#source rcfile
cd $RDECK_BASE 
if [ 0 != $? ] ; then
    errorMsg "failed to cd to RDECK_BASE: $RDECK_BASE: $!"
    exit 2
fi

# test log in 

url='http://localhost:8080'
loginurl="${url}/j_security_check"

# get main page for login
echo "WEB Starting tests."
echo "WEB Trying login..."
curl -s -S -L -c $DIR/cookies ${url}/menu/index > $DIR/curl.out 
if [ 0 != $? ] ; then
    errorMsg "failed menu request to ${url}/menu/index"
    exit 2
fi

grep 'j_security_check' -q $DIR/curl.out 
if [ 0 != $? ] ; then
    errorMsg "login page not found"
    exit 2
fi


curl -s -S -L -c $DIR/cookies -b $DIR/cookies -d j_username=admin -d j_password=admin $loginurl > $DIR/curl.out 
if [ 0 != $? ] ; then
    errorMsg "failed login request to ${loginurl}"
    exit 2
fi


grep 'id="nodesContent"' -q $DIR/curl.out 
if [ 0 != $? ] ; then
    errorMsg "login didnt seem to return the right page"
    exit 2
fi
grep 'j_security_check' -q $DIR/curl.out 
if [ 0 == $? ] ; then
    errorMsg "login was not successful"
    exit 2
fi

echo "WEB Login OK"
echo "WEB Testing Nodes..."
# get nodes page
curl -s -S -L -c $DIR/cookies -b $DIR/cookies ${url}/resources/nodes?project=test > $DIR/curl.out
if [ 0 != $? ] ; then
    errorMsg "failed nodes request"
    exit 2
fi

grep 'Nodes (1)' -q $DIR/curl.out 
if [ 0 != $? ] ; then
    errorMsg "nodes output didnt have right nodes count"
    exit 2
fi
grep 'j_security_check' -q $DIR/curl.out 
if [ 0 == $? ] ; then
    errorMsg "login was not successful "
    exit 2
fi

rm $DIR/curl.out
rm $DIR/cookies

echo "WEB Nodes Page OK"
