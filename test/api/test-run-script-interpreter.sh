#!/bin/bash

# TEST: /api/run/script action

DIR=$(cd `dirname $0` && pwd)
source $DIR/include.sh

proj="test"

# now submit req
runurl="${APIURL}/project/${proj}/run/script"

#remove script.out if it exists

OUTF=/tmp/script.out
SCRIPTF=/tmp/script.tmp

####
#  echo a script into a temp file
####
cat <<END > $SCRIPTF
#!/bin/bash
echo \$1 > $OUTF
END

[ -f $OUTF ] && rm $OUTF

run_script(){
  local SCRIPTF=${1};shift
  local params=${1};shift
  # make api request
  docurl -F scriptFile=@$SCRIPTF ${runurl}?${params} > $DIR/curl.out
  if [ 0 != $? ] ; then
      errorMsg "FAIL: failed query request"
      exit 2
  fi

  $SHELL $SRC_DIR/api-test-success.sh $DIR/curl.out || exit 2

  execid=$(jq -r ".execution.id" < $DIR/curl.out)
  if [ "" == "${execid}" ] ; then
      errorMsg "FAIL: expected execution id in result: ${execid}"
      exit 2
  fi
  echo $execid
}
echo "TEST: /api/run/script with scriptInterpreter and interpreterArgsQuoted=true"
params="scriptInterpreter=bash+-c&argString=%24%7Bnode.name%7D&interpreterArgsQuoted=true"
execid=$(run_script $SCRIPTF $params)

##wait for script to execute...
api_waitfor_execution $execid || fail "Waiting for $execid to finish"
$SHELL $SRC_DIR/api-expect-exec-success.sh $execid || exit 2

if [ ! -f $OUTF ] ; then
    errorMsg "FAIL: Expected script to execute and create script.out file"
    exit 2
fi

output=$(cat $OUTF)

if [ -z "$output" ] ; then
    errorMsg "FAIL: Expected script output to have node name in $OUTF"
    exit 2
fi

cat $OUTF
rm $OUTF


echo "TEST: /api/run/script with scriptInterpreter and interpreterArgsQuoted=false"
params="scriptInterpreter=bash+-c&argString=%24%7Bnode.name%7D&interpreterArgsQuoted=false"
execid=$(run_script $SCRIPTF $params)

##wait for script to execute...
api_waitfor_execution $execid || fail "Waiting for $execid to finish"
$SHELL $SRC_DIR/api-expect-exec-success.sh $execid || exit 2

if [ ! -f $OUTF ] ; then
    errorMsg "FAIL: Expected script to execute and create script.out file"
    exit 2
fi

output=$(cat $OUTF)

if [ ! -z "$output" ] ; then
    errorMsg "FAIL: Expected script output to be empty in $OUTF"
    exit 2
fi



echo "OK"

rm $DIR/curl.out
rm $SCRIPTF
