const Glob = require('glob')

const fse = require('fs-extra')
const Path = require('path')
const walk = require('walk')
const webpack = require('webpack')

const nodeExternals = require('webpack-node-externals');
const BUILD_COPYRIGHT = `© ${new Date().getFullYear()} PagerDuty, Inc. All Rights Reserved.`

process.env.VUE_APP_BUILD_COPYRIGHT = BUILD_COPYRIGHT

/** Create a "page" for each component */
pages = {}
walk.walkSync('./src/library/components', {
    listeners: {
        file: (root, stat, next) => {
            const path = Path.parse(stat.name)

            /** Use this to limit the build to a specific path for testing */
            // if (!root.includes('tabs'))
            //   return

            /* We only want to register the following as entry points */
            if (! ['.vue', '.tsx', '.jsx', '.ts', '.js'].includes(path.ext) || path.name.includes('stories'))
                return
            const base = root.split(Path.sep).slice(3).join(Path.sep)
            const entry = Path.join(root, stat.name)

            const component = Path.join(base, path.name)

            pages[component] = {entry, chunks: ['index']}
        }
    }
})

module.exports = {
  pages,

  outputDir: './lib',
  publicPath: './',
  filenameHashing: false,
  parallel: true,
  css: {
    extract: false
  },
  /** Don't emit index html files */
  chainWebpack: config => {
    config.entryPoints.store.forEach( (_, entry) => {
      config.plugins.delete(`html-${entry}`)
      config.plugins.delete(`preload-${entry}`)
      config.plugins.delete(`prefetch-${entry}`)
      config.plugins.delete('copy')
    })

    /** Remove cache loaders so .d.ts files are emitted */
    config.module.rule('ts').uses.delete('cache-loader')
    config.module.rule('tsx').uses.delete('cache-loader')
  },
  configureWebpack: config => {
    config.devtool = 'cheap-module-source-map'

    /** Put vue in extension so files match typescript decleration files */
    config.output.filename = (asset) => {
      if (asset.chunk.entryModule._identifier.endsWith('vue'))
        return '[name].vue.js'
      else
        return '[name].js'
    }
    config.output.library = 'rundeckUiTrellis'
    config.output.libraryTarget = 'commonjs2'

    config.externals = [
      /** Externalize everything under node_modules: Use peer deps */
      nodeExternals(),
      /** Externalize local project imports: ie require('../util/Foo') */
      function (context, request, callback) {
       
        if (/^\..*\.vue$/.test(request)) // Components requiring other components
          return callback(null, request)

        /** Inline CSS */
        if (context.startsWith(process.cwd())
          && request.includes('.scss')) {
          return callback()
        }

        /** Bundle javascript code inside components */
        if (request.startsWith('./')
          && !context.endsWith('.scss')
          && !context.includes('node_modules')
          && !request.includes('.vue')) {
          return callback(null, request)
        }

        if (request.startsWith('.')
            && !request.includes('?') // These are typically compile time generated files in flight
            && !context.includes('node_modules') // Runtime stuff still getting required from node_modules
            && !request.includes('node_modules')) {
          return callback(null, request)
        }

        callback()
      }
    ]

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
          console.log(fse.existsSync('./lib'))
          if (fse.existsSync('./lib')) {
            logger.error('Fixing up .d.ts location')
            console.log(fse.existsSync('./lib/src'))
            // fse.copySync('./lib/src/library', './lib')
            // fse.removeSync('./lib/src')
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