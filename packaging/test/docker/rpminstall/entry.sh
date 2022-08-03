#!/bin/bash
. /rd-util.sh

set -e

FLAV=${EDITION:-cluster}
DIR=$HOME/"${PACKAGING_DIR_PARENT}"packaging/packaging/rundeck/build/distributions


install_rundeck(){
	echo "Install Rundeck from file: " $DIR/$PACKAGE
	rpm -ivh $PACKAGE 
}

main(){
	install_rundeck
	cp_license
	echo_config
	entry_start "$@"
}


main "$@"