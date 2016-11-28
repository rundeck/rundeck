#!/bin/bash

SRC_DIR=$(cd `dirname $0` && pwd)
DIR=${TMP_DIR:-$SRC_DIR}

RDECK_ETC=${RDECK_ETC:-$RDECK_BASE/etc}
# modify to point to xmlstarlet
XMLSTARLET=${XMLSTARLET:-xmlstarlet}


#assert RDECK_BASE
if [ -z "$RDECK_BASE" ] ; then
    echo "RDECK_BASE not set"
    exit 2
fi

cd $RDECK_BASE 
if [ 0 != $? ] ; then
	echo Failed to cd to $RDECK_BASE : $!
	exit 2
fi


RDECK_URL=$(grep framework.server.url $RDECK_ETC/framework.properties  | cut -d' ' -f3)

egrep 'https://' $RDECK_ETC/framework.properties > /dev/null
https=$?

if [ 0 = $https ] ; then
    # call api/testall.sh and use -k curl option to ignore server certificate
    # 
    bash $SRC_DIR/../api/testall.sh "$RDECK_URL" -k
    #################
    # alternate args to curl to use a pem formatted cert to verify server cert:
    #sh $SRC_DIR/testweb.sh "https://localhost:4443" "--cacert $RDECK_ETC/rundeck.server.pem"
    ################
else
    bash $SRC_DIR/../api/testall.sh "$RDECK_URL"
fi

if [ 0 != $? ] ; then
	echo Failed to run testall.sh : $!
	exit 2
fi
