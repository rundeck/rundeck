#!/bin/bash

set -eou pipefail

bash clean.sh || echo 'clean had errors..continuing'

for composefile in $(ls -1 docker-compose-*.yml); do
  docker-compose -f $composefile build
  docker-compose -f $composefile up
done







