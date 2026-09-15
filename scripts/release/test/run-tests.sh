#!/bin/bash
#
# run-tests.sh - functional tests for release-rc.sh, release-tag.sh,
# release-version.sh, and this repo's own setversion.sh.
#
# Builds a real throwaway git repo (+ a local bare "origin") and a stub `gh` in a
# mktemp workspace, simulates merged PRs covering every merge strategy
# release-rc.sh has to distinguish (squash, merge-commit, rebase-merge) plus a
# genuine cherry-pick conflict, and drives the real scripts against them - no
# mocking of git itself, only of the GitHub API calls (`gh`).
#
# Requires bash 4+ (associative arrays) - on macOS, /bin/bash is 3.2; run with
# an updated bash (e.g. Homebrew's), not the system default.
#
# Usage: scripts/release/test/run-tests.sh

set -uo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
RELEASE_DIR="$(cd "$SCRIPT_DIR/.." && pwd)"
WORK="$(mktemp -d)"
trap 'rm -rf "$WORK"' EXIT

PASS=0
FAIL=0

function ok   { PASS=$((PASS + 1)); echo "  ok - $1"; }
function fail { FAIL=$((FAIL + 1)); echo "  FAIL - $1"; }

function assert_exit {
    local desc="$1" expected="$2" actual="$3"
    if [ "$expected" = "$actual" ]; then ok "$desc (exit $actual)"; else fail "$desc (expected exit $expected, got $actual)"; fi
}

function assert_contains {
    local desc="$1" haystack="$2" needle="$3"
    if [[ "$haystack" == *"$needle"* ]]; then ok "$desc"; else fail "$desc (output did not contain: $needle)"; fi
}

function assert_not_contains {
    local desc="$1" haystack="$2" needle="$3"
    if [[ "$haystack" != *"$needle"* ]]; then ok "$desc"; else fail "$desc (output unexpectedly contained: $needle)"; fi
}

# --- fixture: repo, bare origin, stub gh, four simulated PRs ---

REPO="$WORK/repo"
ORIGIN="$WORK/origin.git"
STUB_DATA="$WORK/gh-stub-data"
mkdir -p "$STUB_DATA"

git init --bare -q "$ORIGIN"
git init -q "$REPO"
cd "$REPO"
git config user.email test@test.com
git config user.name Test
git remote add origin "$ORIGIN"
git checkout -q -b main

echo "base" > file1.txt
echo "base2" > file2.txt
git add -A && git commit -q -m initial
git tag v1.0.0-rc1
git push -q origin main
git push -q origin v1.0.0-rc1

# PR A: squash merge, single commit, modifies file1.txt line 1
git checkout -q --detach v1.0.0-rc1
sed -i '' 's/base/line1-changed-by-A/' file1.txt
git commit -q -am "PR A (squash)"
PRA_SHA="$(git rev-parse HEAD)"
echo 1 > "$STUB_DATA/pr_101_commits"

# PR B: merge-commit strategy (2 parents), untouched file1
git checkout -q --detach v1.0.0-rc1
git checkout -q -b prb-feature
echo "feature-b" >> file2.txt
git commit -q -am "PR B feature commit"
git checkout -q --detach v1.0.0-rc1
git merge -q --no-ff prb-feature -m "Merge PR B"
PRB_SHA="$(git rev-parse HEAD)"
git branch -D prb-feature -q

# PR C: rebase-merge, 2 original commits - tip alone doesn't match the full diff
git checkout -q --detach v1.0.0-rc1
git checkout -q -b prc-original
echo "c1" > file3.txt
git add file3.txt && git commit -q -m "PR C commit 1"
echo "c2" >> file3.txt
git commit -q -am "PR C commit 2"
PRC_HEAD_ORIGINAL="$(git rev-parse HEAD)"
git checkout -q --detach v1.0.0-rc1
git cherry-pick $(git rev-list --reverse "v1.0.0-rc1..prc-original") >/dev/null
PRC_SHA="$(git rev-parse HEAD)"
git branch -D prc-original -q
echo 2 > "$STUB_DATA/pr_103_commits"
echo "v1.0.0-rc1" > "$STUB_DATA/pr_103_diffbase"
echo "$PRC_HEAD_ORIGINAL" > "$STUB_DATA/pr_103_diffhead"

# PR D: conflicts with PR A (same line, different edit)
git checkout -q --detach v1.0.0-rc1
sed -i '' 's/base/line1-changed-by-D/' file1.txt
git commit -q -am "PR D (conflicts with A)"
PRD_SHA="$(git rev-parse HEAD)"
echo 1 > "$STUB_DATA/pr_104_commits"

git checkout -q main

