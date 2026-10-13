/**
 * ESLint config for ui-trellis.
 *
 * The shared rule set lives in `./eslint/base.js`, which rundeckpro also
 * consumes through the `@rundeck/ui-trellis` package. Only settings specific
 * to this package belong here.
 */
module.exports = {
  extends: ["./eslint/base.js"],
  ignorePatterns: ["build", "test", ".storybook"],
  overrides: [
    {
      // ui-trellis keeps test helpers and mocks under tests/ directories
      files: ["**/tests/**"],
      rules: {
        complexity: "off",
      },
    },
  ],
};
