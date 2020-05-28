const Glob = require('glob')

const Path = require('path')
const walk = require('walk')

const nodeExternals = require('webpack-node-externals');

/** Create a "page" for each component */
pages = {}
walk.walkSync('./src', {
    listeners: {
        file: (root, stat, next) => {
            if (Path.parse(stat.name).ext != '.vue')
                return
            const base = root.split(Path.sep).slice(3).join(Path.sep)
            const entry = Path.join(root, stat.name)

            const component = Path.join(base, Path.parse(stat.name).name)

            pages[component] = {entry, chunks: ['index']}
        }
    }
})

console.log(pages)

module.exports = {
  pages,

  outputDir: './lib/components',
  publicPath: './',
  filenameHashing: false,
  parallel: false,
  css: {
    extract: false
  },
  /** Don't emit index html files */
  chainWebpack: config => {
    config.entryPoints.store.forEach( (_, entry) => {
      config.plugins.delete(`html-${entry}`)
      config.plugins.delete(`preload-${entry}`)
      config.plugins.delete(`prefetch-${entry}`)
    })

    config.module.rule('ts').uses.delete('cache-loader')
    config.module.rule('tsx').uses.delete('cache-loader')
  },
  configureWebpack: config => {
    config.devtool = process.env.NODENV = 'production' ? 'source-map' : 'eval-source-map'

    /** Put vue in extension so files match typescript decleration files */
    config.output.filename = (asset) => {
      if (asset.chunk.entryModule._identifier.endsWith('vue'))
        return '[name].vue.js'
      else
        return '[name].js'
    }
    config.output.library = 'rundeckCore'
    config.output.libraryTarget = 'commonjs2'

    config.externals = [
      /** Externalize everything under node_modules: Use peer deps */
      nodeExternals(),
      /** Externalize local project imports: ie require('../util/Foo') */
      function (context, request, callback) {
        if (/^\..*\.vue$/.test(request)) // Components requiring other components
          return callback(null, request)

        if (request.startsWith('.')
            && !request.includes('?') // These are typically compile time generated files in flight
            && !context.includes('node_modules') // Runtime stuff still getting required from node_modules
            && !request.includes('node_modules')) {
          console.log(request)
          return callback(null, request)
        }

        callback()
      }
    ]

    /** Don't minimize or split chunks */
    config.optimization.minimize = false
    config.optimization.splitChunks = false

    /**
     * Disable transpile only so types are emitted
     * Use custom tsconfig to exclude stories
    */
    config.module.rules.forEach( r => {
      if (r.use)
        r.use.forEach( u => {
          if (u.loader.match(/ts-loader/)) {
            u.options.transpileOnly = false
            u.options.configFile = 'tsconfig.webpack.json'
            u.options.logLevel = 'info'
          }
        })
    })
  }
};