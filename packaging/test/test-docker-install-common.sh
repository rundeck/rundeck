#!/bin/bash

set -euo pipefail

readonly ARGS=("$@")

list_debs(){
	PATTERN="packaging/packaging/build/distributions/*.${PACKAGE_TYPE}"
	echo $PATTERN
}

build(){
  echo "$PWD"
	local TAG="rdpro-$COMMON"
	docker build -t "$TAG-util" -f packaging/test/docker/installcommon/"$COMMON".Dockerfile packaging/test/docker/installcommon
	docker build "$DIR" -t "$TAG"
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
		docker run -it -v "$PWD:/home/rundeck/rundeck" -e "PACKAGE=$DEB" "$TAG":latest -test
	done
}
