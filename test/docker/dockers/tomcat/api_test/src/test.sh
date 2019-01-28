#!/bin/bash

SRC_DIR=$(cd `dirname $0` && pwd)
DIR=${TMP_DIR:-$SRC_DIR}

# call api/testall.sh and use -k curl option to ignore server certificate
# 
bash $SRC_DIR/../api/testall.sh "http://127.0.0.1:8080/rundeck"
#################
# alternate args to curl to use a pem formatted cert to verify server cert:
#sh $SRC_DIR/testweb.sh "https://localhost:4443" "--cacert $RDECK_ETC/rundeck.server.pem"
################


if [ 0 != $? ] ; then
	echo Failed to run testall.sh : $!
	exit 2
fi
