import path from "path";
import { defineConfig } from "vite";
import { createVuePlugin } from "vite-plugin-vue2";
import WindiCSS from "vite-plugin-windicss";
import Components from "unplugin-vue-components/vite";


const config = defineConfig({
  resolve: {
    alias: {
      "@": `${path.resolve(__dirname, "src")}`,
    }
  },

  build: {
    outDir: './dist-app',
    minify: true,
    lib: {
      entry: path.resolve(__dirname, './src/app.ts'),
      name: 'rundeckapp',
      fileName: (format) => `app/app.${format}.js`
    },
    rollupOptions: {
      // make sure to externalize deps that shouldn't be bundled
      // into your library
      external: ['vue'],
      output: {
        // Provide global variables to use in the UMD build
        // for externalized deps
        globals: {
          vue: 'Vue'
        }
      }
    }
  },

  plugins: [
    createVuePlugin(),
    WindiCSS(),
    Components(),
  ],

  server: {
    port: 8080,
  },
});

export default config;
