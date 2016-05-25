#!/bin/bash

DIR=$(cd `dirname $0` && pwd)
source $DIR/include.sh
proj="test"

# test invalid request

ENDPOINT="${APIURL}/project/$proj/run/url"

EXPECT_STATUS=400

test_begin "should fail with no url param"

api_request $ENDPOINT $DIR/curl.out

$SHELL $SRC_DIR/api-test-error.sh $DIR/curl.out 'parameter "scriptURL" is required'

test_succeed


## test valid request

EXPECT_STATUS=200
TMPDIR=`mktemp -d -t test-run-url.XXX` || exit 1
cat >$TMPDIR/test.sh <<END 
#!/bin/bash

echo this is a script

END

PARAMS="scriptURL=file:$TMPDIR/test.sh"

test_begin "should succeed and return execution id"

api_request $ENDPOINT $DIR/curl.out

$SHELL $SRC_DIR/api-test-success.sh $DIR/curl.out || exit 2

assert_xml_notblank "/result/execution/@id" $DIR/curl.out

test_succeed

rm $DIR/curl.out
rm $TMPDIR/test.sh
