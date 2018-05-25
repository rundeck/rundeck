#!/bin/bash
# Check we are in correct dir.
SCRIPTFILE=$(readlink -f "$0")
SCRIPTDIR=$(dirname "$SCRIPTFILE")
cd $SCRIPTDIR

for composefile in $(ls -1 docker-compose-*.yml); do
  echo "=== Purging resources from $composefile..."
  docker-compose -f $composefile down --volumes --remove-orphans
done







