// webpack.mix.js

let mix = require('laravel-mix');

mix
    .copy('node_modules/jquery/dist/jquery.min.js', '../vendor/jquery.js')
    .copy('node_modules/jquery-ui-dist/jquery-ui.min.js', '../vendor/jquery-ui.js')
    .copy('node_modules/jquery-ui-dist/jquery-ui.css', '../vendor/jquery-ui.css')
    .copy('node_modules/jquery-ui-timepicker-addon/dist/jquery-ui-timepicker-addon.min.js', '../vendor/jquery-ui-timepicker-addon.js')
    .copy('../knockout_3_5_1.js', '../vendor/knockout.min.js')
    .copy('../jquery_autocomplete_1_3_0.js', '../vendor/jquery.autocomplete.min.js')
    .copy('node_modules/knockout-mapping/dist/knockout.mapping.min.js', '../vendor/knockout-mapping.js')
    .copy('node_modules/perfect-scrollbar/dist/perfect-scrollbar.min.js', '../vendor/perfect-scrollbar.js')
    .copy('node_modules/perfect-scrollbar/css/perfect-scrollbar.css', '../vendor/perfect-scrollbar.css')
    .copy('node_modules/vue/dist/vue.global.js', '../vendor/vue.global.js')
    .copy('node_modules/vue/dist/vue.global.prod.js', '../vendor/vue.global.prod.js')
    .combine(['../bootstrap_3_4_2.js', './popover-default.js'], '../vendor/bootstrap/bootstrap.js');
