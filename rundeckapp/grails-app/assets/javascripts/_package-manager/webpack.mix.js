// webpack.mix.js

let mix = require('laravel-mix');

mix
    .copy('node_modules/jquery/dist/jquery.min.js', '../vendor/jquery.js')
    .copy('node_modules/jquery-migrate/dist/jquery-migrate.js', '../vendor/jquery-migrate.js')
    .copy('node_modules/jquery-ui-dist/jquery-ui.min.js', '../vendor/jquery-ui.js')
    .copy('node_modules/jquery-ui-dist/jquery-ui.css', '../vendor/jquery-ui.css')
    .copy('node_modules/jquery-ui-timepicker-addon/dist/jquery-ui-timepicker-addon.min.js', '../vendor/jquery-ui-timepicker-addon.js')
    .copy('node_modules/knockout/build/output/knockout-latest.js', '../vendor/knockout.min.js')
    .copy('node_modules/knockout-mapping/dist/knockout.mapping.min.js', '../vendor/knockout-mapping.js')
    .copy('node_modules/perfect-scrollbar/dist/perfect-scrollbar.min.js', '../vendor/perfect-scrollbar.js')
    .copy('node_modules/perfect-scrollbar/css/perfect-scrollbar.css', '../vendor/perfect-scrollbar.css')
    .copy('node_modules/vue/dist/vue.js', '../vendor/vue.js')
    .copy('node_modules/vue/dist/vue.min.js', '../vendor/vue.min.js')
    .combine(['node_modules/bootstrap/dist/js/bootstrap.js', './popover-default.js'], '../vendor/bootstrap/bootstrap.js');
