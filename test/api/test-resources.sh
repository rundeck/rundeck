#!/bin/bash

#test /api/resources output.

DIR=$(cd `dirname $0` && pwd)
source $DIR/include.sh

file=$DIR/curl.out

###
# Setup: acquire local node name from RDECK_BASE/etc/framework.properties#node.name
####
localnode=$(grep 'framework.node.name' $RDECK_BASE/etc/framework.properties | sed 's/framework.node.name = //')

if [ -z "${localnode}" ] ; then
    errorMsg "FAIL: Unable to determine framework.node.name from $RDECK_BASE/etc/framework.properties"
    exit 2
fi

# now submit req
runurl="${APIURL}/resources"

echo "TEST: /api/resources, basic XML response with all nodes: >0 result"

project="test"
params="project=${project}"

# get listing
$CURL --header "$VERSHEADER" ${runurl}?${params} > ${file}
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
echo "$itemcount Nodes"
if [ "0" == "$itemcount" ] ; then
    errorMsg "FAIL: expected multiple /project/node element"
    exit 2
fi

echo "OK"

#test yaml output
params="project=${project}&format=yaml"

echo "TEST: /api/resources, basic YAML response with no query: >0 result"

# get listing
$CURL --header "$VERSHEADER" ${runurl}?${params} > ${file}
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
    waserror=$($XMLSTARLET sel -T -t -v "/result/@error" ${file})
    errmsg=$($XMLSTARLET sel -T -t -v "/result/error/message" ${file})
    errorMsg "FAIL: expected YAML content but received error result: $errmsg"
    exit 2
fi

itemcount=$(grep 'hostname: ' ${file} | wc -l | sed  's/^[ ]*//g')

if [ "0" == "$itemcount" ] ; then
    errorMsg "FAIL: Expected multiple yaml result, was \"${itemcount}\""
    exit 2
fi

echo "OK"

####
# test with preset resources.
####

# temporarily move actual resources.xml out of the way, and replace with our own

cp $RDECK_BASE/projects/test/etc/resources.xml $RDECK_BASE/projects/test/etc/resources.xml.backup

cat <<END > $RDECK_BASE/projects/test/etc/resources.xml
<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE project PUBLIC "-//DTO Labs Inc.//DTD Resources Document 1.0//EN" "project.dtd">

<project>
  <node name="test1" type="Node" description="Rundeck test node" tags="test1,testboth" hostname="testhost1" osArch="x86_64" osFamily="unix" osName="Mac OS X" osVersion="10.6.6" username="rdeck" editUrl="" remoteUrl=""/>
  <node name="test2" type="Node" description="Rundeck test node" tags="test2,testboth" hostname="testhost2" osArch="x86_64" osFamily="unix" osName="Mac OS X" osVersion="10.6.6" username="rdeck1" editUrl="" remoteUrl=""/>
</project>
END

#now query and expect certain results. 

query="name=test1"
params="project=${project}&format=xml&${query}"

echo "TEST: query result for /etc/resources"

$CURL --header "$VERSHEADER" ${runurl}?${params} > ${file}
if [ 0 != $? ] ; then
    errorMsg "ERROR: failed query request"
    exit 2
fi
$XMLSTARLET val -w ${file} > /dev/null 2>&1
if [ 0 != $? ] ; then 
    errorMsg "FAIL: result was not valid xml"
    exit 2
fi

#Check results list
itemcount=$($XMLSTARLET sel -T -t -v "count(/project/node)" ${file})
if [ "1" != "$itemcount" ] ; then
    errorMsg "FAIL: expected single /project/node element"
    exit 2
fi
itemname=$($XMLSTARLET sel -T -t -v "/project/node/@name" ${file})
assert "test1" $itemname "Query result"

echo "OK"

####
#query test2 node
####

query="name=test2"
params="project=${project}&format=xml&${query}"

echo "TEST: query result for /etc/resources"

$CURL --header "$VERSHEADER" ${runurl}?${params} > ${file}
if [ 0 != $? ] ; then
    errorMsg "ERROR: failed query request"
    exit 2
fi
$XMLSTARLET val -w ${file} > /dev/null 2>&1
if [ 0 != $? ] ; then 
    errorMsg "FAIL: result was not valid xml"
    exit 2
fi

#Check results list
itemcount=$($XMLSTARLET sel -T -t -v "count(/project/node)" ${file})
if [ "1" != "$itemcount" ] ; then
    errorMsg "FAIL: expected single /project/node element"
    exit 2
