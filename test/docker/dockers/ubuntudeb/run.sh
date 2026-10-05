#!/bin/bash

#exit on error
set -e
set -x

sudo service rundeckd start

LOGS=/var/log/rundeck

# Keep alive
sudo tail -F -n100 \
 $LOGS/service.log \
 $LOGS/rundeck.api.log \
 $LOGS/rundeck.executions.log \
 $LOGS/rundeck.jobs.log \
 $LOGS/rundeck.log \
 $LOGS/rundeck.options.log \
 $LOGS/rundeck.storage.log