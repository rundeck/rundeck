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

COLOR=${NO_COLOR:-yes}
TEST_OK="OK"
TEST_FAIL="FAILED"
if [ "$COLOR" == "yes" ] ; then
    GREEN="\033[0;32m"
    RED="\033[0;31m"
    COLOR_NONE="\033[0m"
    TEST_OK="${GREEN}OK${COLOR_NONE}"
    TEST_FAIL="${RED}FAILED${COLOR_NONE}"
fi
test_ok(){
    echo -e "$1: ${TEST_OK}"
}
test_fail(){
    echo -e "$1: ${TEST_FAIL}"
}
myexit=0
for i in $(ls ./unauthorized-test*.sh)  ; do
    tname=$(basename $i)
    $SHELL ${i} ${URL} &>$DIR/${tname}.output
    if [ $? != 0 ] ; then
        let myexit=2
        test_fail "${i}"
        echo "${i}: ${TEST_FAIL}" >> $DIR/testall.output
        cat $DIR/${tname}.output >> $DIR/testall.output
    else
        test_ok "${i}"
        rm $DIR/${tname}.output
    fi
done

#perform login
rm $DIR/cookies
$SHELL $SRC_DIR/rundecklogin.sh $URL $USER $PASS >/dev/null && test_ok "Login" || die "Login: ${TEST_FAIL}"

TESTS=$(ls ./test-*.sh)
if [ -n "$TEST_NAME" ] ; then
    TESTS=$(ls $TEST_NAME)
fi
for i in $TESTS ; do
    tname=$(basename $i)
    $SHELL ${i} ${URL} &>$DIR/${tname}.output
    if [ $? != 0 ] ; then
        let myexit=2
        test_fail "$i"
        echo "${i}: FAILED" >> $DIR/testall.output
        cat $DIR/${tname}.output >> $DIR/testall.output
    else
        test_ok "$i"
        rm $DIR/${tname}.output
    fi
done

if [ $myexit -ne 0 ] ; then
    cat $DIR/*.output
fi

exit $myexit
