#!/bin/bash

#Usage: 
#    util-resources.sh <URL> <project> <format> [param=value [param=value] .. ]

if [ $# -lt 1 ] ; then 
    echo "Usage: util-projects.sh <URL>"
    exit 2
fi

DIR=$(cd `dirname $0` && pwd)
source $DIR/include.sh


# now submit req
runurl="${APIURL}/projects"
args="$*"
echo "# Listing RunDeck Projects..."



params="${args}"

file=$DIR/curl.out

# get listing
docurl ${runurl}?${params} > ${file} || fail "failed request: ${runurl}"
$SRC_DIR/api-test-success.sh ${file} || exit 2

#Check projects list
itemcount=$($XMLSTARLET sel -T -t -v "count(/result/projects/project)" $DIR/curl.out)
echo "$itemcount Projects"
if [ "0" != "$itemcount" ] ; then
    #echo all on one line
    $XMLSTARLET sel -T -t -m "//project" -v "name"  -o ": &quot;" -v "description" -o '&quot;' -n $DIR/curl.out
fi

rm $DIR/curl.out