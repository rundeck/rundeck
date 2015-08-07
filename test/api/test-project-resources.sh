#!/bin/bash

#test update project resources file

DIR=$(cd `dirname $0` && pwd)
export API_VERSION=2 #/api/2/project/NAME/resources
source $DIR/include.sh

file=$DIR/curl.out

# now submit req
proj="test"

runurl="${APIURL}/project/${proj}/resources"

echo "TEST: /api/2/project/${proj}/resources (GET)"
params="format=xml"

# get listing
docurl ${runurl}?${params} > ${file} || fail "ERROR: failed request"

#test curl.out for valid xml
$XMLSTARLET val -w ${file} > /dev/null 2>&1
validxml=$?
if [ 0 == $validxml ] ; then 
    #test for for possible result error message
    $XMLSTARLET el ${file} | grep -e '^result' -q
    if [ 0 == $? ] ; then
        #test for error message
        #If <result error="true"> then an error occured.
        waserror=$($XMLSTARLET sel -T -t -v "/result/@error" ${file})
        errmsg=$($XMLSTARLET sel -T -t -v "/result/error/message" ${file})
        if [ "" != "$waserror" -a "true" == $waserror ] ; then
            errorMsg "FAIL: expected resource.xml content but received error result: $errmsg"
            exit 2
        fi
    fi
fi

#test curl.out for valid xml
if [ 0 != $validxml ] ; then
    errorMsg "ERROR: Response was not valid xml"
    exit 2
fi

#test for expected /joblist element
$XMLSTARLET el ${file} | grep -e '^project' -q
if [ 0 != $? ] ; then
    errorMsg "ERROR: Response did not contain expected result"
    exit 2
fi

#Check results list
itemcount=$($XMLSTARLET sel -T -t -v "count(/project/node)" ${file})
if [ "0" == "$itemcount" ] ; then
    errorMsg "FAIL: expected multiple /project/node element"
    exit 2
fi

echo "OK"


echo "TEST: /api/2/project/${proj}/resources (GET) (YAML)"
params="format=yaml"

# get listing
docurl ${runurl}?${params} > ${file} || fail "ERROR: failed request"

#test curl.out for valid xml
$XMLSTARLET val -w ${file} > /dev/null 2>&1
validxml=$?
if [ 0 == $validxml ] ; then
    fail "Should not be XML"
fi

grep -e 'nodename:' -q ${file}
if [ 0 != $? ] ; then
    fail "Result does not appear correct"
fi

echo "OK"


echo "TEST: /api/2/project/${proj}/resources (GET) (unsupported)"
params="format=unsupported"

docurl ${runurl}?${params} > ${file} || fail "ERROR: failed request"
$SHELL $SRC_DIR/api-test-error.sh ${file} "Unsupported API Version \"2\". API Request: /api/2/project/${proj}/resources. Reason: Minimum supported version: 3" || fail "ERROR: failed request"

echo "OK"

echo "TEST: /api/3/project/${proj}/resources (GET) (otherformat)"

API3URL="${RDURL}/api/3"
runurl3="${API3URL}/project/${proj}/resources"
params="format=other"

docurl ${runurl3}?${params} > ${file} || fail "ERROR: failed request"
$SHELL $SRC_DIR/api-test-error.sh ${file} "The format specified is unsupported: other" || fail "ERROR: failed request"

echo "OK"

echo "TEST: /api/2/project/${proj}/resources (POST) (xml)"

TETC=$RDECK_PROJECTS/test/etc
TRES=$TETC/resources.xml

if [ ! -f $TRES.testbackup ] ; then
    cp $TRES $TRES.testbackup
fi

cat <<END > $TETC/testUpdateResources.xml
<?xml version="1.0" encoding="UTF-8"?>

<project>
  <node name="test1" type="Node" description="Rundeck test node" tags="test1,testboth" hostname="testhost1" osArch="x86_64" osFamily="unix" osName="Mac OS X" osVersion="10.6.6" username="rdeck" editUrl="" remoteUrl=""/>
  <node name="test2" type="Node" description="Rundeck test node" tags="test2,testboth" hostname="testhost2" osArch="x86_64" osFamily="unix" osName="Mac OS X" osVersion="10.6.6" username="rdeck1" editUrl="" remoteUrl=""/>
