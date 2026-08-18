# Design: Migrate Webhook Auth Tokens off LEGACY to SECURED

**Ticket**: PS-1686 (Security Report) · P2 · Due 2026-09-17

## Problem

`ApiService.groovy:114` hardcodes `AuthTokenMode.LEGACY` for `AuthTokenType.WEBHOOK`, so webhook auth
tokens are stored in plaintext in the `auth_token.token` column. Every other token type uses
`AuthTokenMode.SECURED` (SHA-256 hash at rest). A DB dump, backup exposure, or insider access yields
webhook tokens directly usable to trigger job executions.

## Root Cause (from investigation)

There are two independent lookup paths involved in webhook auth:

1. **Webhook definition lookup** (`Webhook.findByAuthToken`, `webhook.auth_token` column) — a raw,
   unhashed string match against the webhook's own table. This has nothing to do with `AuthTokenMode`
   and is unaffected by anything below.
2. **Session/subject authentication** (`ApiService.tokenLookupWithType` / `GormTokenDataProvider`) —
   queries the `auth_token` table. This lookup **already** does a hybrid match: SHA-256-hash the
   incoming value and compare against SECURED rows, OR compare raw against LEGACY/null-mode rows. It
   was already built to support a mixed-mode dataset.

The reason WEBHOOK is forced to LEGACY is not the query layer — it's that
`WebhookService.saveHook` (`grails-webhooks/.../WebhookService.groovy:260`) publishes
`at.token` (the *persisted* value — hash, once SECURED) as the public webhook URL secret, instead of
`at.clearToken` (the transient plaintext, only available at creation time). Flipping the mode without
fixing this line would publish a hash as the "secret" and break authentication (hash-of-hash never
matches the stored hash).

The correct pattern already exists for USER tokens in
`rundeckapp/src/main/groovy/com/dtolabs/rundeck/app/api/tokens/Token.groovy`, which selects
`clearToken` vs `token` based on `tokenMode`.

## Changes

### 1. Fix the publish path (root cause)
`WebhookService.saveHook`: change `saveWebhookRequest.setAuthToken(at.token)` to
`saveWebhookRequest.setAuthToken(at.clearToken)`. This decouples the public webhook secret
(`webhook.auth_token`, plaintext, its own column/table) from the hashed-at-rest `auth_token.token`
column. The webhook-definition lookup (path 1 above) is untouched — still raw-to-raw match — so
existing webhook URLs keep working regardless of `AuthTokenMode`.

### 2. Flip the default token mode
Remove the `tokenType == AuthTokenType.WEBHOOK ? AuthTokenMode.LEGACY : AuthTokenMode.SECURED`
special-case in both:
- `ApiService.groovy:114` (`generateAuthToken`)
- `GormTokenDataProvider.createWithId` (duplicated branch)

Both resolve unconditionally to `AuthTokenMode.SECURED`. No config flag — matches how every other
token type already behaves, and no precedent exists in this codebase for gating token-mode behavior.

### 3. Fix the silently-breaking raw finder
`RundeckAuthTokenManagerService.importWebhookToken` and `.updateAuthRoles` call
`tokenDataProvider.findByTokenAndType(token, AuthTokenType.WEBHOOK)`, a plain raw-value GORM finder
(`AuthToken.findByTokenAndType`). Once SECURED webhook rows exist, incoming raw tokens will never
match a stored hash via this finder. Update it to the same hybrid match already implemented in
`GormTokenDataProvider.tokenLookupWithType`: hash-and-match-SECURED OR raw-match-LEGACY/null.

### 4. Data migration for existing rows
New startup-run Groovy component (plain Spring-managed bean / BootStrap-time hook — no Liquibase
`customChange` precedent exists in this repo, so this stays a code-based migration rather than a new
migration-file pattern):

- Scans `AuthToken` rows where `type = WEBHOOK` and `tokenMode` is `LEGACY` or `null`.
- For each row: re-hash `token` via the existing `AuthenticationTokenUtils.encodeTokenValue(value,
  AuthTokenMode.SECURED)`, set `tokenMode = SECURED`, save — one row per transaction (the `token`
  column has a unique constraint; per-row transactions avoid transient duplicate-value windows a bulk
  UPDATE could hit).
- Naturally idempotent: converted rows no longer match the LEGACY/null filter on a later boot, so
  running this on every startup is safe and cheap once the one-time backlog is cleared.
- `webhook.auth_token` (the URL secret) is never touched by this migration — existing webhook URLs
  keep working with zero user action.

## Testing

- Spock unit test: `saveHook` persists the clear value to `webhook.auth_token` and the hash to
  `AuthToken.token`.
- Spock unit test: migration component converts LEGACY → SECURED rows, and a subsequent
  `tokenLookupWithType` call with the original clear-text token still authenticates successfully.
- Spock unit test: `findByTokenAndType`-based paths (`importWebhookToken`, `updateAuthRoles`) work
  against both SECURED and LEGACY rows after the hybrid-match fix.
- Existing webhook functional/API test suites must remain green (webhook creation, webhook POST
  triggering, project import/export of webhooks).

## Out of Scope

- Moving `SECURED` off plain SHA-256 to a slow KDF (ticket remediation item 3) — separate, larger
  change with broader implications (existing SECURED rows for all token types), not bundled here.
- Any UI/API surface change to let admins manually trigger re-encoding — the startup migration covers
  all existing rows automatically.
