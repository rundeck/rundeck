#!/bin/bash

set -e
# set -u

DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"

bintray_user=$1
bintray_api_key=$2
bintray_org=$3
bintray_repo=$4
build_number=$5

WORKSPACE=release

if [ -d $WORKSPACE ]; then
    rm -r $WORKSPACE
fi

mkdir $WORKSPACE

cp core/build/libs/* $WORKSPACE
cp rundeckapp/build/libs/*.{jar,war} $WORKSPACE

# remove other artifacts we don't want and can't exclude in the copy artifacts step
rm $WORKSPACE/*-sources.jar $WORKSPACE/*-tests.jar $WORKSPACE/*-javadoc.jar

for pkg in rundeck-core rundeck rundeckapp ; do
    bash $DIR/delete-bintray-package.sh $bintray_user $bintray_api_key $bintray_org $bintray_repo $pkg;
done


for jar in $WORKSPACE/*.jar $WORKSPACE/*.war ; do
    #rename any SNAPSHOT to something else
    bash $DIR/replace-filename-token.sh $jar SNAPSHOT $build_number
done

for war in $WORKSPACE/*.war ; do 
    bash $DIR/replace-filename-token.sh $war rundeck- rundeckapp-
done

for jar in $WORKSPACE/*.jar $WORKSPACE/*.war ; do
    bash $DIR/publish-jar-to-bintray.sh $bintray_user $bintray_api_key $bintray_org $bintray_repo $jar "org/rundeck/" ;
done