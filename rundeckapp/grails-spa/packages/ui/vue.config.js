const Path = require('path')
const webpack = require('webpack')

module.exports = {
  pages: {
    'components/central': { entry: './src/components/central/main.ts'},
    'components/motd': { entry: './src/components/motd/main.js'},
    'components/tour': { entry: './src/components/tour/main.js'},
    'components/version-notification': { entry: './src/components/version-notification/main.js'},
    'components/version': { entry: './src/components/version/main.js'},
    'components/community-news-notification': { entry: './src/components/community-news-notification/main.js'},
    'pages/project-dashboard': { entry: './src/pages/project-dashboard/main.js'},
    'pages/project-activity': { entry: './src/pages/project-activity/main.js'},
    'pages/repository': { entry: './src/pages/repository/main.js'},
    'pages/command': { entry: './src/pages/command/main.ts'},
    'pages/community-news': { entry: './src/pages/community-news/main.js'},
    'pages/project-nodes-config': { entry: './src/pages/project-nodes-config/main.js'},
    'pages/execution-show': { entry: './src/pages/execution-show/main.js'},
    'pages/webhooks': { entry: './src/pages/webhooks/main.js'},
    'pages/user-summary': {entry: './src/pages/menu/main.js'}
  },

  outputDir: process.env.VUE_APP_OUTPUT_DIR,
  publicPath: '/assets',
  assetsDir: 'static',
  filenameHashing: false,
  parallel: true,
  css: {
    extract: true,
    loaderOptions: {
      less: {
        lessOptions: {
          javascriptEnabled: true
        }
      }
    }
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
      .enforce('pre')
      .use('source-map-loader')
      .loader('source-map-loader')
      .end()
  },
  configureWebpack: {
    devtool: 'eval-source-map',
    output: {
      filename: Path.join('static', '[name].js'),
      library: 'rundeckCore',
    },
    externals: {'vue': 'Vue'},
    plugins: [
      new webpack.NormalModuleReplacementPlugin( /node_modules\/ant-design-vue\/es\/style\/index\.less/, function(resource) {
        if (resource.resource) {
          resource.resource = resource.resource.replace(/node_modules\/ant-design-vue\/es\/style\/index\.less/, 'src/antScope.less')
        }
      })
    ]
  }
};
