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
    TEST_OK="${GREEN}OK${COLOR_NONE}    "
    TEST_FAIL="${RED}FAILED${COLOR_NONE}"
fi
test_ok(){
    echo -e "${TEST_OK} $1"
}
test_fail(){
    echo -e "${TEST_FAIL} $1"
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

# prepare a new project
#$SHELL $SRC_DIR/prepare.sh ${URL} 'test'

TESTS=$(ls test-*.sh)
if [ -n "$TEST_NAME" ] ; then
    TESTS=$(ls $TEST_NAME)
fi
for i in $TESTS ; do
    tname=$(basename $i)
    $SHELL ${i} ${URL} &>$DIR/${tname}.output
    if [ $? != 0 ] ; then
        if [ -f $DIR/curl.out ] ; then
            cat $DIR/curl.out >>  $DIR/${tname}.output
        fi
        let myexit=2
        test_fail "$i"
        echo "*****************" >> $DIR/testall.output
        echo "${i}: FAILED" >> $DIR/testall.output
        echo "*****************" >> $DIR/testall.output
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
