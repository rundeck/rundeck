#!/usr/bin/env bash

set -euo pipefail
set -x

DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" >/dev/null 2>&1 && pwd )"
SRCDIR="$DIR/.."
buildDir="$SRCDIR/build"
buildPackDir="$buildDir/pack"

#/ Create the npm package
create_npm_package(){
  cd "$buildPackDir"
  nvm use
  which node || ( echo "node not found $PATH" && exit 2 )
  npm pack
  cp -v *.tgz "$1/rundeck-ui-trellis.tgz"
}

#/ Copy the source files and dirs into the pack dir
npm_stage_package(){
  if [ -d "$buildPackDir" ]; then
    rm -rf "$buildPackDir"
  fi
  mkdir -p "$buildPackDir"

  sourceDirs=(
    'lib'
    'src'
  )
  sourceFiles=(
    'package.json'
    'README.md'
  )
  #  copy source files and dirs into the pack dir:
  cd "$SRCDIR"
  for dir in "${sourceDirs[@]}"; do
    cp -rv "$dir" "$buildPackDir"
  done

  for file in "${sourceFiles[@]}"; do
    cp -v "$file" "$buildPackDir"
  done
}

#/ call the function based on the first argument
main(){
  #require at least one argument
    if [ $# -lt 1 ]; then
      echo "Usage: $0 <output-dir>"
      exit 1
    fi
    npm_stage_package
    create_npm_package "$@"
}

main "$@"
