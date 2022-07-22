#!/bin/bash

set -euo pipefail

readonly ARGS=("$@")

list_debs(){
  echo "--------Enter .sh listDebs method--------"
  echo "$PWD"
	PATTERN="build/distributions/*.${PACKAGE_TYPE}"
	echo $PATTERN
	echo "--------Leave .sh listDebs method--------"
}

build(){
  echo "--------Enter .sh build method--------"
  echo "$PWD"
	local TAG="rdpro-$COMMON"
	docker build -t "$TAG-util" -f test/docker/installcommon/"$COMMON".Dockerfile test/docker/installcommon
	docker build "$DIR" -t "$TAG"
	echo "--------Leave .sh build method--------"
}

run(){
  echo "--------Leave .sh run method--------"
  echo "$PWD"
	local TAG="rdpro-$COMMON"
	# for EDITION in cluster dr team ; do
	echo "----------BEFORE PARSING DEB LIST----------"
	DEBS=$(list_debs)
	for DEB in $DEBS; do
	  echo "---------THE NAME OF THE DEBIAN FILE---------------"
		echo $DEB
		docker run -it -v "$PWD:/home/rundeck/rundeck" -e "PACKAGE=$DEB" "$TAG":latest -test
	done
	echo "--------Leave .sh run method--------"
}
