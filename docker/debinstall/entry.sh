#!/bin/bash
. /rd-util.sh

test -f "$HOME"/rundeck/packaging/debdist/rundeck*.deb || {
	echo "debian not found at $HOME/rundeck/packaging/debdist/rundeck*.deb"
	exit 2
}

dpkg -i "$HOME"/rundeck/packaging/debdist/rundeck*.deb

service rundeckd start
wait_for_start /var/log/rundeck/service.log
exec "$@"