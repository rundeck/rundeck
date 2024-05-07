#!/bin/bash
. /rd-util.sh

set -e

FLAV=${EDITION:-cluster}

install_rundeck(){
	echo "Install Rundeck from file: " $PACKAGE
	dpkg -i "$PACKAGE"
}

main(){
	install_rundeck
	cp_license
	echo_config
	entry_start "$@"
}

main "$@"