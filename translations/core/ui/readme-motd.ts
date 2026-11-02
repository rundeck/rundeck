/**
 * Sparse public overlay for the core Readme/MOTD Vue catalog.
 *
 * The existing component-local catalog remains authoritative for keys omitted
 * here until a broader migration is designed.
 */
export default {
  en_US: {
    "translation.poc.core.precedence": "public",
    "translation.poc.core.canonical-only": "canonical-only-value",
    "translation.poc.core.flat.dotted": "canonical-flat-value",
    edit: {
      readme: {
        label: "Canonical Edit Readme",
      },
    },
  },
  de_DE: {
    edit: {
      readme: {
        label: "Kanonische Readme bearbeiten",
      },
    },
  },
} as Record<string, Record<string, unknown>>;
