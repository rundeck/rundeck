#!/usr/bin/env bash

set -euo pipefail

#/ Write Dependency Verification
#/ Update dependency verification metadata and export any new keys.
#/ usage: sh update-dependency-verification.sh [-r]
#/
#/ use -r to refresh missing keys
#/
REFRESHSHA=
REFRESHPGP=

usage() {
  grep '^#/' <"$0" | cut -c4- # prints the #/ lines above as usage info
}
die() {
  echo >&2 "$@"
  exit 2
}

update_sha() {
  ./gradlew --write-verification-metadata sha256 ${REFRESHSHA:-} help
}
update_pgp() {
  ./gradlew --write-verification-metadata pgp,sha256 ${REFRESHPGP:-} help
}
git_add() {
  rm gradle/verification-keyring.gpg
  git add gradle/verification-metadata.xml
  git add gradle/verification-keyring.keys
}
notify() {
  echo "================================"
  echo "Update complete."
  echo "Next: Commit changes to gradle/verification-metadata.xml and gradle/verification-keyring.keys"
  echo "================================"
}
check_args(){
  while getopts :re flag; do
    case "${flag}" in
    r)
      REFRESHSHA="--refresh-keys --export-keys --refresh-dependencies"
      REFRESHPGP="--refresh-dependencies"
      ;;
    ?)
      usage
      die "unknown option $OPTARG"
      ;;
    esac
  done
}

main() {
  check_args "$@"
  update_sha
  update_pgp
  git_add
  notify
}

main "$@"
