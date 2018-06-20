'use strict';

let gulp = require('gulp');
let notifier = require('node-notifier');

// Default task
gulp.task('default', ['build', 'watch'], function () {
  // notifier.notify({ title: 'Gulp', message: 'Started' });    
});
