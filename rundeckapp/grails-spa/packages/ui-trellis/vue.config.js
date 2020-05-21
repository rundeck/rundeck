const Glob = require('glob')

const Path = require('path')
const walk = require('walk')

/** Create a "page" for each component */
pages = {}
walk.walkSync('./src', {
    listeners: {
        file: (root, stat, next) => {
            if (Path.parse(stat.name).ext != '.vue')
                return
            const base = root.split(Path.sep).slice(2).join(Path.sep)
            const entry = Path.join(root, stat.name)

            const component = Path.join(base, Path.parse(stat.name).name)

            pages[component] = {entry, chunks: ['index']}
        }
    }
})

module.exports = {
  pages,

  outputDir: './lib',
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

    /** Externalize vue */
    config.externals = {'vue': 'vue'}

    /** Don't minimize or split chunks */
    config.optimization.minimize = false
    config.optimization.splitChunks = false

    /** Disable transpile only so types are emitted */
    config.module.rules.forEach( r => {
      if (r.use)
        r.use.forEach( u => {
          if (u.loader.match(/ts-loader/))
            u.options.transpileOnly = false
        })
    })
  }
};