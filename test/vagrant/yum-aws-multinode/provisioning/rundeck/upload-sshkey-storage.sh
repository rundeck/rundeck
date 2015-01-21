#!/usr/bin/env bash

set -eu

SSH_KEY=$1
API_KEY=$2
STORAGE_PATH=$3

echo "test key exists $STORAGE_PATH"
set +e
CODE=$(curl -w "%{http_code}" -f -s -G -H 'Accept: application/json' -H "X-rundeck-auth-token:$API_KEY" "http://localhost:4440/api/12/storage/keys/$STORAGE_PATH")
result=$?
set -e
if [ "$CODE" == "404" ] ; then
	echo "key not found, uploading $STORAGE_PATH ..."
	curl -f -s -X POST --data-binary @$SSH_KEY -H 'Content-type: application/octet-stream' \
	 -H "X-rundeck-auth-token:$API_KEY" \
   "http://localhost:4440/api/12/storage/keys/$STORAGE_PATH"
elif [ $result -eq 0 -o "$CODE" == "200" ] ; then
	echo "key found, re-uploading $STORAGE_PATH ..."
	curl -f -s -X PUT --data-binary @$SSH_KEY -H 'Content-type: application/octet-stream' \
	 -H "X-rundeck-auth-token:$API_KEY" \
   "http://localhost:4440/api/12/storage/keys/$STORAGE_PATH"
else
	echo "failed, key storage result ${CODE}"
	exit 2
fi
