#!/bin/bash

#test update project resources file

DIR=$(cd `dirname $0` && pwd)
source $DIR/include.sh

# now submit req
proj="test"

runurl="${APIURL}/project/${proj}/updateResources"

echo "TEST: /api/project/${proj}/updateResources (no URL defined)"
params="t=t"

sh $DIR/api-expect-error.sh ${runurl} ${params} "Resources were not updated because no resource model provider URL is configured for project test" || exit 2

echo "OK"

echo "TEST: /api/project/${proj}/updateResources (invalid URL)"

#backup project.properties and set invalid resources url

TETC=$RDECK_BASE/projects/test/etc
TPROPS=$TETC/project.properties
TRES=$TETC/resources.xml

if [ ! -f $TPROPS.testbackup ] ; then
    cp $TPROPS $TPROPS.testbackup
fi
if [ ! -f $TRES.testbackup ] ; then
    cp $TRES $TRES.testbackup
fi

echo "project.resources.url=http://invaliddomain:1234/resources.xml" >> $TPROPS

sh $DIR/api-expect-error.sh ${runurl} ${params} "Error updating node resources file for project test: java.net.UnknownHostException: invaliddomain" || exit 2

echo "OK"

cp $TPROPS.testbackup $TPROPS

echo "TEST: /api/project/${proj}/updateResources (valid temp URL)"

cat <<END > $TETC/testUpdateResources.xml
<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE project PUBLIC "-//DTO Labs Inc.//DTD Resources Document 1.0//EN" "project.dtd">

<project>
  <node name="test1" type="Node" description="Rundeck test node" tags="test1,testboth" hostname="testhost1" osArch="x86_64" osFamily="unix" osName="Mac OS X" osVersion="10.6.6" username="rdeck" editUrl="" remoteUrl=""/>
  <node name="test2" type="Node" description="Rundeck test node" tags="test2,testboth" hostname="testhost2" osArch="x86_64" osFamily="unix" osName="Mac OS X" osVersion="10.6.6" username="rdeck1" editUrl="" remoteUrl=""/>
</project>
END

TEMPURL="file://$TETC/testUpdateResources.xml"

echo "project.resources.url=$TEMPURL" >> $TPROPS

$CURL ${runurl}?${params} > $DIR/curl.out || fail "Failed making request"

sh $DIR/api-test-success.sh $DIR/curl.out "Resources were successfully updated for project test" || exit 2

echo "OK"

if [ -f $TPROPS.testbackup ] ; then
    mv $TPROPS.testbackup $TPROPS
fi
if [ -f $TRES.testbackup ] ; then
    mv $TRES.testbackup $TRES
fi

rm $DIR/curl.out

