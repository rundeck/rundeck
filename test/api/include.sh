#!/bin/bash

# common header for test scripts

SRC_DIR=$(cd `dirname $0` && pwd)
DIR=${TMP_DIR:-$SRC_DIR}

errorMsg() {
   echo "$*" 1>&2
}
fail(){
    errorMsg "FAIL: $@"
    exit 2
}
test_begin(){
    local msg=$1
    echo "Test: $msg $ENDPOINT"
}
test_succeed(){
    echo "OK"
}
assert(){
    # assert expected, actual
    if [ "$1" != "$2" ] ; then
        fail "Expected value \"$1\" but saw: \"$2\" ${3}"
    fi
}

# modify to point to xmlstarlet
XMLSTARLET=${XMLSTARLET:-xmlstarlet}

RDECK_PROJECTS=${RDECK_PROJECTS:-$RDECK_BASE/projects}
RDECK_ETC=${RDECK_ETC:-$RDECK_BASE/etc}
RDECK_URL=$(grep framework.server.url $RDECK_ETC/framework.properties  | cut -d' ' -f3)

# xmlstarlet select xpath
# usage: xmlsel XPATH file
xmlsel(){
    xpath=$1
    shift
    $XMLSTARLET sel -T -t -v "$xpath" $*
}

API_CURRENT_VERSION=17

API_VERSION=${API_VERSION:-$API_CURRENT_VERSION}


# curl opts to use a cookie jar, and follow redirects, showing only errors
if [ -n "$RDAUTH" ] ; then 
    AUTHHEADER="X-RunDeck-Auth-Token: $RDAUTH"
    CURLOPTS="-s -S -L"
else
    CURLOPTS="-s -S -L -c $DIR/cookies -b $DIR/cookies"
fi

if [ -z "$API_XML_NO_WRAPPER" ] ; then
    CURLOPTS="$CURLOPTS -H X-Rundeck-API-XML-Response-Wrapper:true"
fi

if [ -n "$DEBUG" ] ; then
    CURLOPTS="$CURLOPTS -v"
fi
CURL="curl $CURLOPTS"
docurl(){
    if [ -n "${RDAUTH:-}" ] ; then
        if [ "true" == "${RDDEBUG:-}" ] ; then
            echo $CURL -H "${AUTHHEADER:-}" "$@" 1>&2
        fi
        $CURL -H "${AUTHHEADER:-}" "$@"
    else    
        if [ "true" == "${RDDEBUG:-}" ] ; then
            echo $CURL "$@" 1>&2
        fi
        $CURL "$@"
    fi
}

