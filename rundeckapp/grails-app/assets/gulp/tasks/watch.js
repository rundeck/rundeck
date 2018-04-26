'use strict';

let gulp = require('gulp');
let notifier = require('node-notifier');

// Watch
gulp.task('watch', function () {

  gulp.watch('./less/**/*.less', ['less']);  // Watch all the .less files, then run the styles task
  gulp.watch('./scss/**/*.scss', ['scss']);  // Watch all the .less files, then run the styles task
  notifier.notify({ title: 'Gulp Watch', message: 'Started' });

});
