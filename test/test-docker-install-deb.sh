#!/bin/bash

set -euo pipefail
IFS=$'\n\t'
readonly ARGS=("$@")

NAME=ubuntu16.04
DIR=docker/debinstall
TAG="rd$NAME"

build(){
	docker build -t "$TAG-util" -f docker/installcommon/ubuntu.Dockerfile docker/installcommon
	docker build "$DIR" -t "$TAG"
}
run(){
	docker run -it -v "$PWD:/home/rundeck/rundeck" "$TAG":latest -test
}

build
run