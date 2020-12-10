#!/bin/bash

./node_modules/.bin/vue-cli-service --mode production build

if [ -d "lib/src" ]; then
    echo "Fixing up .d.ts location..."
    (
        cd ./lib/src || exit
        cp -ar ./* ../
    )
    rm -r ./lib/src
fi