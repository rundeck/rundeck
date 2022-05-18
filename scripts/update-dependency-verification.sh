#!/usr/bin/env bash

set -euo pipefail

#/ Write Dependency Verification
#/ Update dependency verification metadata and export any new keys.

main() {
  ./gradlew --write-verification-metadata sha256 help
  ./gradlew --write-verification-metadata pgp,sha256 --refresh-keys --export-keys help
  rm gradle/verification-keyring.gpg
  git add gradle/verification-metadata.xml
  git add gradle/verification-keyring.keys

  echo "================================"
  echo "Update complete."
  echo "Next: Commit changes to gradle/verification-metadata.xml and gradle/verification-keyring.keys"
  echo "================================"
}
main
