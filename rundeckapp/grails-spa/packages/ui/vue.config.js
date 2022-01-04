const Path = require('path')
const webpack = require('webpack')

module.exports = {
  pages: {
    'components/copybox': { entry: './src/components/copybox/main.ts' },
    'components/central': { entry: './src/components/central/main.ts'},
    'components/ko-paginator': { entry: './src/components/ko-paginator/main.ts'},
    'components/motd': { entry: './src/components/motd/main.js'},
    'components/navbar': { entry: './src/components/navbar/main.ts'},
    'components/project-picker': { entry: './src/components/project-picker/main.ts'},
    'components/first-run': { entry: './src/components/first-run/main.ts' },
    'components/theme': { entry: './src/components/theme/main.ts'},
    'components/tour': { entry: './src/components/tour/main.js'},
    'components/version-notification': { entry: './src/components/version-notification/main.js'},
    'components/version': { entry: './src/components/version/main.js'},
    'components/community-news-notification': { entry: './src/components/community-news-notification/main.js'},
    'pages/login': { entry: './src/pages/login/main.ts'},
    'pages/project-dashboard': { entry: './src/pages/project-dashboard/main.js'},
    'pages/project-activity': { entry: './src/pages/project-activity/main.js'},
    'pages/repository': { entry: './src/pages/repository/main.js'},
    'pages/command': { entry: './src/pages/command/main.ts'},
    'pages/community-news': { entry: './src/pages/community-news/main.js'},
    'pages/project-nodes-config': { entry: './src/pages/project-nodes-config/main.js'},
    'pages/execution-show': { entry: './src/pages/execution-show/main.js'},
    'pages/webhooks': { entry: './src/pages/webhooks/main.js'},
    'pages/user-summary': {entry: './src/pages/menu/main.js'},
    'pages/dynamic-form': {entry: './src/pages/dynamic-form/main.js'},
    'pages/job/editor': {entry: './src/pages/job/editor/main.js'},

  },

  outputDir: process.env.VUE_APP_OUTPUT_DIR,
  publicPath: '/assets/static/',
  filenameHashing: false,
  parallel: true,
  css: {
    sourceMap: true,
    /** Workaround for Vue CLI accounting for nested page paths
     * https://github.com/vuejs/vue-cli/issues/4378
    */
    extract: process.env.VUE_APP_CSS_EXTRACT == 'true' ? {
      filename: '/css/[name].css',
      chunkFilename: '/css/[name].css',
    } : false
  },
  chainWebpack: config => {
    /** Do not create index pages for entry points */
    config.entryPoints.store.forEach( (_, entry) => {
      config.plugins.delete(`html-${entry}`)
      config.plugins.delete(`preload-${entry}`)
      config.plugins.delete(`prefetch-${entry}`)
    })

    /** Process source maps from deps */
    config.module.rule('source-map-loader')
      .test(/\.js$/)
      .include.add(/ui-trellis\/lib/).end()
      .enforce('pre')
      .use('source-map-loader')
      .loader('source-map-loader')
      .end()
  },
  configureWebpack: config => {
    config.devtool = process.env.VUE_APP_DEVTOOL
    config.output.filename = (asset) => {
      if (asset.chunk.entryModule._identifier.endsWith('vue'))
        return '[name].vue.js';
      else
        return '[name].js';
    },
    config.output.library = 'rundeckCore';
    config.devServer = {
      hot: true,
      watchOptions: {
        followSymlinks: true,
      },
      proxy: {
        ".": {
          target: "http://localhost:4440"
        }
      }
    };
    config.externals = {'vue': 'Vue'}
    config.plugins = [
      /** Generate source maps for CSS as it does not support eval-source-map */
      new webpack.SourceMapDevToolPlugin({
        filename: "[file].map",
        include: [/\.css$/]
      })
    ],
    /** Don't minimize or split chunks */
    config.optimization.minimize = false
    config.optimization.splitChunks = false
    config.optimization.minimizer.shift()

    /**
     * Disable transpile only so types are emitted
     * Use custom tsconfig to exclude stories
    */
    config.module.rules.forEach( r => {
      if (r.use)
        r.use.forEach( u => {
          if (u.loader.match(/ts-loader/)) {
            u.options.transpileOnly = false
            u.options.onlyCompileBundledFiles = false
            u.options.configFile = 'tsconfig.webpack.json'
            u.options.compilerOptions = {
              declarationDir: './lib',
            }
          }
        })
    })

    /**
     * Add plugin to fixup .d.ts locations after webpack emit
     * Randomly the webpack build will spit the TypeScript .d.ts
     * files out into the wrong directory structure. This appears to be an interplay
     * between ts-loader and how it utilizes the TypeScript compiler in multi-entry(page)
     * builds. Or an error in either.
     * */
    config.plugins.push({
      apply: (compiler) => {
        const logger = compiler.getInfrastructureLogger('RundeckTsFixup')
        compiler.hooks.afterEmit.tap('RundeckTsFixup', (compilation) => {
          if (fse.existsSync('./lib/src')) {
            logger.error('Fixing up .d.ts location')
            fse.copySync('./lib/src', './lib')
            fse.removeSync('./lib/src')
          }
          if (fse.existsSync('./lib/node_modules')) {
            logger.error('Removing extraneous lib/node_modules')
            fse.removeSync('./lib/node_modules')
          }
        })
      }
    })
  }
};
