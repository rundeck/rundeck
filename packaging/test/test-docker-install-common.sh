#!/bin/bash

set -euo pipefail

readonly ARGS=("$@")

list_debs(){
  echo "----enter list debs-----"
  echo "$PWD"
	PATTERN="build/distributions/*.${PACKAGE_TYPE}"
	echo $PATTERN
	echo "----leave list debs-----"
}

build(){
  echo "----enter build debs-----"
  echo "$PWD"
	local TAG="rdpro-$COMMON"
	docker build -t "$TAG-util" -f packaging/test/docker/installcommon/"$COMMON".Dockerfile packaging/test/docker/installcommon
	docker build "$DIR" -t "$TAG"
	echo "----leave build debs-----"
}

run(){
echo "----enter run debs-----"
	local TAG="rdpro-$COMMON"
	# for EDITION in cluster dr team ; do
	DEBS=$(list_debs)
	for DEB in $DEBS; do
		echo $DEB
		docker run -it -v "$PWD:/home/rundeck/rundeck" -e "PACKAGE=$DEB" "$TAG":latest -test
	done
	echo "----leave run debs-----"
}