</project>
END

# post data
$CURL -H "$AUTHHEADER" -X POST -H 'Content-Type: text/xml' --data-binary "@$TETC/testUpdateResources.xml" ${runurl}?${params} > ${file} || fail "ERROR: failed request"

$SHELL $SRC_DIR/api-test-success.sh ${file} "Resources were successfully updated for project test" || exit 2

echo "OK"


cat <<END > $TETC/testUpdateResources.yaml
test1:
   tags: test1, testboth
   osFamily: unix
   osVersion: 10.6.6
   osArch: x86_64
   editUrl: ''
   hostname: testhost1
   type: Node
   username: rdeck
   description: Rundeck test node
   remoteUrl: ''
   nodename: test1
   osName: Mac OS X
test2:
   tags: test2, testboth
   osFamily: unix
   osVersion: 10.6.6
   osArch: x86_64
   editUrl: ''
   hostname: testhost2
   type: Node
   username: rdeck1
   description: Rundeck test node
   remoteUrl: ''
   nodename: test2
   osName: Mac OS X
END

echo "TEST: /api/2/project/${proj}/resources (POST) (yaml)"

# post data
$CURL -H "$AUTHHEADER" -X POST -H 'Content-Type: text/yaml' --data-binary "@$TETC/testUpdateResources.yaml" ${runurl}?${params} > ${file} || fail "ERROR: failed request"

$SHELL $SRC_DIR/api-test-success.sh ${file} "Resources were successfully updated for project test" || exit 2

echo "OK"


echo "TEST: /api/2/project/${proj}/resources (POST) (unsupported)"

cat <<END > $TETC/testUpdateResources.xml
<?xml version="1.0" encoding="UTF-8"?>

<project>
  <node name="test1" type="Node" description="Rundeck test node" tags="test1,testboth" hostname="testhost1" osArch="x86_64" osFamily="unix" osName="Mac OS X" osVersion="10.6.6" username="rdeck" editUrl="" remoteUrl=""/>
  <node name="test2" type="Node" description="Rundeck test node" tags="test2,testboth" hostname="testhost2" osArch="x86_64" osFamily="unix" osName="Mac OS X" osVersion="10.6.6" username="rdeck1" editUrl="" remoteUrl=""/>
</project>
END

# post data
$CURL -H "$AUTHHEADER" -X POST -H 'Content-Type: text/x-something' --data-binary "@$TETC/testUpdateResources.xml" ${runurl}?${params} > ${file} || fail "ERROR: failed request"

$SHELL $SRC_DIR/api-test-error.sh ${file} "Unsupported API Version \"2\". API Request: /api/2/project/test/resources. Reason: Minimum supported version: 3" || exit 2

echo "OK"



echo "TEST: /api/3/project/${proj}/resources (POST) (format=other)"
params=""

cat <<END > $TETC/testUpdateResources.xml
<?xml version="1.0" encoding="UTF-8"?>

<project>
  <node name="test1" type="Node" description="Rundeck test node" tags="test1,testboth" hostname="testhost1" osArch="x86_64" osFamily="unix" osName="Mac OS X" osVersion="10.6.6" username="rdeck" editUrl="" remoteUrl=""/>
  <node name="test2" type="Node" description="Rundeck test node" tags="test2,testboth" hostname="testhost2" osArch="x86_64" osFamily="unix" osName="Mac OS X" osVersion="10.6.6" username="rdeck1" editUrl="" remoteUrl=""/>
</project>
END

# post data
$CURL -H "$AUTHHEADER" -X POST -H 'Content-Type: text/x-something' --data-binary "@$TETC/testUpdateResources.xml" ${runurl3}?${params} > ${file} || fail "ERROR: failed request"

$SHELL $SRC_DIR/api-test-error.sh ${file} "Unsupported format: No provider available to parse MIME type: text/x-something" || exit 2

echo "OK"

if [ -f $TRES.testbackup ] ; then
    mv $TRES.testbackup $TRES
fi

rm $TETC/testUpdateResources.yaml
rm $TETC/testUpdateResources.xml
rm $DIR/curl.out

