const Util = require('util')
const webpack = require('webpack')
const path = require('path');

module.exports = {
    typescript: {
        check: false,
    },
    addons: [
        {
            name: '@storybook/addon-docs',
            options: {
                vueDocgenOptions: {
                    alias: {
                        '@': path.resolve(__dirname, '../'),
                    },
                },
            },
        },
       // '@storybook/addon-knobs',
        'storybook-dark-mode',
        {
            name: "@storybook/addon-styling",
            options: {
                scssBuildRule: {
                    test: /\.scss$/,
                    use: [
                        {loader: 'vue-style-loader'},
                        {loader: 'css-loader', options: {sourceMap: true}},
                        {loader: 'postcss-loader', options: {sourceMap: true, postcssOptions: { plugins: [require('autoprefixer')]}}},
                        {loader: 'sass-loader', options: {sourceMap: true}},
                    ],
                }
            }
        },
        '@storybook/addon-backgrounds'
    ],
    stories: [`${process.cwd()}/src/**/*.stories.(ts|js|tsx|jsx)`],
    staticDirs: ['../public','../theme','../tests/data'],
    webpackFinal: (config) => {
        const vueLoader = config.module.rules.find(r => String(r.test) == String(/\.vue$/))
        vueLoader.options.compilerOptions = {
            preserveWhitespace: false
        }

        const cssLoader = config.module.rules.find(r => String(r.test) == String(/\.css$/))
        cssLoader.use[1].options.sourceMap = true

        config.optimization.splitChunks = false

        config.devtool = 'eval-source-map'
        config.module.rules.push(
        {
            test: /\.ts$/,
            exclude: /node_modules/,
            use: [{
                loader: 'ts-loader',
                options: {
                appendTsSuffixTo: [/\.vue$/],
                transpileOnly: true
                },
            }],
        },
            {
                test: /\.(ts|js)x?$/,
                include: /uiv/,
                use: { loader: "babel-loader" },
            },
        );

        config.plugins.unshift(new webpack.NormalModuleReplacementPlugin( /index.less/, function(resource) {
            if (resource.resource) {
                resource.resource = resource.resource.replace(/node_modules\/ant-design-vue\/es\/style\/index\.less/, 'src/antScope.less')
            }
        }))

        return {
            ...config,
        };
    }
}