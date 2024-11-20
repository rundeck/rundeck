import { defineConfig, loadEnv } from 'vite'
import { resolve } from 'path'
import vue from '@vitejs/plugin-vue'
import VueI18nPlugin from '@intlify/unplugin-vue-i18n/vite'
import vueJsx from '@vitejs/plugin-vue-jsx'
import { viteExternalsPlugin } from "vite-plugin-externals";
import symfonyPlugin from 'vite-plugin-symfony'

// https://vite.dev/config/
export default defineConfig(({ command, mode })=> {
  process.env = Object.assign(process.env, loadEnv(mode, process.cwd(), ''));
  const inputs = {
    'components/central': 'src/app/components/central/main.ts',
    'components/community-news-notification': 'src/app/components/community-news-notification/main.js',
    'components/copybox': 'src/app/components/copybox/main.ts',
    'components/uisockets': 'src/app/components/ui/main.ts',
    'components/first-run': 'src/app/components/first-run/main.ts',
    'components/ko-paginator': 'src/app/components/ko-paginator/main.ts',
    'components/motd': 'src/app/components/motd/main.js',
    'components/navbar': 'src/app/components/navbar/main.ts',
    'components/project-picker': 'src/app/components/project-picker/main.ts',
    'components/theme': 'src/app/components/theme/main.ts',
    'components/tour': 'src/app/components/tour/main.js',
    'components/version': 'src/app/components/version/main.js',
    'components/server-identity': 'src/app/components/server-identity/serverIdentity.ts',
    'components/readme-motd': './src/app/components/readme-motd/main.ts',
    'pages/storage': 'src/app/pages/storage/main.ts',
    'pages/login': 'src/app/pages/login/main.ts',
    'pages/project-dashboard': 'src/app/pages/project-dashboard/main.js',
    'pages/project-activity': 'src/app/pages/project-activity/main.js',
    'pages/repository': 'src/app/pages/repository/main.js',
    'pages/command': 'src/app/pages/command/main.ts',
    'pages/community-news': 'src/app/pages/community-news/main.js',
    'pages/project-nodes-config': 'src/app/pages/project-nodes-config/main.js',
    'pages/project-nodes-editor': 'src/app/pages/project-nodes-editor/main.ts',
    'pages/project-config': 'src/app/pages/project-config/main.js',
    'pages/execution-show': 'src/app/pages/execution-show/main.js',
    'pages/webhooks': 'src/app/pages/webhooks/main.js',
    'pages/user-summary': 'src/app/pages/menu/main.js',
    'pages/dynamic-form': 'src/app/pages/dynamic-form/main.js',
    'pages/job/editor': 'src/app/pages/job/editor/main.js',
    "pages/nodes": "./src/app/pages/nodes/main.ts",
    "pages/job/browse": "./src/app/pages/job/browse/main.ts",
    "pages/home": "./src/app/pages/home/main.ts",
    "pages/job/head/scm-action-buttons": "./src/app/pages/job/head/scm/scm-action-buttons.ts",
    "pages/job/head/scm-status-badge": "./src/app/pages/job/head/scm/scm-status-badge.ts",
  };
  return {
    plugins: [
        vue(),
        VueI18nPlugin({
          include: [
            resolve(__dirname, 'node_modules/uiv/src/locale/lang/**'),
            resolve(__dirname, 'app/utilities/locales/**'),
          ],
          runtimeOnly: false,
        }),
        vueJsx(),
        viteExternalsPlugin({
          vue: 'Vue',
        }),
        symfonyPlugin(),
    ],
    base: '/assets/static/',
    publicDir: '/assets/static/',
    appType: 'custom',
    server: {
      host: '127.0.0.1',
      port: 4173,
      cors: true,
      hmr: false,
      watch: {
        followSymlinks: true,
      },
      origin: "http://127.0.0.1:4173",
    },
    build: {
      outDir: process.env.VUE_APP_OUTPUT_DIR,
      cssCodeSplit: true,
      sourcemap: true,
      manifest: true,
      emptyOutDir: true,
      assetsInlineLimit: 0,
      rollupOptions: {
        input: Object.entries(inputs).reduce((acc, [key, value]) => ({ ...acc, [key]: resolve(__dirname, value) }), {}),
        output: {
          entryFileNames: `[name].js`,
          chunkFileNames: `[name].js`,
          assetFileNames: (assetInfo) => {
            const [names = ""] = assetInfo.names || [];
            const [originalFilename = ""] = assetInfo.originalFileNames || [];
            let [_, extType] = names.split('.');
            if (/png|jpe?g|svg|gif|tiff|bmp|ico/i.test(extType)) {
              extType = 'img';
            }
            if (/eot|ttf|woff2?/i.test(extType)) {
              extType = 'fonts';
            }
            return `${extType}/[name][extname]`;
          },
        },
        external: [
          'vue',
        ],
      },
    },
    resolve: {
      preserveSymlinks: true,
      alias: {
        vue: 'Vue',
        '@': resolve(__dirname, './src'),
        '~perfect-scrollbar': resolve(__dirname, 'node_modules', 'perfect-scrollbar'),
        '~vue-virtual-scroller': resolve(__dirname, 'node_modules', 'vue-virtual-scroller'),
        '~vue-good-table-next': resolve(__dirname, 'node_modules', 'vue-good-table-next'),
        '~vue3-markdown': resolve(__dirname, 'node_modules', 'vue3-markdown'),
        'uiv/src': resolve(__dirname, 'node_modules', 'uiv', 'src')
      }
    },
  }
})
