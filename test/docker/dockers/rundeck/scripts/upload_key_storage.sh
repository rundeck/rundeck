#!/usr/bin/env bash

set -eu

SSH_KEY=$1
API_KEY=$2
STORAGE_PATH=$3
HOSTNAME=$RUNDECK_NODE

echo "test key exists $STORAGE_PATH"
set +e
CODE=$(curl -w "%{http_code}" -f -s -G -H 'Accept: application/json' \
	-H "X-rundeck-auth-token:$API_KEY" \
 "http://$HOSTNAME:4440/api/12/storage/keys/$STORAGE_PATH")
result=$?
set -e
if [ "$CODE" == "404" ] ; then
	echo "key not found, uploading $STORAGE_PATH ..."
	rd keys create -f $SSH_KEY -t privateKey $STORAGE_PATH
elif [ $result -eq 0 -o "$CODE" == "200" ] ; then
	echo "key found, re-uploading $STORAGE_PATH ..."
	rd keys update -f $SSH_KEY -t privateKey $STORAGE_PATH
else
	echo "failed, key storage result ${CODE}"
	exit 2
fi
