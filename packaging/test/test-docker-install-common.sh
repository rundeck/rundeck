#!/bin/bash

set -euo pipefail

readonly ARGS=("$@")

list_debs(){
	PATTERN="${PACKAGING_DIR}/packaging/build/distributions/*.${PACKAGE_TYPE}"
	echo $PATTERN
}

build(){
  echo "$PWD"
	local TAG="rdpro-$COMMON"
	docker build -t "$TAG-util" -f "${PACKAGING_DIR}/test/docker/installcommon/${COMMON}.Dockerfile" "${PACKAGING_DIR}/test/docker/installcommon"
}

run(){
	local TAG="rdpro-$COMMON"
	echo "---------- PWD -------------"
	echo "$PWD"
	echo "---------- list_debs -------------"
	# for EDITION in cluster dr team ; do
	DEBS=$(list_debs)
	for DEB in $DEBS; do
		echo $DEB
		cp -pv "$DEB" "$DIR/rundeck.${PACKAGE_TYPE}"
		docker build --no-cache "$DIR" -t "$TAG":latest
		docker run -it -e "PACKAGE=/rundeck/rundeck.${PACKAGE_TYPE}" "$TAG":latest -test
	done
}
