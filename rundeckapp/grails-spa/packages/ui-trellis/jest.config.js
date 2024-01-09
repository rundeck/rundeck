const esModules = ['vue-virtual-scroller', 'uuid'].join('|');
module.exports = {
  globals: {
    'ts-jest': {
      tsconfig: 'tsconfig.app.json',
    }
  },
  moduleFileExtensions: [
    'js',
    'ts',
    'vue',
  ],
  transform: {
    "^.+\\.tsx?$": "ts-jest",
    '^.+\\.vue$': '@vue/vue3-jest',
    '^.+\\.(j|t)s$': 'babel-jest',
  },
  setupFiles: ['<rootDir>/setupTests.js'],
  roots: ['<rootDir>/src/app', '<rootDir>/src/library', '<rootDir>/tests'],
  modulePathIgnorePatterns: [
    '<rootDir>/public',
  ],
  testMatch: [
    '**/*.test.ts',
    '**/*.spec.ts',
  ],
  verbose: true,
  testEnvironment: "@happy-dom/jest-environment",
  transformIgnorePatterns: [`/node_modules/(?!${esModules})`],
};