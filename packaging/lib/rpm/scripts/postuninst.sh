#!/bin/sh

if [ "$1" -ge "1" ] ; then
    /sbin/service rundeckd condrestart >/dev/null 2>&1 || :
fi
