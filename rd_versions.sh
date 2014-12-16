#!/bin/bash
#/ utility functions for versioning

rd_get_version(){
    local CUR_VERSION=$(grep version.number= `pwd`/version.properties | cut -d= -f 2)
    local CUR_RELEASE=$(grep version.release.number= `pwd`/version.properties | cut -d= -f 2)
    local CUR_TAG=$(grep version.tag= `pwd`/version.properties | cut -d= -f 2)

    echo "${CUR_VERSION}" 
    echo "${CUR_RELEASE}"
    echo "${CUR_TAG}"
}
rd_make_release_version(){
   local VERS=("$@")

    let relvers=$(( 1 + ${VERS[1]} ))

    echo "${VERS[0]}"
    echo "$relvers"
    echo "GA"
}
rd_make_next_snapshot(){
    local VERS=("$@")

    local -a PARTS=()
    IFS='.' read -ra PARTS <<< "${VERS[0]}"
    PARTS[2]=$(( ${PARTS[2]} + 1 ))

    local val=$( O=$IFS ; IFS='.' ; local X="${PARTS[*]}" ; OFS=$O ; echo "$X" )
    echo "$val"
    echo "0"
    echo "SNAPSHOT"
}
rd_set_version(){
   local FARGS=("$@")

    echo "Setting version to ${FARGS[0]}-${FARGS[1]}-${FARGS[2]}"

    local result=$( bash setversion.sh "${FARGS[@]}" 2>&1 )
}



