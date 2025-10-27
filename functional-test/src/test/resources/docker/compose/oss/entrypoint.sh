#!/bin/bash

chown -R rundeck /home/rundeck/privatekeys
chmod 755 /home/rundeck/privatekeys

exec /home/rundeck/docker-lib/entry.sh "$@"