#!/usr/bin/env bash

#Your script here
HOST=$1
shift
SOURCE=$1
shift
DESTINATION="$*"

#do a dry run
if [[ "true" == "$RD_CONFIG_DRY_RUN" ]] ; then
    env | grep RD_CONFIG
    echo "Coping  file from $SOURCE to  $HOST: $DESTINATION "
    exit 0

fi

echo "running myscriptplugin"
echo "Example Config: $RD_CONFIG_EXAMPLE"
echo "Example Select Config: $RD_CONFIG_EXAMPLESELECT"
echo "Coping  file from $SOURCE to  $HOST: $DESTINATION "

## Copy file from $SOURCE to $DESTINATION