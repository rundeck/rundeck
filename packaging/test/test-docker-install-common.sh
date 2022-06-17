#!/bin/bash

set -euo pipefail

readonly ARGS=("$@")

list_debs(){
	PATTERN="build/distributions/*.${PACKAGE_TYPE}"
	echo $PATTERN
}

build(){
	local TAG="rdpro-$COMMON"
	docker build -t "$TAG-util" -f test/docker/installcommon/"$COMMON".Dockerfile test/docker/installcommon
	docker build "$DIR" -t "$TAG"
}

run(){
	local TAG="rdpro-$COMMON"
	# for EDITION in cluster dr team ; do
	DEBS=$(list_debs)
	for DEB in $DEBS; do
		echo $DEB
		docker run -it -v "$PWD:/home/rundeck/rundeck" -e "PACKAGE=$DEB" "$TAG":latest -test
	done
}
