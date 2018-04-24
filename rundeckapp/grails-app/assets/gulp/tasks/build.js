'use strict';

let gulp = require('gulp');
let notifier = require('node-notifier');

// Build
gulp.task('build', ['styles'], function() {
  notifier.notify({ title: 'CSS Build', message: 'Done' });
});
