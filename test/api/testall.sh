#!/bin/bash
SRC_DIR=$(cd `dirname $0` && pwd)
DIR=${TMP_DIR:-$SRC_DIR}
cd $SRC_DIR
URL=${1:-"http://localhost:4440"}

die() {
   echo "$*" 1>&2
   exit 2
}
USER=${2:-"admin"}
PASS=${3:-"admin"}

#perform unauthorized tests

myexit=0
for i in $(ls ./unauthorized-test*.sh)  ; do
    sh ${i} ${URL} >/dev/null
    if [ $? != 0 ] ; then
        let myexit=2
        echo "${i}: FAILED"
    else
        echo "${i}: OK"
    fi
done

#perform login
rm $DIR/cookies
sh $SRC_DIR/rundecklogin.sh $URL $USER $PASS >/dev/null && echo "Login: OK" || die "Login: FAILED"

for i in $(ls ./test-*.sh) ; do
    sh ${i} ${URL} >/dev/null
    if [ $? != 0 ] ; then
        let myexit=2
        echo "${i}: FAILED"
    else
        echo "${i}: OK"
    fi
done

exit $myexit
