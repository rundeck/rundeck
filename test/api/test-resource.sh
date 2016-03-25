#!/bin/bash

#test /api/resource/name output.

DIR=$(cd `dirname $0` && pwd)
set -- - 

source $DIR/include.sh


project="test"
proj_config_url="${APIURL}/project/${project}/config"
set_url_config(){
    prop=$1
    value=$2

    docurl -X PUT --data-binary "${value}" -H 'Content-Type:text/plain' "${proj_config_url}/${prop}" > $DIR/curl.out
    if [ 0 != $? ] ; then
        errorMsg "ERROR: failed PUT request"
        exit 2
    fi
    #echo "project.resources.url=http://invalid.domain:1235/resources.xml" >> $TPROPS
}


#disable node caching
set_url_config "project.nodeCache.enabled" "false"


file=$DIR/curl.out

###
# Setup: acquire local node name from RDECK_ETC/framework.properties#server.name
####
localnode=$(grep 'framework.server.name' $RDECK_ETC/framework.properties | sed 's/framework.server.name = //')

if [ -z "${localnode}" ] ; then
    errorMsg "FAIL: Unable to determine framework.server.name from $RDECK_ETC/framework.properties"
    exit 2
fi

runurl="${APIURL}/resource/$localnode"
project="test"
params="project=${project}"

echo "TEST: /api/resource/$localnode"

# get listing
docurl ${runurl}?${params} > ${file}
if [ 0 != $? ] ; then
    errorMsg "ERROR: failed query request"
    exit 2
fi
#test curl.out for valid xml
$XMLSTARLET val -w ${file} > /dev/null 2>&1
validxml=$?
if [ 0 == $validxml ] ; then 
    #test for for possible result error message
    $XMLSTARLET el ${file} | grep -e '^result' -q
    if [ 0 == $? ] ; then
        #test for error message
        #If <result error="true"> then an error occured.
        waserror=$(xmlsel "/result/@error" ${file})
        errmsg=$(xmlsel "/result/error/message" ${file})
        if [ "" != "$waserror" -a "true" == $waserror ] ; then
            errorMsg "FAIL: expected resource.xml content but received error result: $errmsg"
            exit 2
        fi
    fi
fi

#test curl.out for valid xml
if [ 0 != $validxml ] ; then
    errorMsg "FAIL: Response was not valid xml"
    exit 2
fi

#test for expected /joblist element
$XMLSTARLET el ${file} | grep -e '^project' -q
if [ 0 != $? ] ; then
    errorMsg "FAIL: Response did not contain expected result"
    exit 2
fi

#Check projects list
itemcount=$(xmlsel "count(/project/node)" ${file})
if [ "1" != "$itemcount" ] ; then
    errorMsg "FAIL: expected single /project/node element ${runurl}?${params}"
    cat $file
    exit 2
fi

testname=$(xmlsel "/project/node/@name" ${file})

assert "$localnode" "$testname" "Wrong node name returned"
echo "OK"

#test yaml output
params="project=${project}&format=yaml"

echo "TEST: /api/resource/$localnode YAML response"

# get listing
docurl ${runurl}?${params} > ${file}
if [ 0 != $? ] ; then
    errorMsg "ERROR: failed query request"
    exit 2
fi
#test curl.out for valid xml
$XMLSTARLET val -w ${file} > /dev/null 2>&1
validxml=$?
if [ 0 == $validxml ] ; then 
    #test for error message
    #If <result error="true"> then an error occured.
    waserror=$(xmlsel "/result/@error" ${file})
    errmsg=$(xmlsel "/result/error/message" ${file})
    errorMsg "FAIL: expected YAML content but received error result: $errmsg"
    exit 2
fi

itemcount=$(grep 'hostname: ' ${file} | wc -l | sed  's/^[ ]*//g')

if [ "1" != "$itemcount" ] ; then 
    errorMsg "FAIL: Expected single yaml result, was \"${itemcount}\""
    exit 2
fi

testname=$(grep 'nodename: ' ${file})

assert "  nodename: $localnode" "$testname" "Wrong node name returned"

echo "OK"

####
# test with preset resources.
####

# temporarily move actual resources.xml out of the way, and replace with our own
# 

cp $RDECK_PROJECTS/test/etc/resources.xml $RDECK_PROJECTS/test/etc/resources.xml.backup

# sleep to force file mtime to change
sleep 1

cat <<END > $RDECK_PROJECTS/test/etc/resources.xml
<?xml version="1.0" encoding="UTF-8"?>

<project>
  <node name="test1" type="Node" description="Rundeck test node" tags="test1,testboth" hostname="testhost1" osArch="x86_64" osFamily="unix" osName="Mac OS X" osVersion="10.6.6" username="rdeck" editUrl="" remoteUrl=""/>
  <node name="test2" type="Node" description="Rundeck test node" tags="test2,testboth" hostname="testhost2" osArch="x86_64" osFamily="unix" osName="Mac OS X" osVersion="10.6.6" username="rdeck1" editUrl="" remoteUrl=""/>
</project>
END

####
# query specific test nodes
####

params="project=${project}&format=xml&"
runurl="${APIURL}/resource/test1"

echo "TEST: query result for /etc/resources/test1"

docurl ${runurl}?${params} > ${file}
if [ 0 != $? ] ; then
    errorMsg "ERROR: failed query request"
    exit 2
fi
$XMLSTARLET val -w ${file} > /dev/null 2>&1
if [ 0 != $? ] ; then 
    errorMsg "FAIL: result was not valid xml"
    exit 2
fi

#Check projects list
itemcount=$(xmlsel "count(/project/node)" ${file})
if [ "1" != "$itemcount" ] ; then
    errorMsg "FAIL: expected single /project/node element ${runurl}?${params}"
    cat $file
    exit 2
fi
itemname=$(xmlsel "/project/node/@name" ${file})
assert "test1" $itemname "Query result name was wrong"

echo "OK"

####
#query test2 node
####

params="project=${project}&format=xml&"
runurl="${APIURL}/resource/test2"

echo "TEST: query result for /etc/resource/test2"

docurl ${runurl}?${params} > ${file}
if [ 0 != $? ] ; then
    errorMsg "ERROR: failed query request"
    exit 2
fi
$XMLSTARLET val -w ${file} > /dev/null 2>&1
if [ 0 != $? ] ; then 
    errorMsg "FAIL: result was not valid xml"
    exit 2
fi

#Check projects list
itemcount=$(xmlsel "count(/project/node)" ${file})
if [ "1" != "$itemcount" ] ; then
    errorMsg "FAIL: expected single /project/node element ${runurl}?${params}"
    cat $file
    exit 2
fi
itemname=$(xmlsel "/project/node/@name" ${file})
assert "test2" $itemname "Query result name was wrong"

echo "OK"

rm ${file}
mv $RDECK_PROJECTS/test/etc/resources.xml.backup $RDECK_PROJECTS/test/etc/resources.xml
