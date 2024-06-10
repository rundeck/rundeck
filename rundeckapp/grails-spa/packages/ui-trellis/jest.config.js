const esModules = ["vue-virtual-scroller", "uuid"].join("|");
module.exports = {
  moduleFileExtensions: ["js", "ts", "vue"],
  transform: {
    "^.+\\.tsx?$": ["ts-jest", { tsconfig: "tsconfig.app.json" }],
    "^.+\\.vue$": "@vue/vue3-jest",
    "^.+\\.(j|t)s$": "babel-jest",
  },
  setupFiles: ["<rootDir>/setupTests.js"],
  roots: ["<rootDir>/src/app", "<rootDir>/src/library", "<rootDir>/tests"],
  moduleNameMapper: {
    "\\.(css|less|sass|scss)$": "identity-obj-proxy",
    "^@/(.*)$": "<rootDir>/src/$1",
  },
  modulePathIgnorePatterns: ["<rootDir>/public"],
  testMatch: ["**/*.test.ts", "**/*.spec.ts"],
  verbose: true,
  testEnvironment: "@happy-dom/jest-environment",
  transformIgnorePatterns: [`/node_modules/(?!${esModules})`],
  clearMocks: true,
  reporters: ["default", "jest-junit"],
};
