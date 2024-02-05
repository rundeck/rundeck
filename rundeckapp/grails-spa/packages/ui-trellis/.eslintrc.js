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
    "@typescript-eslint/ban-ts-comment": "off",
    "@typescript-eslint/no-explicit-any": "off",
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
