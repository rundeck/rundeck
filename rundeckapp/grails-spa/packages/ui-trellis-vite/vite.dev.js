import path from "path";
import { defineConfig } from "vite";
import { createVuePlugin } from "vite-plugin-vue2";


const config = defineConfig({
  resolve: {
    alias: {
      "@": `${path.resolve(__dirname, "src")}`,
    }
  },


  plugins: [
    createVuePlugin(),
  ],

  server: {
    port: 8080,
  },
});

export default config;