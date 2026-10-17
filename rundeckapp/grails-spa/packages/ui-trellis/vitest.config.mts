import { fileURLToPath, URL } from "node:url";
import { transformAsync } from "@babel/core";
import classProperties from "@babel/plugin-proposal-class-properties";
import decorators from "@babel/plugin-proposal-decorators";
import { defineConfig } from "vitest/config";
import vue from "@vitejs/plugin-vue";

const esModules = [
  "vue-virtual-scroller",
  "uuid",
  "primevue",
  "@primevue",
  "@primeuix",
];

export default defineConfig({
  plugins: [
    {
      name: "legacy-mobx-decorators",
      enforce: "pre",
      async transform(code, id) {
        if (!id.includes("/src/library/stores/") || !id.endsWith(".ts")) {
          return null;
        }

        const result = await transformAsync(code, {
          filename: id,
          babelrc: false,
          configFile: false,
          parserOpts: {
            plugins: ["typescript", "decorators-legacy"],
          },
          plugins: [
            [decorators, { version: "legacy" }],
            [classProperties, { loose: true }],
          ],
          sourceMaps: true,
        });

        return result?.code
          ? { code: result.code, map: result.map ?? undefined }
          : null;
      },
    },
    vue(),
  ],
  resolve: {
    alias: {
      "@": fileURLToPath(new URL("./src", import.meta.url)),
      "/static/images/icon-tiny-removex-gray.png": fileURLToPath(
        new URL("./src/__mocks__/fileMock.js", import.meta.url),
      ),
      "vue-i18n": fileURLToPath(
        new URL("./node_modules/vue-i18n/dist/vue-i18n.cjs", import.meta.url),
      ),
    },
  },
  test: {
    environment: "happy-dom",
    pool: "vmThreads",
    environmentOptions: {
      happyDOM: {
        url: "http://localhost:4440/",
      },
    },
    globals: true,
    setupFiles: ["./vitest.setup.ts"],
    include: [
      "src/**/*.test.ts",
      "src/**/*.spec.ts",
      "src/**/*.vitest.ts",
      "tests/**/*.test.ts",
      "tests/**/*.spec.ts",
      "tests/**/*.vitest.ts",
    ],
    exclude: ["public/**"],
    clearMocks: true,
    sequence: {
      hooks: "list",
    },
    reporters: [
      "default",
      ["junit", { outputFile: "build/test-results/rundeck-vitest-junit.xml" }],
    ],
    server: {
      deps: {
        inline: esModules,
      },
    },
  },
});
