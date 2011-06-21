#!/bin/bash

#Utility to log into the RunDeck server and store cookie file for later use

errorMsg() {
   echo "$*" 1>&2
}

DIR=$(cd `dirname $0` && pwd)

# accept url argument on commandline, if '-' use default
url="$1"
if [ "-" == "$1" ] ; then
    url='http://localhost:4440'
fi
apiurl="${url}/api"
if [ -z "$RDAUTH" ] ; then
    loginurl="${url}/j_security_check"
    
    # curl opts to use a cookie jar, and follow redirects, showing only errors
    CURLOPTS="-s -S -L -c $DIR/cookies -b $DIR/cookies"
    CURL="curl $CURLOPTS"
    
    # get main page for login
    RDUSER=${2:-"admin"}
    RDPASS=${3:-"admin"}
    echo "Login..."
    $CURL -d j_username=$RDUSER -d j_password=$RDPASS $loginurl > $DIR/curl.out 
    if [ 0 != $? ] ; then
        errorMsg "failed login request to ${loginurl}"
        exit 2
    fi
    
    grep 'j_security_check' -q $DIR/curl.out 
    if [ 0 == $? ] ; then
        errorMsg "login was not successful"
        exit 2
    fi
fi

echo "Login OK"
