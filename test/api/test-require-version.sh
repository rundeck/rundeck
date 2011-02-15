#!/bin/bash

#test api version in request URL

DIR=$(cd `dirname $0` && pwd)
source $DIR/include.sh

# now submit req
WRONG_VERS="FAKE"
runurl="${RDURL}/api/${WRONG_VERS}/projects"

echo "TEST: Invalid API Version..."

# get listing
sh $DIR/api-expect-error.sh ${runurl} "${params}" "RunDeck API Version is not supported: ${WRONG_VERS}, current version: 1" || exit 2
echo "OK"


WRONG_VERS=$(( $API_VERSION + 1 ))
runurl="${RDURL}/api/${WRONG_VERS}/projects"

echo "TEST: Wrong API Version: ${WRONG_VERS}..."

# get listing
sh $DIR/api-expect-error.sh ${runurl} "${params}" "RunDeck API Version is not supported: ${WRONG_VERS}, current version: 1" || exit 2
echo "OK"


WRONG_VERS="0"
runurl="${RDURL}/api/${WRONG_VERS}/projects"

echo "TEST: Wrong API Version: ${WRONG_VERS}..."

# get listing
sh $DIR/api-expect-error.sh ${runurl} "${params}" "RunDeck API Version is not supported: ${WRONG_VERS}, current version: 1" || exit 2
echo "OK"

WRONG_VERS="000001"
runurl="${RDURL}/api/${WRONG_VERS}/projects"

echo "TEST: Wrong API Version: ${WRONG_VERS}..."

# get listing
sh $DIR/api-expect-error.sh ${runurl} "${params}" "RunDeck API Version is not supported: ${WRONG_VERS}, current version: 1" || exit 2
echo "OK"

rm $DIR/curl.out
