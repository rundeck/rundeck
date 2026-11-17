/**
 * Sparse public overlay for the core Readme/MOTD Vue catalog.
 *
 * The existing component-local catalog remains authoritative for keys omitted
 * here until a broader migration is designed.
 */
export default {
  en_US: {
    edit: {
      readme: {
        label: "Edit Readme",
      },
    },
  },
  de_DE: {
    edit: {
      readme: {
        label: "Readme bearbeiten",
      },
    },
  },
} as Record<string, Record<string, unknown>>;
