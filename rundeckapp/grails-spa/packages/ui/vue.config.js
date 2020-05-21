const Path = require('path')
const webpack = require('webpack')

module.exports = {
  pages: {
    'components/central': { entry: './src/components/central/main.ts'},
    'components/execution-log': { entry: './src/components/execution-log/main.js'},
    'components/motd': { entry: './src/components/motd/main.js'},
    'components/tour': { entry: './src/components/tour/main.js'},
    'components/version-notification': { entry: './src/components/version-notification/main.js'},
    'components/community-news-notification': { entry: './src/components/community-news-notification/main.js'},
    'pages/project-dashboard': { entry: './src/pages/project-dashboard/main.js'},
    'pages/project-activity': { entry: './src/pages/project-activity/main.js'},
    'pages/repository': { entry: './src/pages/repository/main.js'},
    'pages/community-news': { entry: './src/pages/community-news/main.js'},
    'pages/project-nodes-config': { entry: './src/pages/project-nodes-config/main.js'},
    'pages/webhooks': { entry: './src/pages/webhooks/main.js'},
    'pages/user-summary': {entry: './src/pages/menu/main.js'}
  },

  outputDir: process.env.VUE_APP_OUTPUT_DIR,
  publicPath: './',
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
    config.entryPoints.store.forEach( (_, entry) => {
      config.plugins.delete(`html-${entry}`)
      config.plugins.delete(`preload-${entry}`)
      config.plugins.delete(`prefetch-${entry}`)
    })
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
        resource.request = resource.request.replace(/node_modules\/ant-design-vue\/es\/style\/index\.less/, 'src/components/execution-log/antScope.less')
      })
    ]
  }
};