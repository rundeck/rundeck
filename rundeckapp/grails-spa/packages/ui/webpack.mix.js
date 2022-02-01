// webpack.mix.js

let mix = require('laravel-mix');

mix
  .ts('./src/components/copybox/main.ts', '../../../grails-app/assets/provided/static/components/copybox')
  .ts('./src/components/navbar/main.ts', '../../../grails-app/assets/provided/static/components/navbar')
  .setPublicPath('./assets/static/');