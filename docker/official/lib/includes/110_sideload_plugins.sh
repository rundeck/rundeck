#!/bin/bash
set -eou pipefail

# Copy the plugins instead of moving them since it requires fewer permissions
# And typically does not actually save space in a containerized environment.
find /home/rundeck/plugins-to-sideload/ -mindepth 1 -name '*' -exec cp -r {} /home/rundeck/libext/ \;
