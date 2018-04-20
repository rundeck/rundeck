#!/bin/bash
########
#/ test getting resources from a specific source for a project
########
TEST_DESC="test getting resources from a specific source for a project"

DIR=$(cd `dirname $0` && pwd)
export API_VERSION=23 #/api/23/project/NAME/sources
source $DIR/include.sh

file=$DIR/curl.out
localnode=$(grep 'framework.server.name' $RDECK_ETC/framework.properties | sed 's/framework.server.name = //')

test_proj="APIProjectResourceGetTest"


test_begin "Get project source resources"
res_file1=$RDECK_BASE/projects/$test_proj/etc/resources1.xml
res_file2=$RDECK_BASE/projects/$test_proj/etc/resources2.xml
# Create a project
ENDPOINT="${APIURL}/projects"
tmp_file=$DIR/proj_create_$test_proj.post.json
cat > $tmp_file <<END
{
    "name": "$test_proj",
    "description":"$TEST_DESC",
    "config": {
        "resources.source.1.config.file":"$res_file1",
        "resources.source.1.config.format":"resourcexml",
        "resources.source.1.config.generateFileAutomatically":"false",
        "resources.source.1.config.includeServerNode":"false",
        "resources.source.1.config.requireFileExists":"false",
        "resources.source.1.config.writeable":"true",
        "resources.source.1.type":"file",

        "resources.source.2.config.file":"$res_file2",
        "resources.source.2.config.format":"resourcexml",
        "resources.source.2.config.generateFileAutomatically":"true",
        "resources.source.2.config.includeServerNode":"true",
        "resources.source.2.config.requireFileExists":"false",
        "resources.source.2.config.writeable":"false",
        "resources.source.2.type":"file"
    }
}
END
TYPE='application/json'
METHOD='POST'
POSTFILE=$tmp_file
ACCEPT="application/json"
EXPECT_STATUS=201
api_request $ENDPOINT $file
rm $tmp_file

ENDPOINT="${APIURL}/project/$test_proj/sources"
ACCEPT="application/json"
api_request $ENDPOINT $file
assert_json_value '2' 'length' $file
assert_json_value 'true' '.[0].resources.writeable' $file
assert_json_value 'true' '.[0].resources.empty' $file
assert_json_value 'false' '.[1].resources.writeable' $file

# auto server node
ENDPOINT="${APIURL}/project/$test_proj/source/2/resources"
ACCEPT="application/json"
api_request $ENDPOINT $file
assert_json_value '1' 'length' $file
assert_json_not_null ".[\"$localnode\"].nodename" $file
assert_json_not_null ".[\"$localnode\"].hostname" $file
assert_json_not_null ".[\"$localnode\"].osFamily" $file

# empty resources data
ENDPOINT="${APIURL}/project/$test_proj/source/1/resources"
ACCEPT="application/json"
api_request $ENDPOINT $file
assert_json_value '0' 'length' $file

# post resources data
tmp_file=$DIR/resources_create_$test_proj.post.json
cat > $tmp_file <<END
{
    "mynode1": {
        "nodename":"mynode1",
        "hostname":"mynode1",
        "attr1":"testvalue",
        "tags":"api, test"
    }
}
END
ENDPOINT="${APIURL}/project/$test_proj/source/1/resources"
ACCEPT="application/json"
TYPE="application/json"
POSTFILE=$tmp_file
METHOD='POST'
api_request $ENDPOINT $file

assert_json_value '1' 'length' $file
assert_json_value "mynode1" ".mynode1.nodename" $file
assert_json_value "mynode1" ".mynode1.hostname" $file
assert_json_value "testvalue" ".mynode1.attr1" $file
assert_json_value "api, test" ".mynode1.tags" $file

test_succeed

# delete project


ENDPOINT="${APIURL}/project/$test_proj"
METHOD='DELETE'
EXPECT_STATUS=204
api_request $ENDPOINT $file
rm $file $res_file1 $res_file2 || echo "files not present"
