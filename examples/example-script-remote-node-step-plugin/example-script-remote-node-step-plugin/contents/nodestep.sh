#!/bin/sh

##
# This script will be executed on each matched node for the workflow.
##

##
# Arguments defined in the plugin.yaml will be passed to the script
##

NODE=$1
EXAMPLE=$2


##
# Config options to the plugin will also be available as environment
# variables, however your SSHD must be configured with AllowEnv RD_*
# to enable this on remote nodes.
##

ASIF=$RD_CONFIG_ASIF
NUMBER=$RD_CONFIG_NUMBER
VAMPIRES=$RD_CONFIG_VAMPIRES

echo "Example script plugin executing on node $NODE"
echo "Arguments: $@"
echo "Environment variable config values: "
echo "Project Example: $RD_CONFIG_PROJECTEXAMPLE"
echo "Framework Example: $RD_CONFIG_FWKEXAMPLE"
echo "Example: $EXAMPLE"
echo "Number: $NUMBER"
echo "Vapires: $VAMPIRES"
echo "Exported Rundeck environment variables: "

env | grep 'RD_' || echo "WARN: no RD_ environment variables found. Check your /etc/ssh/sshd_config and add AcceptEnv RD_*"

exit 0