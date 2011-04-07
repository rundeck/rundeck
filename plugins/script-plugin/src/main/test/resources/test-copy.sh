#!/bin/bash

# args are [hostname] [username] [destdir] [filepath]

host=$1
shift
user=$1
shift
dir=$1
shift
file=$1

name=`basename $file`

# copy to node

exec scp $file $user@$host:$dir/$name > /dev/null || exit $?

echo "$dir/$name"