fi
itemname=$($XMLSTARLET sel -T -t -v "/project/node/@name" ${file})
assert "test2" $itemname "Query result"

echo "OK"

####
#query both nodes node
####

query="tags=testboth"
params="project=${project}&format=xml&${query}"

echo "TEST: query result for /etc/resources"

$CURL --header "$VERSHEADER" ${runurl}?${params} > ${file}
if [ 0 != $? ] ; then
    errorMsg "ERROR: failed query request"
    exit 2
fi
$XMLSTARLET val -w ${file} > /dev/null 2>&1
if [ 0 != $? ] ; then 
    errorMsg "FAIL: result was not valid xml"
    exit 2
fi

#Check results list
itemcount=$($XMLSTARLET sel -T -t -v "count(/project/node)" ${file})
assert "2" $itemcount "Expected two /project/node results"
itemname=$($XMLSTARLET sel -T -t -v "/project/node[@name='test1']/@name" ${file})
assert "test1" "$itemname" "Query for first item"
itemname=$($XMLSTARLET sel -T -t -v "/project/node[@name='test2']/@name" ${file})
assert "test2" "$itemname" "Query for second item"

echo "OK"

####
#exclude both nodes node
####

query="exclude-tags=testboth"
params="project=${project}&format=xml&${query}"

echo "TEST: query result for /etc/resources&$query"

$CURL --header "$VERSHEADER" ${runurl}?${params} > ${file}
if [ 0 != $? ] ; then
    errorMsg "ERROR: failed query request"
    exit 2
fi
$XMLSTARLET val -w ${file} > /dev/null 2>&1
if [ 0 != $? ] ; then 
    errorMsg "FAIL: result was not valid xml"
    exit 2
fi

#Check results list
itemcount=$($XMLSTARLET sel -T -t -v "count(/project/node)" ${file})
assert "1" $itemcount "Expected two /project/node results"
itemname=$($XMLSTARLET sel -T -t -v "/project/node[@name='test1']/@name" ${file})
assert "" "$itemname" "Query for first item"
itemname=$($XMLSTARLET sel -T -t -v "/project/node[@name='test2']/@name" ${file})
assert "" "$itemname" "Query for second item"

echo "OK"


####
#query mixed filters
####

query="tags=testboth&exclude-name=test2"
params="project=${project}&format=xml&${query}"

echo "TEST: query result for /etc/resources, using mixed include/exclude filters"

$CURL --header "$VERSHEADER" ${runurl}?${params} > ${file}
if [ 0 != $? ] ; then
    errorMsg "ERROR: failed query request"
    exit 2
fi
$XMLSTARLET val -w ${file} > /dev/null 2>&1
if [ 0 != $? ] ; then 
    errorMsg "FAIL: result was not valid xml"
    exit 2
fi

#Check results list
itemcount=$($XMLSTARLET sel -T -t -v "count(/project/node)" ${file})
if [ "1" != "$itemcount" ] ; then
    errorMsg "FAIL: expected single /project/node element"
    exit 2
fi
itemname=$($XMLSTARLET sel -T -t -v "/project/node/@name" ${file})
assert "test1" $itemname "Query result"

echo "OK"

####
#query mixed filters, change exclude-precedence=false
####

query="tags=test1&exclude-tags=testboth&exclude-precedence=false"
params="project=${project}&format=xml&${query}"

echo "TEST: /etc/resources, using mixed include/exclude filters, exclude-precedence=false"

$CURL --header "$VERSHEADER" ${runurl}?${params} > ${file}
if [ 0 != $? ] ; then
    errorMsg "ERROR: failed query request"
    exit 2
fi
$XMLSTARLET val -w ${file} > /dev/null 2>&1
if [ 0 != $? ] ; then 
    errorMsg "FAIL: result was not valid xml"
    exit 2
fi


#Check results list
itemcount=$($XMLSTARLET sel -T -t -v "count(/project/node)" ${file})
assert "2" $itemcount "Expected two /project/node results"
itemname=$($XMLSTARLET sel -T -t -v "/project/node[@name='test1']/@name" ${file})
assert "test1" "$itemname" "Query for first item"
itemname=$($XMLSTARLET sel -T -t -v "/project/node[@name='${localnode}']/@name" ${file})
assert "$localnode" "$itemname" "Query for local item"

echo "OK"

rm ${file}
mv $RDECK_BASE/projects/test/etc/resources.xml.backup $RDECK_BASE/projects/test/etc/resources.xml