cat > "$WORK/gh" <<STUBEOF
#!/bin/bash
DATA="$STUB_DATA"
REPO_DIR="$REPO"
if [ "\$1" = "label" ] && [ "\$2" = "create" ]; then exit 0; fi
if [ "\$1" = "pr" ] && [ "\$2" = "list" ]; then
    if [[ "\$*" == *"--json number "*"--jq"* ]]; then cat "\$DATA/pr_numbers.json"; else cat "\$DATA/pr_list.json"; fi
    exit 0
fi
if [ "\$1" = "pr" ] && [ "\$2" = "diff" ]; then
    git -C "\$REPO_DIR" diff "\$(cat "\$DATA/pr_\${3}_diffbase")" "\$(cat "\$DATA/pr_\${3}_diffhead")"
    exit 0
fi
if [ "\$1" = "api" ]; then
    cat "\$DATA/pr_\$(echo "\$2" | grep -oE '[0-9]+\$')_commits"
    exit 0
fi
if [ "\$1" = "pr" ] && [ "\$2" = "edit" ]; then
    echo "LABELED PR #\$3" >> "\$DATA/labeled.log"
    exit 0
fi
echo "STUB GH: unhandled command: \$*" >&2
exit 1
STUBEOF
chmod +x "$WORK/gh"
export PATH="$WORK:$PATH"

function pr_list_json {
    # pr_list_json <number:sha:mergedAt:label,label,...> ...
    local entries=() e num sha at labels
    for e in "$@"; do
        IFS=: read -r num sha at labels <<< "$e"
        local label_json="[]"
        if [ -n "$labels" ]; then
            label_json="$(jq -nc --arg l "$labels" '$l | split(",") | map({name: .})')"
        fi
        entries+=("$(jq -nc --argjson n "$num" --arg s "$sha" --arg a "$at" --argjson labels "$label_json" \
            '{number:$n, title:"PR", mergedAt:$a, mergeCommit:{oid:$s}, labels:$labels}')")
    done
    printf '%s\n' "${entries[@]}" | jq -sc .
}

function write_pr_data {
    pr_list_json "$@" > "$STUB_DATA/pr_list.json"
    jq -r '[.[].number] | sort | @tsv' "$STUB_DATA/pr_list.json" > "$STUB_DATA/pr_numbers.json"
}

RC_SCRIPT="$RELEASE_DIR/release-rc.sh"
RUN_RC="/opt/homebrew/bin/bash"
if ! command -v "$RUN_RC" >/dev/null 2>&1; then RUN_RC="bash"; fi

echo "=== release-rc.sh: happy path (squash + merge-commit + rebase-merge) ==="
cd "$REPO"
write_pr_data "101:$PRA_SHA:2024-01-01T00:00:00Z:rc-backport-1.0.0" \
              "102:$PRB_SHA:2024-01-02T00:00:00Z:rc-backport-1.0.0" \
              "103:$PRC_SHA:2024-01-03T00:00:00Z:rc-backport-1.0.0"
OUT="$("$RUN_RC" "$RC_SCRIPT" 1.0.0 rc2 --push 2>&1)"; EXIT=$?
assert_exit "happy path tags successfully" 0 "$EXIT"
assert_contains "happy path detects rebase-merge range" "$OUT" "Detected rebase-merge of 2 commits"
assert_eq_content() { [ "$(git show v1.0.0-rc2:"$1" 2>/dev/null)" = "$2" ]; }
if assert_eq_content file1.txt "line1-changed-by-A"; then ok "tag contains PR A's change"; else fail "tag missing PR A's change"; fi
if assert_eq_content file3.txt "$(printf 'c1\nc2')"; then ok "tag contains both of PR C's rebase-merge commits"; else fail "tag missing PR C's second commit (range detection failed)"; fi
if git -C "$ORIGIN" tag -l | grep -qx v1.0.0-rc2; then ok "tag pushed to origin"; else fail "tag not found on origin"; fi
LABELED="$(cat "$STUB_DATA/labeled.log" 2>/dev/null || true)"
for n in 101 102 103; do
    if grep -q "PR #$n" <<< "$LABELED"; then ok "PR #$n labeled applied"; else fail "PR #$n not labeled applied"; fi
done

echo "=== release-rc.sh: conflict -> rescue branch, then resume ==="
git checkout -q --detach v1.0.0-rc1
write_pr_data "101:$PRA_SHA:2024-01-01T00:00:00Z:rc-backport-1.0.0" \
              "104:$PRD_SHA:2024-01-04T00:00:00Z:rc-backport-1.0.0"
