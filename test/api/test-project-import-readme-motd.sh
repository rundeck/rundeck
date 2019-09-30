#!/bin/bash

#test GET /api/11/project/name/import
#using API v13
# use api V13
API_VERSION=13
API_XML_NO_WRAPPER=true

DIR=$(cd `dirname $0` && pwd)
source $DIR/include.sh



##
# SETUP: create project, import job
##



create_proj(){
    projname=$1
    cat > $DIR/proj_create.post <<END
<project>
    <name>$projname</name>
    <description>test1</description>
    <config>
        <property key="test.property" value="test value"/>
    </config>
</project>
END

    runurl="${APIURL}/projects"

    # post
    docurl -X POST -D $DIR/headers.out --data-binary @$DIR/proj_create.post \
        -H Content-Type:application/xml ${runurl}?${params} > $DIR/curl.out
    if [ 0 != $? ] ; then
        errorMsg "ERROR: failed POST request"
        exit 2
    fi
    rm $DIR/proj_create.post
    assert_http_status 201 $DIR/headers.out
}
delete_proj(){
    projname=$1

    runurl="${APIURL}/project/$projname"
    docurl -X DELETE  ${runurl} > $DIR/curl.out
    if [ 0 != $? ] ; then
        errorMsg "ERROR: failed DELETE request"
        exit 2
    fi
}
assert_readme(){
    projname=$1
    runurl="${APIURL}/project/$projname/readme.md"
    docurl -D $DIR/headers.out ${runurl} > $DIR/curl.out
    if [ 0 != $? ] ; then
        errorMsg "ERROR: failed query request"
        exit 2
    fi
    assert_http_status 200 $DIR/headers.out
    value=$(cat $DIR/curl.out)


    assert "this is a readme file" "$value"
}

assert_motd(){
    projname=$1
    runurl="${APIURL}/project/$projname/motd.md"
    docurl -D $DIR/headers.out ${runurl} > $DIR/curl.out
    if [ 0 != $? ] ; then
        errorMsg "ERROR: failed query request"
        exit 2
    fi
    assert_http_status 200 $DIR/headers.out
    value=$(cat "$DIR/curl.out")
    assert "this is a message of the day" "$value"
}
