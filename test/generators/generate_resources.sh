#!/bin/bash

# generate a resources.xml file using random names
# usage: generate_resouces.sh <count> <tagcount>
# will generate <count> resources with <tagcount> tags each

export LC_ALL=C

genrand() {
    cat /dev/random | tr -dc ${2:-'_.A-Za-z1-9'} | head -c${1:-1024}
}

cat <<END
<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE project PUBLIC "-//DTO Labs Inc.//DTD Resources Document 1.0//EN" "project.dtd">

<project>
END

WORDLIST=/usr/share/dict/words
NAMELIST=/usr/share/dict/propernames
#dict word count
WC=`cat $WORDLIST | wc -l`
WC2=`cat $NAMELIST | wc -l`


X=${1:-100}
tcount=${2:-0}
#set range to guarantee some tag overlap for nodes
RANGE=$[ ($X * $tcount ) / 2 ]
OFFSET=$[ $RANDOM * ( $WC / 32000 ) ]

gentags(){
    tags=
    for ((i=0; i<${1:-0}; i++)) ; do
        number=$[ ( $RANDOM % $RANGE )  + 1 + $OFFSET ]
        word=`cat $WORDLIST | head -"$number" | tail -1`
        tags="$word,$tags"
    done
    echo $tags
}
genuser(){
        number=$[ ( $RANDOM % $WC2 )  + 1 ]
        cat $NAMELIST | head -"$number" | tail -1
}


for ((i=0; i<$X; i++)) ; do
    name=`genrand 20`
    tags=`gentags ${2:0}`
    user=`genuser`
    cat <<END
    <node name="node$i-$name" type="Node" description="node$i-$name node" hostname="localhost" osArch="x86_64" osFamily="unix" osName="Mac OS X" osVersion="10.6.5" username="$user" editUrl="" remoteUrl="" tags="$tags"/>
END
done

echo '</project>'
