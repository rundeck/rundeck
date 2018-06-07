#!/bin/bash

set -e
set -u

DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"

bintray_user=$1
bintray_api_key=$2
bintray_org=$3
bintray_repo=$4
build_number=$5

for deb in packaging/debdist/*.deb; do
    bash $DIR/publish-deb-to-bintray.sh $bintray_user $bintray_api_key $bintray_org $bintray_repo $deb "SNAPSHOT-${build_number}"
done