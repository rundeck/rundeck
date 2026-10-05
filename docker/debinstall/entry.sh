#!/bin/bash
. /rd-util.sh

test -f "$HOME"/packaging/packaging/debdist/rundeck*.deb || {
	echo "debian not found at $HOME/packaging/packaging/debdist/rundeck*.deb"
	exit 2
}

dpkg -i "$HOME"/packaging/packaging/debdist/rundeck*.deb

entry_start "$*"
