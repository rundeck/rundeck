#!/bin/bash
set -e
set -u

if [ $# -lt 3 ] ; then
    echo "Usage: $0 filename replace value"
    exit 1
fi
FILE=$1
REPL=$2
NEW=$3

NAME=$(basename $FILE)

function replace(){
    echo $1 | sed "s/$2/$3/"
}
NNAME=$(replace $NAME $REPL $NEW)

echo "Renaming $NAME to $NNAME"

mv $(dirname $FILE)/$NAME $(dirname $FILE)/$NNAME