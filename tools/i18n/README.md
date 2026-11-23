# Translation runtime helpers

This directory contains code shared by translation consumers. Canonical
catalog data belongs under `translations/`; executable helpers do not.

- `mergeCatalogs.ts` performs immutable, key-level Vue catalog overlays.

The direct catalog-management CLI remains under `scripts/i18n` and
`scripts/i18n-lib`.