# accept url argument on commandline, if '-' use default
RDURL="$1"
if [ "-" == "$1" ] ; then
    RDURL=${RDECK_URL:-http://localhost:4440}
fi
shift

APIURL="${RDURL}/api/${API_VERSION}"
CUR_APIURL="${RDURL}/api/${API_CURRENT_VERSION}"


api_request(){
    local ENDPOINT=$1
    local FILE=$2
    local H_ACCEPT=
    if [ -n "${ACCEPT:-}" ] ; then
        H_ACCEPT="-H accept:$ACCEPT"
    fi
    local H_REQUEST_TYPE=
    if [ -n "${TYPE:-}" ] ; then
        H_REQUEST_TYPE="-H content-type:$TYPE"
    fi
    local H_METHOD="-X GET"
    if [ -n "${METHOD:-}" ] ; then
        H_METHOD="-X $METHOD"
    fi
    local H_UPLOAD=
    if [ -n "${POSTFILE:-}" ] ; then
        H_UPLOAD="--data-binary @$POSTFILE"
        if [ -n "$DEBUG" ] ; then
            1>&2 echo "POSTFILE=$POSTFILE" 
            1>&2 echo ">>>>"
            1>&2 cat $POSTFILE
            1>&2 echo ">>>>"
        fi
    fi
    # get listing
    docurl -D $DIR/headers.out $H_METHOD $H_UPLOAD $H_ACCEPT $H_REQUEST_TYPE ${ENDPOINT}?${PARAMS:-} > $FILE
    if [ 0 != $? ] ; then
        fail "ERROR: failed query request"
    fi
    if [ -n "${DEBUG:-}" ] ; then
            1>&2 echo "FILE=$FILE" 
            1>&2 echo "<<<<"
            1>&2 cat $FILE
            1>&2 echo "<<<<"
        fi
    
    assert_http_status ${EXPECT_STATUS:-200} $DIR/headers.out
    METHOD=
    ACCEPT=
    TYPE=
    POSTFILE=
    EXPECT_STATUS=
    
}


##
# utilities for testing http responses
##

assert_http_status(){
    egrep -v "HTTP/1.1 100" $2 | egrep -q "HTTP/1.1 $1"
    if [ 0 != $? ] ; then
        errorMsg "ERROR: Expected $1 result"
        egrep 'HTTP/1.1' $2
        exit 2
    fi
}
##
# utilities for testing xml or json
##

assert_xml_valid(){
    $XMLSTARLET val -w ${1} > /dev/null 2>&1
    if [ 0 != $? ] ; then
        errorMsg "FAIL: Response was not valid xml, file: $1"
        exit 2
    fi

}
##
# assert_xml_value 'value' 'xpath' $file
##
assert_xml_value(){
    local value=$($XMLSTARLET sel -T -t -v "$2" $3)
    if [ $? != 0 -a -n "$1" ] ; then
        errorMsg "xmlstarlet failed: $!: $value, for $1 $2 $3"
        exit 2
    fi
    if [ "$1" != "$value" ] ; then
        errorMsg "XPath $2 wrong value, expected $1 was $value (in file $3)"
        exit 2
    fi
}
assert_xml_notblank(){
    local value=$($XMLSTARLET sel -T -t -v "$1" $2)
    if [ $? != 0 ] ; then
        errorMsg "Expected value for XPath $1, but select failed: $! (in file $2)"
        exit 2
    fi
    if [ "" == "$value" ] ; then
        errorMsg "XPath $1 wrong value, expected (not blank) was $value (in file $2)"
        exit 2
    fi
}

##
# assert_json_value 'value' 'jsonquery' $file
## 
assert_json_value(){
    local JQ=`which jq`

    if [ -z "$JQ" ] ; then
        errorMsg "FAIL: Can't test JSON format, install jq"
        exit 2
    fi
    local propval=$($JQ -r "$2" < $3 )
    if [ $? != 0 ] ; then
        errorMsg "Json query invalid: $2: $!"
        exit 2
    fi
    local expval=$(echo "$1")
    if [ "$expval" != "$propval" ] ; then
        errorMsg "Json query $2 wrong value, expected '$1' was $propval (in file $3)"
        exit 2
    fi
}


##
# assert_json_null  'jsonquery' $file
##
assert_json_null(){
    local JQ=`which jq`

    if [ -z "$JQ" ] ; then
        errorMsg "FAIL: Can't test JSON format, install jq"
        exit 2
    fi
    local propval=$($JQ  "$1" < $2 )
    if [ $? != 0 ] ; then
        errorMsg "Json query invalid: $1: $!"
        exit 2
    fi
    if [ "null" != "$propval" ] ; then
        errorMsg "Json query $1 wrong value, expected null was $propval (in file $2)"
        exit 2
    fi
}
##
# assert_json_not_null  'jsonquery' $file
##
assert_json_not_null(){
    local JQ=`which jq`

    if [ -z "$JQ" ] ; then
        errorMsg "FAIL: Can't test JSON format, install jq"
        exit 2
    fi
    local propval=$($JQ  "$1" < $2 )
    if [ $? != 0 ] ; then
        errorMsg "Json query invalid: $1: $!"
        exit 2
    fi
    if [ "null" == "$propval" ] ; then
        errorMsg "Json query $1 wrong value, expected not null was $propval (in file $2)"
        exit 2
    fi
}
