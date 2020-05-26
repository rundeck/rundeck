#!/bin/bash
set -euo pipefail

# Hard-coded default for OSS so scheduler is not confused; set manually for multi-node deployments
RUNDECK_SERVER_UUID="${RUNDECK_SERVER_UUID:-a14bc3e6-75e8-4fe4-a90d-a16dcc976bf6}"

JVM_MAX_RAM_PERCENTAGE="${JVM_MAX_RAM_FRACTION:-75}"