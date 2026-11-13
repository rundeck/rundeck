#!/bin/bash
# File-level checks for assert-revoked-ca-roots.sh. No Docker image required.
# Run: bash scripts/circleci/test/revoked-ca-roots-test.sh
set -u

HERE="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
# shellcheck source=../assert-revoked-ca-roots.sh
source "${HERE}/../assert-revoked-ca-roots.sh"

if ! command -v openssl >/dev/null || ! command -v keytool >/dev/null; then
    echo "FAIL: openssl and keytool are required" >&2
    exit 1
fi

WORK=$(mktemp -d)
trap 'rm -rf "${WORK}"' EXIT

openssl req -x509 -newkey rsa:2048 -keyout "${WORK}/key.pem" -out "${WORK}/revoked.pem" \
    -days 1 -nodes -subj "/CN=revoked-test" >/dev/null 2>&1
openssl req -x509 -newkey rsa:2048 -keyout "${WORK}/key2.pem" -out "${WORK}/clean.pem" \
    -days 1 -nodes -subj "/CN=clean-test" >/dev/null 2>&1

revoked_fp=$(openssl x509 -in "${WORK}/revoked.pem" -noout -fingerprint -sha256 | sed 's/^.*=//' | tr -d ':' | tr 'A-F' 'a-f')
clean_fp=$(openssl x509 -in "${WORK}/clean.pem" -noout -fingerprint -sha256 | sed 's/^.*=//' | tr -d ':' | tr 'A-F' 'a-f')
other_fp="0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef"

# Colon form, comments, and a blank line — same shape as revoked-ca-roots.txt.
colon_fp=$(echo "$revoked_fp" | sed 's/../&:/g; s/:$//' | tr 'a-f' 'A-F')
cat > "${WORK}/list-revoked.txt" <<EOF
# comment
${colon_fp}

EOF
printf '%s\n' "$other_fp" > "${WORK}/list-clean.txt"
printf 'not-a-fingerprint\n' > "${WORK}/list-bad.txt"

import_keystore() {
    local cert="$1" dest="$2"
    rm -f "$dest"
    keytool -importcert -noprompt -alias test -file "$cert" -keystore "$dest" \
        -storetype JKS -storepass changeit >/dev/null 2>&1
}

import_keystore "${WORK}/clean.pem" "${WORK}/clean.jks"
import_keystore "${WORK}/revoked.pem" "${WORK}/revoked.jks"

failures=0
run_case() {
    local name="$1" bundle="$2" keystore="$3" list="$4" want="$5"
    local out rc
    out=$(revoked_ca_assert_stores "$bundle" "$keystore" "$list" "test" 2>&1)
    rc=$?
    if [[ "$rc" -ne "$want" ]]; then
        echo "FAIL ${name}: exit=${rc} (want ${want})"
        echo "$out"
        failures=$((failures + 1))
    else
        echo "ok   ${name}"
    fi
}

run_case "clean bundle and keystore pass" "${WORK}/clean.pem" "${WORK}/clean.jks" "${WORK}/list-clean.txt" 0
run_case "revoked root in OS bundle fails" "${WORK}/revoked.pem" "${WORK}/clean.jks" "${WORK}/list-revoked.txt" 1
run_case "revoked root in JVM keystore fails" "${WORK}/clean.pem" "${WORK}/revoked.jks" "${WORK}/list-revoked.txt" 1
run_case "missing bundle fails" "${WORK}/missing.pem" "${WORK}/clean.jks" "${WORK}/list-clean.txt" 1
run_case "missing keystore fails" "${WORK}/clean.pem" "${WORK}/missing.jks" "${WORK}/list-clean.txt" 1
run_case "malformed list fails" "${WORK}/clean.pem" "${WORK}/clean.jks" "${WORK}/list-bad.txt" 1

shipped="${HERE}/../revoked-ca-roots.txt"
if revoked_ca_load_list "$shipped" \
    && printf '%s\n' "${REVOKED_CA_FPS[@]}" | grep -qx '86a1ecba089c4a8d3bbe2734c612ba341d813e043cf9e8a862cd5c57a36bbe6b'; then
    echo "ok   shipped list contains emSign ECC Root CA - G3"
else
    echo "FAIL shipped list does not contain emSign ECC Root CA - G3"
    failures=$((failures + 1))
fi

# A clean store must not match the shipped denylist just because the list loads.
run_case "shipped list does not flag an unrelated cert" "${WORK}/clean.pem" "${WORK}/clean.jks" "$shipped" 0

exit "$failures"
