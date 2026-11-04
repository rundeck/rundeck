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
      files: ["src/app/**", "src/library/**"],
      rules: {
        "@typescript-eslint/no-explicit-any": "error",
      },
    },
    {
      files: [
        "**/*.spec.ts",
        "**/*.spec.js",
        "**/tests/**",
        "**/__tests__/**",
      ],
      rules: {
        "@typescript-eslint/no-explicit-any": "off",
        complexity: "off",
      },
    },
  ],
};
