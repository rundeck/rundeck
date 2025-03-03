const esModules = [
  "vue-virtual-scroller",
  "uuid",
  "primevue/.*",
  "@primevue/.*",
  "@primeuix/styled",
  "@primeuix/utils",
  "@primeuix/styles",
].join("|");
module.exports = {
  moduleFileExtensions: ["js", "ts", "vue"],
  transform: {
    "^.+\\.tsx?$": ["ts-jest", { tsconfig: "tsconfig.app.json" }],
    "^.+\\.vue$": "@vue/vue3-jest",
    "^.+\\.(mj|j|t)s$": "babel-jest",
  },
  setupFiles: ["<rootDir>/setupTests.js"],
  setupFilesAfterEnv: ["<rootDir>/jest.global-mocks.js"],
  roots: ["<rootDir>/src/app", "<rootDir>/src/library", "<rootDir>/tests"],
  moduleNameMapper: {
    "\\.(css|less|sass|scss)$": "identity-obj-proxy",
    "^@/(.*)$": "<rootDir>/src/$1",
    "^@primeuix/styles/(.*)$":
      "<rootDir>/node_modules/@primeuix/styles/$1/index.mjs",
  },
  modulePathIgnorePatterns: ["<rootDir>/public"],
  testMatch: ["**/*.test.ts", "**/*.spec.ts"],
  verbose: true,
  testEnvironment: "@happy-dom/jest-environment",
  transformIgnorePatterns: [`/node_modules/(?!${esModules})`],
  clearMocks: true,
  reporters: ["default", "jest-junit"],
};
