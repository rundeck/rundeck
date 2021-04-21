// webpack.mix.js

let mix = require('laravel-mix');

mix
    .copy('node_modules/jquery/dist/jquery.min.js', '../vendor-jquery.js')
    .copy('node_modules/bootstrap/dist/js/bootstrap.min.js', '../vendor-bootstrap-all.js')
    .copy('node_modules/knockout/build/output/knockout-latest.js', '../vendor-knockout.min.js')
    .copy('node_modules/knockout-mapping/dist/knockout.mapping.min.js', '../vendor-knockout-mapping.js')
    .copy('node_modules/perfect-scrollbar/dist/perfect-scrollbar.min.js', '../vendor-perfect-scrollbar.js')
    .copy('node_modules/jquery-ui-timepicker-addon/dist/jquery-ui-timepicker-addon.min.js', '../vendor-jquery-ui-timepicker-addon.js')
    