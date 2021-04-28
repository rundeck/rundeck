// webpack.mix.js

let mix = require('laravel-mix');

mix
    .copy('node_modules/jquery/dist/jquery.min.js', '../jquery.js')
    .copy('node_modules/bootstrap/dist/js/bootstrap.js', '../bootstrap/bootstrap-341.js')
    .copy('node_modules/knockout/build/output/knockout-latest.js', '../knockout.min.js')
    .copy('node_modules/knockout-mapping/dist/knockout.mapping.min.js', '../knockout-mapping.js')
    .copy('node_modules/perfect-scrollbar/dist/perfect-scrollbar.min.js', '../perfect-scrollbar.js')
    .copy('node_modules/jquery-ui-timepicker-addon/dist/jquery-ui-timepicker-addon.min.js', '../jquery-ui-timepicker-addon.js')
    .combine(['../bootstrap/bootstrap-341.js', '../bootstrap/popover-default.js'], '../bootstrap/bootstrap.js');
