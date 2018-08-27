#!/bin/bash

export REMCO_HOME=../../remco

export RUNDECK_HOME=.tmp/rundeck
export REMCO_RESOURCE_DIR=$REMCO_HOME/resources.d
export REMCO_TEMPLATE_DIR=$REMCO_HOME/templates
export REMCO_TMP_DIR=.tmp

if [[ ! -d .tmp ]] ; then
  mkdir -p .tmp/framework
  mkdir -p .tmp/rundeck-config
  mkdir -p .tmp/rundeck/etc
  mkdir -p .tmp/rundeck/server/config
fi

remco -config config.toml