#!/bin/bash
# Checks dependencies_assert_emudhra_untrusted with a stubbed keytool.
# Run: bash scripts/circleci/test/emudhra-trust-test.sh
set -u

HERE="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
WORK=$(mktemp -d)
trap 'rm -rf "${WORK}"' EXIT

JDK="${WORK}/jdk"
mkdir -p "${JDK}/bin" "${JDK}/lib/security"
touch "${JDK}/lib/security/cacerts"
# A comment that names the roots must not skip the cacerts check.
cat > "${JDK}/lib/security/blocked.certs" <<'EOF'
# eMudhra
# emSign ECC Root CA - G3
# AAA Certificate Services
EOF
cat > "${JDK}/bin/keytool" <<'STUB'
#!/bin/bash
cat "${KEYTOOL_DUMP}"
STUB
chmod +x "${JDK}/bin/keytool"

failures=0
run_case() {
    local name="$1" dump="$2" want="$3"
    local out rc
    out=$(KEYTOOL_DUMP="${dump}" bash -c "set -ueo pipefail; source '${HERE}/../dependencies-functions.sh'; dependencies_assert_emudhra_untrusted '${JDK}'" 2>&1)
    rc=$?
    if [[ "${rc}" -ne "${want}" ]]; then
        echo "FAIL ${name}: exit=${rc} (want ${want})"
        echo "${out}"
        failures=$((failures + 1))
    else
        echo "ok   ${name}"
    fi
}

printf 'Owner: CN=DigiCert Global Root G2, O=DigiCert Inc, C=US\n' > "${WORK}/clean.txt"
{
    printf 'Owner: CN=emSign ECC Root CA - G3, O=eMudhra Technologies Limited, C=IN\n'
    head -c 300000 /dev/zero | tr '\0' 'x'
    printf '\n'
} > "${WORK}/emsign.txt"
printf 'Owner: CN=AAA Certificate Services, O=Comodo CA Limited, C=GB\n' > "${WORK}/aaa.txt"
printf 'Issuer: CN=emSign ECC Root CA - G3, O=eMudhra Technologies Limited, C=IN\nOwner: CN=Some Intermediate, O=Example, C=US\n' > "${WORK}/issuer.txt"

run_case "clean cacerts passes despite blocklist comment" "${WORK}/clean.txt" 0
run_case "emSign owner is refused when it leads a large dump" "${WORK}/emsign.txt" 1
run_case "AAA owner is refused" "${WORK}/aaa.txt" 1
run_case "issuer mention is not a trusted root" "${WORK}/issuer.txt" 0

rm -f "${JDK}/lib/security/cacerts"
run_case "missing cacerts fails" "${WORK}/clean.txt" 1

exit "${failures}"
