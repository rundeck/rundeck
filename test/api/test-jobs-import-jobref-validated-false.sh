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
          <uuid>28a1fc62-92a1-4a45-bbc3-02a8b0f46af0</uuid>
        </jobref>
      </command>
    </sequence>
    <uuid>a6d88d66-920b-492b-b7c4-1a22da67333c</uuid>
  </job>
</joblist>

END

uploadJob "$DIR/temp.out" "$project" 1 "validateJobref=false"

jobid="a6d88d66-920b-492b-b7c4-1a22da67333b"

rm $DIR/curl.out
echo "OK"
