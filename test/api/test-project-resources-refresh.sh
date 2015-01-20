#!/bin/bash

#test update project resources file

DIR=$(cd `dirname $0` && pwd)
export API_VERSION=2 #/api/2/project/NAME/resources/refresh
source $DIR/include.sh
file=$DIR/curl.out

RDECK_PROJECTS=${RDECK_PROJECTS:-$RDECK_BASE/projects}

# now submit req
proj="test"

runurl="${APIURL}/project/${proj}/resources/refresh"

echo "TEST: /api/2/project/${proj}/resources/refresh (no URL defined)"
params="t=t"

docurl -X POST  ${runurl}?${params} > ${file} || fail "ERROR: failed request"

sh $SRC_DIR/api-test-error.sh ${file} "Resources were not updated because no resource model provider URL is configured for project test" || exit 2

echo "OK"

echo "TEST: /api/2/project/${proj}/resources/refresh (invalid URL)"

#backup project.properties and set invalid resources url

TETC=$RDECK_PROJECTS/test/etc
TPROPS=$TETC/project.properties
TRES=$TETC/resources.xml

if [ ! -f $TPROPS.testbackup ] ; then
    cp $TPROPS $TPROPS.testbackup
fi
if [ ! -f $TRES.testbackup -a -f $TRES ] ; then
    cp $TRES $TRES.testbackup
fi

echo "project.resources.url=http://invaliddomain:1235/resources.xml" >> $TPROPS


docurl -X POST  ${runurl}?${params} > ${file} || fail "ERROR: failed request"

sh $SRC_DIR/api-test-error.sh ${file} "Error updating node resources file for project test: com.dtolabs.rundeck.core.common.FileUpdaterException: java.net.UnknownHostException: invaliddomain" || exit 2

echo "OK"

cp $TPROPS.testbackup $TPROPS
sleep 1
echo "TEST: /api/2/project/${proj}/resources/refresh (valid temp URL)"

cat <<END > $TETC/testUpdateResources.xml
<?xml version="1.0" encoding="UTF-8"?>

<project>
  <node name="test1" type="Node" description="Rundeck test node" tags="test1,testboth" hostname="testhost1" osArch="x86_64" osFamily="unix" osName="Mac OS X" osVersion="10.6.6" username="rdeck" editUrl="" remoteUrl=""/>
  <node name="test2" type="Node" description="Rundeck test node" tags="test2,testboth" hostname="testhost2" osArch="x86_64" osFamily="unix" osName="Mac OS X" osVersion="10.6.6" username="rdeck1" editUrl="" remoteUrl=""/>
</project>
END

TEMPURL="file://$TETC/testUpdateResources.xml"

echo "project.resources.url=$TEMPURL" >> $TPROPS


docurl -X POST  ${runurl}?${params} > ${file} || fail "ERROR: failed request"

sh $SRC_DIR/api-test-success.sh ${file} "Resources were successfully updated for project test" || exit 2

echo "OK"

#restore backup props, no provider url
if [ -f $TPROPS.testbackup ] ; then
    cp $TPROPS.testbackup $TPROPS
fi

sleep 1
# set allowed URL with port 1235
echo "project.resources.allowedURL.0=http://invaliddomain:1235/resources.xml" >> $TPROPS

echo "TEST: /api/2/project/${proj}/resources/refresh (POST) (not allowed provider URL)"

#backup project.properties and set invalid resources url

data="providerURL=http://invaliddomain:1234/resources.xml"

# post data
$CURL -H "$AUTHHEADER" -X POST --data-urlencode "${data}" ${runurl}?${params} > ${file} || fail "ERROR: failed request"

sh $SRC_DIR/api-test-error.sh ${file} "Error updating node resources file for project test: providerURL is not allowed: http://invaliddomain:1234/resources.xml" || exit 2

echo "OK"

echo "TEST: /api/2/project/${proj}/resources/refresh (POST) (allowed URL, invalid hostname)"

#backup project.properties and set invalid resources url

data="providerURL=http://invaliddomain:1235/resources.xml"

# post data
$CURL -H "$AUTHHEADER" -X POST --data-urlencode "${data}" ${runurl}?${params} > ${file} || fail "ERROR: failed request"

sh $SRC_DIR/api-test-error.sh ${file} "Error updating node resources file for project test: com.dtolabs.rundeck.core.common.FileUpdaterException: java.net.UnknownHostException: invaliddomain" || exit 2

echo "OK"

echo "TEST: /api/2/project/${proj}/resources/refresh (POST) (not allowed file URL)"

TEMPURL="file://$TETC/testUpdateResources.xml"

data="providerURL=$TEMPURL"

# post data
$CURL -H "$AUTHHEADER" -X POST --data-urlencode "${data}" ${runurl}?${params} > ${file} || fail "ERROR: failed request"

sh $SRC_DIR/api-test-error.sh ${file} "Error updating node resources file for project test: providerURL is not allowed: file://$TETC/testUpdateResources.xml" || exit 2

echo "OK"


echo "TEST: /api/2/project/${proj}/resources/refresh (POST) (valid provider URL)"


TEMPURL="file://$TETC/testUpdateResources.xml"
sleep 1
# set allowed URL for test file
echo "project.resources.allowedURL.1=file://$TETC/testUpdateResources.xml" >> $TPROPS

data="providerURL=$TEMPURL"

# post data
$CURL -H "$AUTHHEADER" -X POST --data-urlencode "${data}" ${runurl}?${params} > ${file} || fail "ERROR: failed request"

sh $SRC_DIR/api-test-success.sh ${file} "Resources were successfully updated for project test" || exit 2

echo "OK"

#restore backup props, no provider url
if [ -f $TPROPS.testbackup ] ; then
    mv $TPROPS.testbackup $TPROPS
fi

if [ -f $TRES.testbackup ] ; then
    mv $TRES.testbackup $TRES
fi

rm ${file}

