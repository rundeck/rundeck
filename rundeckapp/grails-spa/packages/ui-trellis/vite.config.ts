import { defineConfig, loadEnv, splitVendorChunkPlugin } from 'vite'
import { resolve } from 'path'
import vue from '@vitejs/plugin-vue'
import VueI18nPlugin from '@intlify/unplugin-vue-i18n/vite'
import vueJsx from '@vitejs/plugin-vue-jsx'
import { viteExternalsPlugin } from 'vite-plugin-externals'
import symfonyPlugin from "vite-plugin-symfony"

// https://vitejs.dev/config/
export default defineConfig(({ command, mode })=> {
    process.env = Object.assign(process.env, loadEnv(mode, process.cwd(), ''));
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
            splitVendorChunkPlugin(),
            symfonyPlugin(),
        ],
        base: '/assets/static/',
        publicDir: '/assets/static/',
        appType: 'custom',
        server: {
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
            rollupOptions: {
                input: {
                    'components/central': resolve(__dirname, 'src/app/components/central/main.ts'),
                    'components/community-news-notification': resolve(__dirname, 'src/app/components/community-news-notification/main.js'),
                    'components/copybox': resolve(__dirname, 'src/app/components/copybox/main.ts'),
                    'components/uisockets': resolve(__dirname, 'src/app/components/ui/main.ts'),
                    'components/first-run': resolve(__dirname, 'src/app/components/first-run/main.ts'),
                    'components/ko-paginator': resolve(__dirname, 'src/app/components/ko-paginator/main.ts'),
                    'components/motd': resolve(__dirname, 'src/app/components/motd/main.js'),
                    'components/navbar': resolve(__dirname, 'src/app/components/navbar/main.ts'),
                    'components/project-picker': resolve(__dirname, 'src/app/components/project-picker/main.ts'),
                    'components/theme': resolve(__dirname, 'src/app/components/theme/main.ts'),
                    'components/tour': resolve(__dirname, 'src/app/components/tour/main.js'),
                    'components/version': resolve(__dirname, 'src/app/components/version/main.js'),
                    'components/server-identity': resolve(__dirname, 'src/app/components/server-identity/serverIdentity.js'),
                    'pages/storage': resolve(__dirname, 'src/app/pages/storage/main.ts'),
                    'pages/login': resolve(__dirname, 'src/app/pages/login/main.ts'),
                    'pages/project-dashboard': resolve(__dirname, 'src/app/pages/project-dashboard/main.js'),
                    'pages/project-activity': resolve(__dirname, 'src/app/pages/project-activity/main.js'),
                    'pages/repository': resolve(__dirname, 'src/app/pages/repository/main.js'),
                    'pages/command': resolve(__dirname, 'src/app/pages/command/main.ts'),
                    'pages/community-news': resolve(__dirname, 'src/app/pages/community-news/main.js'),
                    'pages/project-nodes-config': resolve(__dirname, 'src/app/pages/project-nodes-config/main.js'),
                    'pages/project-nodes-editor': resolve(__dirname, 'src/app/pages/project-nodes-editor/main.ts'),
                    'pages/project-config': resolve(__dirname, 'src/app/pages/project-config/main.js'),
                    'pages/execution-show': resolve(__dirname, 'src/app/pages/execution-show/main.js'),
                    'pages/webhooks': resolve(__dirname, 'src/app/pages/webhooks/main.js'),
                    'pages/user-summary': resolve(__dirname, 'src/app/pages/menu/main.js'),
                    'pages/dynamic-form': resolve(__dirname, 'src/app/pages/dynamic-form/main.js'),
                    'pages/job/editor': resolve(__dirname, 'src/app/pages/job/editor/main.js'),
                },
                output: {
                    entryFileNames: `[name].js`,
                    chunkFileNames: `[name].js`,
                    assetFileNames: (assetInfo) => {
                        let [_, extType] = assetInfo.name.split('.');
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