rm -f "$STUB_DATA/labeled.log"
OUT="$("$RUN_RC" "$RC_SCRIPT" 1.0.0 rc3 --push 2>&1)"; EXIT=$?
assert_exit "conflict refuses to tag" 7 "$EXIT"
assert_contains "conflict names the right PR" "$OUT" "PR #104"
if git rev-parse --verify refs/heads/rescue/v1.0.0-rc3 >/dev/null 2>&1; then ok "rescue branch created locally"; else fail "rescue branch not created locally"; fi
if git -C "$ORIGIN" rev-parse --verify refs/heads/rescue/v1.0.0-rc3 >/dev/null 2>&1; then ok "rescue branch pushed to origin"; else fail "rescue branch not pushed"; fi

git checkout -q rescue/v1.0.0-rc3
git cherry-pick -x "$PRD_SHA" >/dev/null 2>&1
echo "line1-changed-by-A-and-D" > file1.txt
git add file1.txt
git cherry-pick --continue --no-edit >/dev/null
git push -q origin rescue/v1.0.0-rc3
write_pr_data "101:$PRA_SHA:2024-01-01T00:00:00Z:rc-backport-1.0.0" \
              "104:$PRD_SHA:2024-01-04T00:00:00Z:rc-backport-1.0.0,rc-backport-1.0.0-applied"
git checkout -q --detach main
OUT="$("$RUN_RC" "$RC_SCRIPT" 1.0.0 rc3 --push 2>&1)"; EXIT=$?
assert_exit "resume tags successfully" 0 "$EXIT"
assert_contains "resume finds the rescue branch" "$OUT" "resuming from it"
if git rev-parse --verify refs/heads/rescue/v1.0.0-rc3 >/dev/null 2>&1; then fail "local rescue branch not cleaned up"; else ok "local rescue branch cleaned up"; fi
if git -C "$ORIGIN" rev-parse --verify refs/heads/rescue/v1.0.0-rc3 >/dev/null 2>&1; then fail "origin rescue branch not cleaned up"; else ok "origin rescue branch cleaned up"; fi
if [ "$(git show v1.0.0-rc3:file1.txt)" = "line1-changed-by-A-and-D" ]; then ok "resumed tag has the resolved content"; else fail "resumed tag missing resolved content"; fi

echo "=== release-rc.sh: --dry-run makes no durable changes ==="
git checkout -q --detach v1.0.0-rc1
write_pr_data "101:$PRA_SHA:2024-01-01T00:00:00Z:rc-backport-1.0.0"
OUT="$("$RUN_RC" "$RC_SCRIPT" 1.0.0 rc4 --push --dry-run 2>&1)"; EXIT=$?
assert_exit "dry-run reports success" 0 "$EXIT"
if git tag -l | grep -qx v1.0.0-rc4; then fail "dry-run created a real tag"; else ok "dry-run created no tag"; fi

echo "=== release-rc.sh: usage/validation errors ==="
assert_exit "missing args -> usage" 2 "$("$RUN_RC" "$RC_SCRIPT" 1.0.0 >/dev/null 2>&1; echo $?)"
assert_exit "rc1 rejected" 3 "$("$RUN_RC" "$RC_SCRIPT" 1.0.0 rc1 >/dev/null 2>&1; echo $?)"
echo "[]" > "$STUB_DATA/pr_list.json"; echo "" > "$STUB_DATA/pr_numbers.json"
git checkout -q --detach v1.0.0-rc1
assert_exit "no labeled PRs -> refuses" 8 "$("$RUN_RC" "$RC_SCRIPT" 1.0.0 rc4 >/dev/null 2>&1; echo $?)"

echo "=== setversion.sh (this repo: version.properties, no RBA) ==="
SV_WORK="$WORK/sv"
mkdir -p "$SV_WORK/scripts/release"
cp "$RELEASE_DIR"/*.sh "$SV_WORK/scripts/release/"
cd "$SV_WORK"
git init -q . && git config user.email t@t.com && git config user.name T
cat > version.properties <<EOF
version.number=1.0.0
version.tag=SNAPSHOT
version.date=20260101
version.version=1.0.0-SNAPSHOT-20260101
EOF
git add -A && git commit -qm init
OUT="$("$RUN_RC" scripts/release/setversion.sh --tag 1.0.0 rc2 abc1234 2>&1)"; EXIT=$?
assert_exit "rc2+ rejected by setversion.sh" 13 "$EXIT"
OUT="$("$RUN_RC" scripts/release/setversion.sh --tag 1.0.0 RBA abcdef1 2345678 2>&1)"; EXIT=$?
assert_contains "RBA is not a recognized tag type here" "$OUT" "Invalid tag format 'RBA'"
OUT="$("$RUN_RC" scripts/release/setversion.sh --tag 1.0.1 rc1 "$(git rev-parse HEAD)" --dry-run 2>&1)"; EXIT=$?
assert_exit "rc1 with explicit commit succeeds (dry-run)" 0 "$EXIT"

echo ""
echo "=== $PASS passed, $FAIL failed ==="
[ "$FAIL" -eq 0 ]
