#!/bin/bash
# Checks apt_get_retry from dependencies-functions.sh using stubbed sudo/apt-get (no real apt needed).
# Run: bash scripts/circleci/test/apt-get-retry-test.sh   (or inside an ubuntu:22.04 container)
set -u

HERE="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
STUBS="$(mktemp -d)"
trap 'rm -rf "${STUBS}"' EXIT

# sudo just runs the command; apt-get fails the first $APT_STUB_FAILURES "install" calls.
printf '#!/bin/bash\nexec "$@"\n' > "${STUBS}/sudo"
cat > "${STUBS}/apt-get" <<'STUB'
#!/bin/bash
[[ "$1" == "update" ]] && exit 0
n=$(cat "${APT_STUB_COUNT}" 2>/dev/null || echo 0)
echo $((n + 1)) > "${APT_STUB_COUNT}"
[[ "${n}" -lt "${APT_STUB_FAILURES}" ]] && exit 100
exit 0
STUB
chmod +x "${STUBS}/sudo" "${STUBS}/apt-get"

failures=0
# run_case <name> <stub failures> <expected exit> <expected install calls>
run_case() {
    local name=$1 stub_failures=$2 want_exit=$3 want_calls=$4
    export APT_STUB_COUNT="${STUBS}/count" APT_STUB_FAILURES="${stub_failures}" APT_RETRY_DELAY=0
    rm -f "${APT_STUB_COUNT}"
    local out rc calls
    # set -e inside: after the third failure the script must stop before printing "not reached"
    out=$(PATH="${STUBS}:${PATH}" bash -c "set -e; source '${HERE}/../dependencies-functions.sh'; apt_get_retry jq; echo not-reached" 2>&1)
    rc=$?
    calls=$(cat "${APT_STUB_COUNT}" 2>/dev/null || echo 0)
    if [[ "${rc}" -ne "${want_exit}" || "${calls}" -ne "${want_calls}" ]]; then
        echo "FAIL ${name}: exit=${rc} (want ${want_exit}) installs=${calls} (want ${want_calls})"; failures=$((failures + 1))
    elif [[ "${want_exit}" -ne 0 && "${out}" == *"not-reached"* ]]; then
        echo "FAIL ${name}: script did not stop under set -e"; failures=$((failures + 1))
    else
        echo "ok   ${name}"
    fi
}

run_case "succeeds first time"        0 0 1
run_case "recovers after 1 failure"   1 0 2
run_case "recovers after 2 failures"  2 0 3
run_case "fails after 3 failures"     3 1 3

exit "${failures}"
