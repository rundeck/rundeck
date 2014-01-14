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

<project>
END

WORDLIST=/usr/share/dict/words
WORDLIST2=/usr/share/dict/web2a
NAMELIST=/usr/share/dict/propernames
CONNECTIVES=/usr/share/dict/connectives
#dict word count
WC=`cat $WORDLIST | wc -l`
WLC2=`cat $WORDLIST2 | wc -l`
CLC=`cat $CONNECTIVES | wc -l`
WC2=`cat $NAMELIST | wc -l`
declare -a DICTS=($WORDLIST $WORDLIST2 $CONNECTIVES)
declare -a COUNTS=($WC $WLC2 $CLC)


X=${1:-100}
tcount=${2:-0}
basename=${3:-node}
suffix=${4:-}
#set range to guarantee some tag overlap for nodes
RANGE=$[ ($X * $tcount ) / 2 ]
OFFSET=$[ $RANDOM * ( $WC / 32000 ) ]

randword(){
    pick=$[ ( $RANDOM % 3 )]
    dict=${DICTS[$pick]}
    count=${COUNTS[$pick]}
    number=$[ ( $RANDOM % $count )  + 1 ]
    word=`cat $dict | head -"$number" | tail -1`
    echo $word
}
gensentence(){
    words=
    for ((i=0; i<${1:-0}; i++)) ; do
        word=`randword`
        words="$words $word"
    done
    echo $words
}
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

key=`randword`
for ((i=0; i<$X; i++)) ; do
    name=`genrand 20`
    tags=`gentags ${tcount}`
    user=`genuser`
    other=`gensentence ${tcount}`
    desc=`gensentence ${5:-0}`
    
    if [ $[ ( $i % 10 ) ]  == 9 ] ; then
        key=`randword`
    fi
    value=`randword`
    cat <<END
    <node name="$basename$i$suffix" description="$desc" hostname="localhost" osArch="x86_64" osFamily="unix" osName="Mac OS X" osVersion="10.6.5" username="$user" editUrl="" remoteUrl="" tags="$tags" node-executor="local" file-copier="local" bleh="$other">
        <attribute name="$key" value="$value"/>
    </node>
END
done

echo '</project>'
