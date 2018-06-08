#!/bin/bash

set -euo pipefail
IFS=$'\n\t'
readonly ARGS=("$@")

NAME=centos6
DIR=docker/rpminstall
TAG="rd$NAME"

build(){
	docker build -t "$TAG-util" -f docker/installcommon/centos6.Dockerfile docker/installcommon
	docker build "$DIR" -t "$TAG"
}
run(){
	docker run -it -v "$PWD:/home/rundeck/rundeck" "$TAG":latest -test
}

build
run