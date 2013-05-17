#!/bin/bash

set -e
set -x
#removes rundeck, so that vagrant provision is faster than destroy/up

service rundeckd stop || echo "rundeck not installed"

yum -y remove rundeck rundeck-config

rm -rf /var/log/rundeck
rm -rf /var/rundeck/projects
rm -rf /etc/rundeck
rm -rf /var/lib/rundeck/*