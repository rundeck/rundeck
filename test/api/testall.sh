#!/bin/bash
DIR=$(cd `dirname $0` && pwd)
cd $DIR
URL=$1
for i in $(ls ./test-*.sh) ; do
    ${i} ${URL} 2>&1 >/dev/null && echo "${i}: OK" || echo "${i}: FAILED"
done