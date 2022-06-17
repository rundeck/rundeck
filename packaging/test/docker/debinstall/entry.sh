#!/bin/bash
. /rd-util.sh

set -e

FLAV=${EDITION:-cluster}
DIR=$HOME/build/distributions


install_rundeck(){
	echo "Install Rundeck from file: " "$DIR"/$PACKAGE
	dpkg -i $PACKAGE
}

main(){
	install_rundeck
	cp_license
	echo_config
	entry_start "$@"
}

main "$@"