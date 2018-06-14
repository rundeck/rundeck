#!/bin/bash
#
# del_rpm_from_bintray.sh - henri.gomez@gmail.com
# 
# This script delete a package from Bintray repo
#

set -e
set -u

function usage() {
  echo "$0 username api_key org repo_name rpm_name*"
  exit 0
}

if [ $# -lt 5 ]; then
 usage
fi

BINTRAY_USER=$1
BINTRAY_APIKEY=$2
BINTRAY_ACCOUNT=$3
BINTRAY_REPO=$4

shift;
shift;
shift;
shift;


PRODUCTION_REPOS=("rundeck-deb rundeck-rpm rundeck-maven")

for repo in ${PRODUCTION_REPOS[@]}; do
    echo $repo
    if [[ "${repo}" == ${BINTRAY_REPO} ]] ; then
        >&2 echo "Refusing to refusing to delete from release repo ${repo}"
        exit 1
    fi
done

CURL_CMD="curl --write-out %{http_code} --silent --output /dev/null -u$BINTRAY_USER:$BINTRAY_APIKEY"

for RPM_NAME in $@; do
    echo "Deleting package $RPM_NAME from Bintray repository $BINTRAY_REPO ..."
    HTTP_CODE=`$CURL_CMD -H "Content-Type: application/json" -X DELETE https://api.bintray.com/packages/$BINTRAY_ACCOUNT/$BINTRAY_REPO/$RPM_NAME`

    if [[ "$HTTP_CODE" != "200" && "$HTTP_CODE" != "404" ]]; then
        echo "can't delete package -> $HTTP_CODE"
    else
        echo "Package deleted"
    fi

done