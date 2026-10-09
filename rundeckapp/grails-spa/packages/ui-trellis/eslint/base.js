/**
 * Shared ESLint rule set for Rundeck front-end packages.
 *
 * Consumed by ui-trellis (`.eslintrc.js` in this package) and by rundeckpro,
 * which loads it from the published `@rundeck/ui-trellis` package via
 * `require.resolve("@rundeck/ui-trellis/eslint/base.js")`. Change rules here
 * once instead of in each consumer.
 *
 * Only reference plugins and configs that every consumer pins at the same
 * version. Package-specific settings (`ignorePatterns`, extra overrides or
 * severity changes) belong in the consuming `.eslintrc.js`.
 */
module.exports = {
  env: {
    node: true,
  },
  parser: "vue-eslint-parser",
  parserOptions: {
    parser: "@typescript-eslint/parser",
    sourceType: "module",
    ecmaVersion: "latest",
    extraFileExtensions: [".vue"],
    ecmaFeatures: {
      jsx: false,
    },
  },
  globals: {
    defineProps: "readonly",
    defineEmits: "readonly",
    withDefaults: "readonly",
  },
  rules: {
    "@typescript-eslint/ban-ts-comment": [
      "error",
      {
        "ts-expect-error": "allow-with-description",
        "ts-ignore": true,
        "ts-nocheck": true,
        minimumDescriptionLength: 10,
      },
    ],
    "@typescript-eslint/no-explicit-any": "off",
    "@typescript-eslint/no-unused-vars": "error",
    "@typescript-eslint/ban-types": "error",
    "@typescript-eslint/no-var-requires": "error",
    "vue/enforce-style-attribute": ["error", { allow: ["scoped"] }],
    "prettier/prettier": "error",
    // Kept as warnings until their violations are fixed in follow-up work
    "vuejs-accessibility/label-has-for": "warn",
    "vuejs-accessibility/click-events-have-key-events": "warn",
    "vuejs-accessibility/no-static-element-interactions": "warn",
    "vuejs-accessibility/form-control-has-label": "warn",
    "vuejs-accessibility/interactive-supports-focus": "warn",
    "vuejs-accessibility/anchor-has-content": "error",
    "vuejs-accessibility/no-autofocus": "error",
    "vuejs-accessibility/alt-text": "error",
    "vuejs-accessibility/iframe-has-title": "error",
    "vue/no-mutating-props": "error",
    "vue/require-v-for-key": "error",
    "vue/no-unused-components": "error",
    "vue/multi-word-component-names": "error",
    "vue/return-in-computed-property": "error",
    "vue/require-slots-as-functions": "error",
    "vue/valid-v-bind": "error",
    "vue/require-toggle-inside-transition": "error",
    "vue/no-reserved-component-names": "error",
    "vue/require-valid-default-prop": "error",
    "vue/no-use-v-if-with-v-for": "error",
    "vue/no-unused-vars": "error",
    "prefer-const": "error",
    "storybook/story-exports": "warn",
    "storybook/context-in-play-function": "warn",
    complexity: ["warn", 25],
  },
  overrides: [
    {
      files: ["**/*.spec.ts", "**/*.spec.js", "**/__tests__/**"],
      rules: {
        complexity: "off",
      },
    },
    {
      // Story files are not components; the rule misreads their exports
      files: ["**/*.stories.*"],
      rules: {
        "vue/multi-word-component-names": "off",
      },
    },
  ],
  extends: [
    "@vue/typescript/recommended",
    "plugin:vue/vue3-recommended",
    "plugin:storybook/recommended",
    "plugin:vuejs-accessibility/recommended",
    // Must stay last so it can turn off rules that conflict with Prettier
    "plugin:prettier/recommended",
  ],
};
