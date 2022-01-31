// webpack.mix.js

let mix = require('laravel-mix');

mix
  .ts('./src/components/copybox/main.ts', '../../../grails-app/assets/provided/static/components/copybox')
  .setPublicPath('./assets/static/');