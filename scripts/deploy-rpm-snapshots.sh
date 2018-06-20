#!/bin/bash

set -e
set -u

DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"

bintray_user=$1
bintray_api_key=$2
bintray_org=$3
bintray_repo=$4
release_tag=$5

VCS_URL=https://github.com/rundeck/rundeck.git

WORKSPACE=release

if [ -d $WORKSPACE ]; then
    rm -r $WORKSPACE
fi

mkdir $WORKSPACE

cp packaging/rpmdist/RPMS/**/*.rpm $WORKSPACE

for rpm in $WORKSPACE/*.rpm ; do
    bash $DIR/replace-filename-token.sh $rpm SNAPSHOT $release_tag
done

# (
#     set -e
#     cd $WORKSPACE
#     for rpm in *.rpm ; do
#         PCK_NAME=$(rpm -qp ${rpm} --queryformat "%{NAME}") ;
#         echo "{ \"name\": \"${PCK_NAME}\",
#           \"desc\": \"\",
#           \"vcs_url\": \"${VCS_URL}\",
#           \"licenses\": [\"Apache-2.0\"]}" > bintray-package.json

#         bash $DIR/publish-rpm-to-bintray.sh $bintray_user $bintray_api_key $bintray_org $bintray_repo $rpm
#         rm bintray-package.json
#     done
# )

