#!/bin/bash

#usage: rdjobslist.sh <server URL> <project> [output.xml]

errorMsg() {
   echo "$*" 1>&2
}

DIR=$(cd `dirname $0` && pwd)

# accept url argument on commandline, if '-' use default
url="$1"
if [ "-" == "$1" ] ; then
    url='http://localhost:4440/api'
fi
apiurl="${url}/api"
loginurl="${url}/j_security_check"
VERSHEADER="X-RUNDECK-API-VERSION: 1.2"

# curl opts to use a cookie jar, and follow redirects, showing only errors
CURLOPTS="-s -S -L -c $DIR/cookies -b $DIR/cookies"
CURL="curl $CURLOPTS"

# get main page for login
RDUSER=default
RDPASS=default
echo "Login..."
$CURL --header "$VERSHEADER" -d j_username=$RDUSER -d j_password=$RDPASS $loginurl > $DIR/curl.out 
if [ 0 != $? ] ; then
    errorMsg "failed login request to ${loginurl}"
    exit 2
fi

grep 'j_security_check' -q $DIR/curl.out 
if [ 0 == $? ] ; then
    errorMsg "login was not successful"
    exit 2
fi

echo "Login OK"

XMLSTARLET=xml

# now submit req
runurl="${apiurl}/projects"

echo "Listing RunDeck projects..."

# get listing
$CURL --header "$VERSHEADER" ${runurl}?${params} > $DIR/curl.out
if [ 0 != $? ] ; then
    errorMsg "ERROR: failed query request"
    exit 2
fi

#test curl.out for valid xml
$XMLSTARLET val -w $DIR/curl.out > /dev/null 2>&1
if [ 0 != $? ] ; then
    errorMsg "ERROR: Response was not valid xml"
    exit 2
fi

#test for expected /joblist element
$XMLSTARLET el $DIR/curl.out | grep -e '^result' -q
if [ 0 != $? ] ; then
    errorMsg "ERROR: Response did not contain expected result"
    exit 2
fi

# job list query doesn't wrap result in common result wrapper
#If <result error="true"> then an error occured.
waserror=$($XMLSTARLET sel -T -t -v "/result/@error" $DIR/curl.out)
if [ "true" == "$waserror" ] ; then
    errorMsg "Server reported an error: "
    $XMLSTARLET sel -T -t -v "/result/error/message" -n  $DIR/curl.out
    exit 2
fi

#Check projects list
itemcount=$($XMLSTARLET sel -T -t -v "/result/projects/@count" $DIR/curl.out)
echo "$itemcount Projects"    
if [ "0" != "$itemcount" ] ; then
    #echo all on one line
    $XMLSTARLET sel -T -t -m "/result/projects/project"  -v "name" -o ": &quot;" -v "description" -o '&quot; &lt;' -v "resources/providerURL" -o "&gt;" -n $DIR/curl.out
fi




#rm $DIR/curl.out
rm $DIR/cookies

