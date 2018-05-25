#!/bin/bash

#exit on error
set -e

bash $HOME/scripts/start_rundeck.sh

# Keep alive
tail -F -n100 \
 $LOGFILE \
 $HOME/var/log/service.log \
 $HOME/server/logs/rundeck.executions.log \
 $HOME/server/logs/rundeck.jobs.log \
 $HOME/server/logs/rundeck.log \
 $HOME/server/logs/rundeck.options.log \
 $HOME/server/logs/rundeck.storage.log