#!/bin/bash

TEMPLATE='job.definition'
COUNT=100

export LC_ALL=C

random_line() {
  numlines=`wc -l $1| awk {'print $1'}`
  t=`date +%s`
  t=`expr $t + $RANDOM`
  a=`expr $t % $numlines + 1`
  RETURN=`head -n $a $1|tail -n 1`
  return 0
}

genrand() {
    cat /dev/random | tr -dc ${2:-' A-Za-z1-9'} | head -c${1:-1024}
}

gengroup() {
    for y in {0..7}; do
        genrand 6 'A-Za-z1-9'
        printf "/"
    done
    genrand 6 'A-Za-z1-9'
}

seloptions() {
    RAND=`od -d -N2 -An /dev/urandom`
    echo $RAND
}

echo '<?xml version="1.0" encoding="UTF-8"?><joblist>'

for x in {0..0} ; do
    name=`genrand 512`
    desc=`genrand 1024`
    addl=`genrand 4048`
    proj=test
    group=`gengroup`

    for y in {1..10}; do
        random_line jobs.definitions
        commands+=$RETURN 
    done
    for z in {1..10}; do
        random_line options.definitions
        options+=$RETURN
    done
    while read LINE; do
      echo $LINE |
      sed "s/@NAME@/${name}/g;s/@DESCRIPTION@/${desc}/g;s/@ADDITIONAL@/${addl}/g;s/@PROJECT@/$proj/g;s#@GROUP@#${group}#g;s#@OPTIONS@#$options#g;s#@COMMANDS@#$commands#g" 
    done < $TEMPLATE
done

echo '</joblist>'