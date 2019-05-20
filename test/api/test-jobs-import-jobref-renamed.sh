#!/bin/bash

#test /api/jobs/import

DIR=$(cd `dirname $0` && pwd)
source $DIR/include.sh

args="echo hello there"

project=$2
if [ "" == "$2" ] ; then
    project="test"
fi

#escape the string for xml
xmlargs=$($XMLSTARLET esc "$args")
xmlproj=$($XMLSTARLET esc "$project")

#produce job.xml content corresponding to the dispatch request
cat > $DIR/temp.out <<END
<joblist>
  <job>
    <context>
      <options preserveOrder='true'>
        <option name='Time' required='true' />
      </options>
    </context>
    <defaultTab>summary</defaultTab>
    <description></description>
    <executionEnabled>true</executionEnabled>
    <group>Manage</group>
    <id>28a1fc62-92a1-4a45-bbc3-02a8b0f46af8</id>
    <loglevel>INFO</loglevel>
    <name>RefMe</name>
    <nodeFilterEditable>false</nodeFilterEditable>
    <scheduleEnabled>true</scheduleEnabled>
    <sequence keepgoing='false' strategy='node-first'>
      <command>
        <exec>sleep \${option.Time}</exec>
      </command>
    </sequence>
    <uuid>28a1fc62-92a1-4a45-bbc3-02a8b0f46af8</uuid>
  </job>
  <job>
    <context>
      <options preserveOrder='true'>
        <option name='Foo' />
      </options>
    </context>
    <defaultTab>summary</defaultTab>
    <description></description>
    <executionEnabled>true</executionEnabled>
    <group>Manage</group>
    <id>a6d88d66-920b-492b-b7c4-1a22da67333b</id>
    <loglevel>INFO</loglevel>
    <name>Wrapper</name>
    <nodeFilterEditable>false</nodeFilterEditable>
    <scheduleEnabled>true</scheduleEnabled>
    <sequence keepgoing='false' strategy='node-first'>
      <command>
        <jobref name='RefMe'>
          <arg line='-Time \${option.Foo}' />
          <uuid>28a1fc62-92a1-4a45-bbc3-02a8b0f46af8</uuid>
        </jobref>
      </command>
    </sequence>
    <uuid>a6d88d66-920b-492b-b7c4-1a22da67333b</uuid>
  </job>
</joblist>

END

# now submit req
runurl="${APIURL}/project/$project/jobs/import"

params=""

# specify the file for upload with curl, named "xmlBatch"
ulopts="-F xmlBatch=@$DIR/temp.out"

# get listing
docurl $ulopts  ${runurl}?${params} > $DIR/curl.out
if [ 0 != $? ] ; then
    errorMsg "ERROR: failed query request"
    exit 2
fi

$SHELL $SRC_DIR/api-test-success.sh $DIR/curl.out || exit 2

#result will contain list of failed and succeeded jobs, in this
#case there should only be 1 failed or 1 succeeded since we submit only 1

succount=$($XMLSTARLET sel -T -t -v "/result/succeeded/@count" $DIR/curl.out)
jobid="a6d88d66-920b-492b-b7c4-1a22da67333b"
#$($XMLSTARLET sel -T -t -v "/result/succeeded/job/id" $DIR/curl.out)

if [ "2" != "$succount" -o "" == "$jobid" ] ; then
    errorMsg  "Upload was not successful."
    exit 2
fi


rm $DIR/curl.out
echo "OK"
