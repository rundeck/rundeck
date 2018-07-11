#!/bin/bash

set -euo pipefail

DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"

bintray_user=$1
bintray_api_key=$2
bintray_org=$3
bintray_repo=$4

WORKSPACE=release

if [ -d $WORKSPACE ]; then
    rm -r $WORKSPACE
fi

mkdir $WORKSPACE

cp packaging/debdist/*.deb $WORKSPACE


for deb in packaging/debdist/*.deb; do
    bash $DIR/publish-deb-to-bintray.sh $bintray_user $bintray_api_key $bintray_org $bintray_repo $deb
done