'use strict'
require('./check-versions')()

const ora = require('ora')
const rm = require('rimraf')
const path = require('path')
const chalk = require('chalk')
const webpack = require('webpack')
const config = require('../config')
const utils = require('./utils')

const CopyWebpackPlugin = require('copy-webpack-plugin')
const ExtractTextPlugin = require('mini-css-extract-plugin')
const OptimizeCSSPlugin = require('optimize-css-assets-webpack-plugin')
const VueLoaderPlugin = require('vue-loader/lib/plugin')

const buildType = process.env.BUILD_TYPE || 'dev'

function resolve(dir) {
  return path.join(__dirname, '..', dir)
}

const assetsRoot = path.resolve(__dirname, '../../../gradle-build/provided')

const webpackConfig = {
  devtool: '#source-map',

  context: path.resolve(__dirname, '../'),

  entry: {
    'components/central': './src/components/central/main.ts',
    'components/motd': './src/components/motd/main.js',
    'components/tour': './src/components/tour/main.js',
    'pages/project-dashboard': './src/pages/project-dashboard/main.js',
    'pages/project-activity': './src/pages/project-activity/main.js',
    'pages/repository': './src/pages/repository/main.js',
    'pages/project-nodes-config': './src/pages/project-nodes-config/main.js',
    'pages/user-summary': './src/pages/menu/main.js'
  },
  output: {
    path: assetsRoot,
    filename: utils.assetsPath('[name].js'),
    publicPath: '/'
  },
  resolve: {
    extensions: ['.js', '.vue', '.json', '.ts'],
    alias: {
      'vue$': 'vue/dist/vue.esm.js',
      '@': resolve('src')
    }
  },
  module: {
    rules: [
      {
        test: /\.vue$/,
        loader: 'vue-loader',
        options: {
          loaders: {
            'scss': 'vue-style-loader!css-loader!sass-loader',
            'sass': 'vue-style-loader!css-loader!sass-loader?indentedSyntax'
          }
        }
      },
      {
        test: /\.tsx?$/,
        loader: 'ts-loader',
        exclude: /node_modules/,
        options: {
          appendTsSuffixTo: [/\.vue$/],
        }
      },
      {
        test: /\.js$/,
        loader: 'babel-loader',
        include: [resolve('src/spa'), resolve('test'), resolve('node_modules/webpack-dev-server/client')]
      },
      {
        test: /\.(png|jpe?g|gif|svg)(\?.*)?$/,
        loader: 'file-loader',
        options: {
          name: '[name]-[hash:7].[ext]',
          outputPath: './static/img',
          publicPath: '/assets/static/img',
          useRelativePath: true
        }
      },
      {
        test: /\.(mp4|webm|ogg|mp3|wav|flac|aac)(\?.*)?$/,
        loader: 'file-loader',
        options: {
          name: '[name]-[hash:7].[ext]',
          outputPath: './static/media',
          publicPath: '/static/media',
          useRelativePath: true
        }
      },
      {
        test: /\.(woff2?|eot|ttf|otf)(\?.*)?$/,
        loader: 'file-loader',
        options: {
          name: '[name]-[hash:7].[ext]',
          outputPath: './static/fonts',
          publicPath: '/static/fonts',
          useRelativePath: true
        }
      },
      {
        test: /\.scss$/,
        use: [
          'vue-style-loader',
          'css-loader',
          'sass-loader'
        ]
      }
    ]
  },
  node: {
    setImmediate: false,
    dgram: 'empty',
    fs: 'empty',
    net: 'empty',
    tls: 'empty',
    child_process: 'empty'
  },

  optimization: {
    splitChunks: {
      cacheGroups: {
        name: 'vendor',
        chunks: 'all'
      }
    }
  },

  plugins: [
    new VueLoaderPlugin(),
    // http://vuejs.github.io/vue-loader/en/workflow/production.html
    new webpack.DefinePlugin({
      'process.env': 'prod'
    }),
    // new UglifyJsPlugin({
    //   uglifyOptions: {
    //     compress: {
    //       warnings: false
    //     }
    //   },
    //   sourceMap: config.build.productionSourceMap,
    //   parallel: true
    // }),
    // extract css into its own file
    new ExtractTextPlugin({
      filename: utils.assetsPath('css/[name].css'),
      // Setting the following option to `false` will not extract CSS from codesplit chunks.
      // Their CSS will instead be inserted dynamically with style-loader when the codesplit chunk has been loaded by webpack.
      // It's currently set to `true` because we are seeing that sourcemaps are included in the codesplit bundle as well when it's `false`,
      // increasing file size: https://github.com/vuejs-templates/webpack/issues/1110
      allChunks: true
    }),
    // Compress extracted CSS. We are using this plugin so that possible
    // duplicated CSS from different components can be deduped.
    new OptimizeCSSPlugin({
      cssProcessorOptions: config.build.productionSourceMap ? {
        safe: true,
        map: {
          inline: false
        }
      } : {
        safe: true
      }
    }),

    new webpack.HashedModuleIdsPlugin(),

    new webpack.optimize.ModuleConcatenationPlugin(),

    // copy custom static assets
    new CopyWebpackPlugin([{
      // from: path.resolve(__dirname, '../static'),
      from: path.resolve(__dirname),
      to: 'static',
      ignore: ['.*']
    }])
  ]
}

const spinner = ora(`building for ${buildType}...`)
spinner.start()

rm(path.join(assetsRoot, 'static'), err => {
  if (err) throw err
  webpack(webpackConfig, (err, stats) => {
    spinner.stop()
    if (err) throw err
    process.stdout.write(stats.toString({
      colors: true,
      modules: false,
      children: false, // If you are using ts-loader, setting this to true will make TypeScript errors show up during build.
      chunks: false,
      chunkModules: false
    }) + '\n\n')

    if (stats.hasErrors()) {
      console.log(chalk.red('  Build failed with errors.\n'))
      process.exit(1)
    }

    console.log(chalk.cyan('  Build complete.\n'))
    console.log(chalk.yellow(
      '  Tip: built files are meant to be served over an HTTP server.\n' +
      '  Opening index.html over file:// won\'t work.\n'
    ))
  })
})
