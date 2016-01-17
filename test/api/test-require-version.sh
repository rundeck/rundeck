#!/bin/bash

#test api version in request URL

DIR=$(cd `dirname $0` && pwd)
source $DIR/include.sh
getpath(){
    echo "$1" | sed 's#^https\{0,1\}://.*:[0-9]*/\(.*\)$#/\1#'
}

# now submit req
WRONG_VERS="FAKE"
runurl="${RDURL}/api/${WRONG_VERS}/projects"
path=$(getpath "$runurl")

echo "TEST: Invalid API Version, $path..."

# get listing
$SHELL $SRC_DIR/api-expect-error.sh ${runurl} "${params}" "Unsupported API Version \"${WRONG_VERS}\". API Request: ${path}. Reason: Current version: ${API_CURRENT_VERSION}" || exit 2
echo "OK"


WRONG_VERS=$(( $API_VERSION + 1000 ))
runurl="${RDURL}/api/${WRONG_VERS}/projects"
path=$(getpath "$runurl")

echo "TEST: Wrong API Version: ${WRONG_VERS}..."

# get listing
$SHELL $SRC_DIR/api-expect-error.sh ${runurl} "${params}" "Unsupported API Version \"${WRONG_VERS}\". API Request: ${path}. Reason: Current version: ${API_CURRENT_VERSION}" || exit 2
echo "OK"


WRONG_VERS="0"
runurl="${RDURL}/api/${WRONG_VERS}/projects"
path=$(getpath "$runurl")

echo "TEST: Wrong API Version: ${WRONG_VERS}..."

# get listing
$SHELL $SRC_DIR/api-expect-error.sh ${runurl} "${params}" "Unsupported API Version \"${WRONG_VERS}\". API Request: ${path}. Reason: Current version: ${API_CURRENT_VERSION}" || exit 2
echo "OK"

WRONG_VERS="000001"
runurl="${RDURL}/api/${WRONG_VERS}/projects"
path=$(getpath "$runurl")

echo "TEST: Wrong API Version: ${WRONG_VERS}..."

# get listing
$SHELL $SRC_DIR/api-expect-error.sh ${runurl} "${params}" "Unsupported API Version \"${WRONG_VERS}\". API Request: ${path}. Reason: Current version: ${API_CURRENT_VERSION}" || exit 2
echo "OK"

rm $DIR/curl.out
