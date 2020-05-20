#!/bin/bash

lerna bootstrap

trap "trap - SIGTERM && kill -- -$$" SIGINT SIGTERM EXIT

(tsc -b packages -W ) &
(cd packages/spa && npm run dev ) &


wait
