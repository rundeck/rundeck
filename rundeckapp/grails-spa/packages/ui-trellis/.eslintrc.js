module.exports = {
  env: {
    node: true,
  },
  parser: "vue-eslint-parser",
  parserOptions: {
    parser: "@typescript-eslint/parser",
    sourceType: "module",
  },
  globals: {
    defineProps: "readonly",
    defineEmits: "readonly",
    withDefaults: "readonly",
  },
  rules: {
    // 'prefer-const': WARN,
    // 'no-unused-vars': ERROR,
  },
  ignorePatterns: ["build", "test", ".storybook"],
  extends: [
    "@vue/typescript/recommended",
    "plugin:vue/vue3-recommended",
    "plugin:storybook/recommended",
    "plugin:vuejs-accessibility/recommended",
    "plugin:prettier/recommended",
  ],
};
