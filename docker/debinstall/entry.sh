#!/bin/bash

echo "args $*"

test -f $HOME/rundeck/packaging/rundeck*.deb || {
	echo "debian not found at $HOME/rundeck/packaging/rundeck*.deb"
	exit 2
}

dpkg -i $HOME/rundeck/packaging/rundeck*.deb 

service rundeckd start

exec "$@"