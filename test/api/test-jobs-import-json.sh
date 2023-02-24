#!/bin/bash

#test /api/jobs/import using json formatted content

DIR=$(cd `dirname $0` && pwd)
source $DIR/include.sh

args="echo hello there"

project=$2
if [ "" == "$2" ] ; then
    project="test"
fi

#escape the string for xml
xmlargs=$($XMLSTARLET esc "$args")
xmlproj=$($XMLSTARLET esc "$project")

cat > $DIR/temp.out <<END
[
   {
      "name":"cli job3",
      "group":"api-test/job-import",
      "description":"",
      "loglevel":"INFO",
      "context":{
          "project":"$xmlproj"
      },
      "dispatch":{
        "threadcount":1,
        "keepgoing":true
      },
      "sequence":{
        "commands":[
          {
            "exec":"$xmlargs"
          }
        ]
      }
   }
]
END

# now submit req
runurl="${APIURL}/project/$project/jobs/import"

echo "TEST: import RunDeck Jobs in json format (multipart file)"

params="format=json"

# specify the file for upload with curl, named "xmlBatch"
ulopts="-F xmlBatch=@$DIR/temp.out"

# get listing
docurl $ulopts  ${runurl}?${params} > $DIR/curl.out
if [ 0 != $? ] ; then
    errorMsg "ERROR: failed query request"
    exit 2
fi


#result will contain list of failed and succeeded jobs, in this
#case there should only be 1 failed or 1 succeeded since we submit only 1

failedcount=$(json_val ".failed | length" $DIR/curl.out)
succount=$(json_val ".succeeded | length" $DIR/curl.out)
skipcount=$(json_val ".skipped | length" $DIR/curl.out)

if [ "1" != "$succount" ] ; then
    errorMsg  "Upload was not successful."
    cat $DIR/curl.out 1>&2
    exit 2
else
    echo "OK"
fi

#test form data instead of multipart

echo "TEST: import RunDeck Jobs in json format (urlencode)"

params="format=json"

# specify the file for upload with curl, named "xmlBatch"
ulopts="--data-urlencode xmlBatch@$DIR/temp.out"

# get listing
docurl $ulopts  ${runurl}?${params} > $DIR/curl.out
if [ 0 != $? ] ; then
    errorMsg "ERROR: failed query request"
    exit 2
fi


#result will contain list of failed and succeeded jobs, in this
#case there should only be 1 failed or 1 succeeded since we submit only 1

failedcount=$(json_val ".failed | length" $DIR/curl.out)
succount=$(json_val ".succeeded | length" $DIR/curl.out)
skipcount=$(json_val ".skipped | length" $DIR/curl.out)

if [ "1" != "$succount" ] ; then
    errorMsg  "Upload was not successful."
    exit 2
else
    echo "OK"
fi


#rm $DIR/curl.out
rm $DIR/temp.out

