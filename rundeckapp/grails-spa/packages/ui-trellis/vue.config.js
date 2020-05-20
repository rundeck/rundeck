const Path = require('path')
const walk = require('walk')

pages = {}

const walker = walk.walkSync('./src', {
    listeners: {
        file: (root, stat, next) => {
            if (Path.parse(stat.name).ext != '.vue')
                return
            const base = root.split(Path.sep).slice(2).join(Path.sep)
            const entry = Path.join(root, stat.name)

            const component = Path.join(base, Path.parse(stat.name).name)

            pages[component] = {entry}
        }
    }
})

console.log(pages)

module.exports = {
  pages,

  outputDir: './es',
  publicPath: './',
  filenameHashing: false,
  parallel: false,

  chainWebpack: config => {
    config.entryPoints.store.forEach( (_, entry) => {
      config.plugins.delete(`html-${entry}`)
      config.plugins.delete(`preload-${entry}`)
      config.plugins.delete(`prefetch-${entry}`)
    })

    config.module.rule('ts').uses.delete('cache-loader')
    config.module.rule('tsx').uses.delete('cache-loader')

    config.module
      .rule('ts')
      .use('ts-loader')
      .loader('ts-loader')
      .tap(opts => {
        opts.transpileOnly = false;
        opts.happyPackMode = false;
        return opts;
      })

    // config.module
    //   .rule('tsx')
    //   .use('ts-loader')
    //   .loader('ts-loader')
    //   .tap(opts => {
    //     opts.transpileOnly = false;
    //     opts.happyPackMode = false;
    //     return opts;
    //   })
  },
  configureWebpack: config => {
    config.devtool = 'eval-source-map'
    config.output.filename = '[name].js'
    config.output.library = 'rundeckCore'
    externals = {'vue': 'Vue'}

    console.log(config.module.rules)
  }
};