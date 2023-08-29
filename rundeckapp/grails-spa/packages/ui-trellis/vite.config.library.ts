import { defineConfig, loadEnv } from 'vite'
import { resolve } from 'path'
import vue from '@vitejs/plugin-vue'
import VueI18nPlugin from "@intlify/unplugin-vue-i18n/vite";
import vueJsx from '@vitejs/plugin-vue-jsx'

const BUILD_COPYRIGHT = `© ${new Date().getFullYear()} PagerDuty, Inc. All Rights Reserved.`

// https://vitejs.dev/config/
export default defineConfig(({ mode }) => {
    process.env = Object.assign(process.env, loadEnv(mode, process.cwd(), ''));
    return {
        plugins: [
            vue({
                template: {
                    compilerOptions: {
                        compatConfig: {
                            MODE: 2
                        }
                    }
                }
            }),
            VueI18nPlugin({
                include: [
                    resolve(__dirname, './src/library/components/plugins/i18n.ts')
                ],
            }),
            vueJsx(),
        ],
        define: {
            BUILD_COPYRIGHT: JSON.stringify(BUILD_COPYRIGHT)
        },
        publicDir: '/assets/static/',
        appType: 'mpa',
        build: {
            outDir: 'lib',
            lib: {
                entry: resolve(__dirname, 'src/library/library.ts'),
                name: 'rundeckUiTrellis',
                formats: ['es', 'umd', 'cjs'],
                fileName: (format) => `rundeck-ui-trellis.${format}.js`,
            },
            rollupOptions: {
                external: [
                    'vue',
                    'axios',
                    'vue-cookies',
                    'vue-moment',
                    'vue-multiselect',
                    '@rundeck/client',
                    'lodash',
                    'timers',
                    'vue2-filters',
                    'vue-scrollto',
                    'vue-i18n',
                    'vue-fuse',
                    'prex',
                    'uiv',
                    'vue3-markdown',
                    'mobx-vue-lite',
                ],
                output: {
                    globals: {
                        vue: 'Vue',
                        axios: 'axios',
                        'vue-cookies': 'VueCookies',
                        'vue-moment': 'VueMoment',
                        'vue-multiselect': 'VueMultiselect',
                        '@rundeck/client': '@Rundeck/Client',
                        'lodash': 'lodash',
                        'timers': 'timers',
                        'vue2-filters': 'Vue2Filters',
                        'vue-scrollto': 'VueScrollTo',
                        'vue-i18n': 'VueI18n',
                        'vue-fuse': 'VueFuse',
                        'prex': 'Prex',
                        'uiv': 'Uiv',
                        'vue3-markdown': 'MarkdownItVue',
                        'mobx-vue-lite': 'MobxVueLite',
                    }
                }
            },
            commonjsOptions: {
                include: ['**/i18n*']
            },
        },
        resolve: {
            alias: {
                vue: '@vue/compat'
            }
        },
    }
})
