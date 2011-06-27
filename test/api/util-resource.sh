#!/bin/bash

#Usage: 
#    util-resource.sh <URL> <project> <format> <name>

if [ $# -lt 4 ] ; then 
    echo "Usage: util-resource.sh <URL> <project>  <format> <name>"
    exit 2
fi

DIR=$(cd `dirname $0` && pwd)
source $DIR/include.sh

proj=${1}
shift

format=${1}
shift

name=$1

# now submit req
runurl="${APIURL}/resource/${name}"

echo "# Getting RunDeck Resource in project ${proj} named ${name}..."

params="project=${proj}&format=${format}"

file=$DIR/curl.out

# get listing
docurl  ${runurl}?${params} > ${file}|| fail "failed request: ${runurl}"

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
            errorMsg "ERROR: $errmsg"
            exit 2
        fi
    fi
fi

if [ "xml" == "$format" ] ; then 
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
    
    #Check projects list
    itemcount=$(xmlsel "count(/project/node)" ${file})
    echo "$itemcount Nodes"
    if [ "0" != "$itemcount" ] ; then
        #echo all on one line
        $XMLSTARLET sel -T -t -m "/project/node" -v "@name"  -o ": &quot;" -v "@description" -o '&quot;' -o ": " -v "@username" -o "@" -v "@hostname" -o " (" -v "@tags" -o ")" -n ${file}
    fi
else
    #echo output
    cat ${file}
fi

rm ${file}
