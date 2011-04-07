#!/bin/bash

# args are [hostname] [username] -- [command to exec...]

host=$1
shift
user=$1
shift
command="$*"

exec ssh $user@$host $command