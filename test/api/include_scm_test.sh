#!/bin/bash

#/ Purpose:
#/   Common functions for scm tests
#/ 


SRC_DIR=$(cd `dirname $0` && pwd)
DIR=${TMP_DIR:-$SRC_DIR}
export API_XML_NO_WRAPPER=1
source $SRC_DIR/include.sh

ARGS=$@

tmpdir(){
	tempfoo=`basename $0`
	local TMPDIR=`mktemp -d -t ${tempfoo}.XXX` || exit 1
	echo $TMPDIR
}

create_project(){
	local projectName=$1
	
	ENDPOINT="${APIURL}/projects"
	TMPDIR=`tmpdir`
	tmp=$TMPDIR/create-project.xml
	cat >$tmp <<END
<project>
    <name>$projectName</name>
</project>
END
	TYPE=application/xml
	POSTFILE=$tmp
	METHOD=POST
	EXPECT_STATUS=201
	api_request $ENDPOINT $DIR/curl.out
}
remove_project(){
	local projectName=$1

	ENDPOINT="${APIURL}/project/$projectName"
	METHOD=DELETE
	EXPECT_STATUS=204
	api_request $ENDPOINT $DIR/curl.out
}

baregitfile=$SRC_DIR/git-bare-init.zip

setup_remote(){
	local gitdir=$1
	mkdir -p $gitdir
	cd $gitdir
	unzip -q $baregitfile -d $gitdir
}
remove_testdir(){
	local gitdir=$1
	if [ -d $gitdir/.git ] ; then
		rm -r $gitdir
	fi
}


do_setup_export_json_valid(){
	local integration=$1
	local plugin=$2
	local project=$3

	ENDPOINT="${APIURL}/project/$project/scm/$integration/plugin/$plugin/setup"

	TMPDIR=`tmpdir`/$project
	mkdir -p $TMPDIR
	dirname=$TMPDIR/testdir
	gitdir=$TMPDIR/testgit
	mkdir $dirname
	setup_remote $gitdir

	tmp=$TMPDIR/test_setup_export_xml-upload.json
	cat >$tmp <<END
{
	"config":{
		"dir":"$dirname",
		"url":"$gitdir",
		"committerName":"Git Test",
		"committerEmail":"A@test.com",
		"pathTemplate":"\${job.group}\${job.name}-\${job.id}.\${config.format}",
		"format":"xml",
		"branch":"master",
		"strictHostKeyChecking":"yes"
	}
}
END
	METHOD=POST
	ACCEPT=application/json
	TYPE=application/json
	POSTFILE=$tmp
	EXPECT_STATUS=200

	
	api_request $ENDPOINT $DIR/curl.out

	assert_json_value "true" '.success' $DIR/curl.out
	assert_json_value "SCM Plugin Setup Complete" '.message' $DIR/curl.out
	
}


create_job(){
	local project=$1
	local jobname=$2
	local action=${3:-echo hi}


	TMPDIR=`tmpdir`
	tmp=$TMPDIR/job.xml
	cat >$tmp <<END
<joblist>
  <job>
    <description></description>
    <executionEnabled>true</executionEnabled>
    <loglevel>INFO</loglevel>
    <name>$jobname</name>
    <scheduleEnabled>true</scheduleEnabled>
    <sequence keepgoing='false' strategy='node-first'>
      <command>
        <exec>$action</exec>
      </command>
    </sequence>
  </job>
</joblist>
END
	METHOD=POST
	ENDPOINT="${APIURL}/project/$project/jobs/import"
	ACCEPT=application/xml
	TYPE=application/xml
	POSTFILE=$tmp

	PARAMS="dupeOption=update"

	api_request $ENDPOINT $DIR/curl.out


	assert_xml_value "1" '/result/succeeded/@count' $DIR/curl.out
	local JOBID=$( xmlsel '/result/succeeded/job/id' $DIR/curl.out )

	echo $JOBID
}