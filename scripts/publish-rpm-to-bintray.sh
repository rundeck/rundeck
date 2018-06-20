#!/bin/bash
#The MIT License
#
#Copyright (c) 2012, Daniel Petisme
#
# Permission is hereby granted, free of charge, to any person obtaining a copy
# of this software and associated documentation files (the "Software"), to deal
# in the Software without restriction, including without limitation the rights
# to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
# copies of the Software, and to permit persons to whom the Software is
# furnished to do so, subject to the following conditions:
#
# The above copyright notice and this permission notice shall be included in
# all copies or substantial portions of the Software.
#
# THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
# IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
# FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
# AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
# LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
# OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
# THE SOFTWARE.


# This script aims to ease the publication of the freshly built devops rpms to the
# content manager https://bintray.com/ It's a lazy script so you won't found any
# CLI controls...

set -e
set -u

#Constants
API=https://api.bintray.com
NOT_FOUND=404
SUCCESS=200
CREATED=201
PACKAGE_DESCRIPTOR=bintray-package.json

# Arguments
# $1 SUBJECT aka. your BinTray username
# $2 API_KEY act as a password for REST authentication
# $3 ORG bintray org may be different than username
# $4 REPO the targeted repo
# $5 the rpm to deploy on BinTray 

function main() {
  SUBJECT=$1
  API_KEY=$2
  BINTRAY_ORG=$3
  REPO=$4
  RPM=$5
  RPM_FILE=`basename ${RPM}`

  PCK_NAME=$(rpm -qp ${RPM} --queryformat "%{NAME}")
  PCK_VERSION=$(rpm -qp ${RPM} --qf "%{VERSION}")
  PCK_RELEASE=$(rpm -qp ${RPM} --qf "%{RELEASE}")

  if [ -z "$PCK_NAME" ] || [ -z "$PCK_VERSION" ] || [ -z "$PCK_RELEASE" ]; then
   echo "no RPM metadata information in $RPM_FILE, aborting..."
   exit -1
  fi
  
  echo "[DEBUG] SUBJECT    : ${SUBJECT}"
  echo "[DEBUG] ORG        : ${BINTRAY_ORG}"
  echo "[DEBUG] REPO       : ${REPO}"
  echo "[DEBUG] RPM_PATH   : ${RPM}"
  echo "[DEBUG] RPM        : ${RPM_FILE}"
  echo "[DEBUG] PCK_NAME   : ${PCK_NAME}"
  echo "[DEBUG] PCK_VERSION: ${PCK_VERSION}"
  echo "[DEBUG] PCK_RELEASE: ${PCK_RELEASE}"
  
  init_curl
  if ( check_package_exists ); then
    echo "[DEBUG] The package ${PCK_NAME} does not exit. It will be created"
    create_package        
  fi
  
  deploy_rpm
}

function init_curl() {
  CURL="curl -u${SUBJECT}:${API_KEY} -H Content-Type:application/json -H Accept:application/json"
}

function check_package_exists() {
  echo "[DEBUG] Checking if package ${PCK_NAME} exists..."
  package_exists=`[  $(${CURL} --write-out %{http_code} --silent --output /dev/null -X GET  ${API}/packages/${BINTRAY_ORG}/${REPO}/${PCK_NAME})  -eq ${SUCCESS} ]`
  echo "[DEBUG] Package ${PCK_NAME} exists? y:1/N:0 ${package_exists}"   
  return ${package_exists} 
}

function create_package() {
  echo "[DEBUG] Creating package ${PCK_NAME}..."
  #search for a descriptor in the current folder or generate one on the fly
  if [ -f "${PACKAGE_DESCRIPTOR}" ]; then
    data="@${PACKAGE_DESCRIPTOR}"
  else
    data="{
    \"name\": \"${PCK_NAME}\",
    \"desc\": \"\",
    \"vcs_url\": \"${VCS_URL}\",
    \"licenses\": [\"Apache-2.0\"]
    }"
  fi
  
  ${CURL} -X POST  -d  "${data}" ${API}/packages/${BINTRAY_ORG}/${REPO}/
}

function upload_content() {
  echo "[DEBUG] Uploading ${RPM_FILE}..."
  [ $(${CURL} --write-out %{http_code} --silent --output /dev/null -T ${RPM} -H X-Bintray-Package:${PCK_NAME} -H X-Bintray-Version:${PCK_VERSION}-${PCK_RELEASE} ${API}/content/${BINTRAY_ORG}/${REPO}/${RPM_FILE}) -eq ${CREATED} ]
  uploaded=$?
  echo "[DEBUG] RPM ${RPM_FILE} uploaded? y:1/N:0 ${uploaded}"
  return ${uploaded}
}
function deploy_rpm() {
  
  if ( upload_content); then
    echo "[DEBUG] Publishing ${RPM_FILE}..."
    ${CURL} -X POST ${API}/content/${BINTRAY_ORG}/${REPO}/${PCK_NAME}/${PCK_VERSION}-${PCK_RELEASE}/publish -d "{ \"discard\": \"false\" }"
  else
    echo "[SEVERE] First you should upload your rpm ${RPM_FILE}"
    exit 2
  fi    
}

main "$@"
