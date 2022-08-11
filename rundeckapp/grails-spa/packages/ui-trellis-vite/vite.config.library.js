import path from 'path'
import { defineConfig } from 'vite'
import { createVuePlugin as Vue2 } from 'vite-plugin-vue2'

import vueJsx from '@vitejs/plugin-vue-jsx'

const config = defineConfig({
  resolve: {
    alias: {
      '@': `${path.resolve(__dirname, 'src')}`,
    },
    dedupe: ['vue-demi'],
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
  },
  css: {
    preprocessorOptions: {
      scss: {
        additionalData: `
          @import "./src/library/scss/app.scss";
        `
      }
    }
  },
  build: {
    minify: false,
    outDir: 'lib',
    lib: {
      entry: path.resolve(__dirname, 'src/library.ts'),
      name: 'UiTrellisLib',
      fileName: (format) => `uitrellis.${format}.js`
    },
  },

  plugins: [
    Vue2(),
    vueJsx()
  ],

  server: {
    port: 3333,
  },
})

export default config
