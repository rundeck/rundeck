/**
 * Sparse public overlay for the core Readme/MOTD Vue catalog.
 *
 * The existing component-local catalog remains authoritative for keys omitted
 * here until a broader migration is designed.
 */
export default {
  en_US: {
    "translation.poc.core.precedence": "public",
    edit: {
      readme: {
        label: "Edit Readme",
      },
    },
  },
} as Record<string, Record<string, unknown>>;
