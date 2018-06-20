'use strict';

let gulp = require('gulp');
let gutil = require('gulp-util');
let plumber = require('gulp-plumber');
let less = require('gulp-less');
let sourcemaps = require('gulp-sourcemaps');
let size = require('gulp-size');
let notify = require('gulp-notify');
let rename = require('gulp-rename');


gulp.task('less', function () {
  return gulp.src('./less/app.less')
    .pipe(plumber())
    // .pipe(sourcemaps.init()) // we are not presently minifying
    .pipe(less().on('error', function(err){
      console.log('error', err)
        gutil.log(err);
        this.emit('end');
    }))
    .pipe(size())
    .pipe(rename('app.less.css'))
    // .pipe(sourcemaps.write('./stylesheets')) // we are not presently minifying
    .pipe(gulp.dest('./stylesheets'))
    .pipe(notify('App LESS compiled'));
});


gulp.task('custom-less', function () {
  return gulp.src('./less/custom.less')
    .pipe(plumber())
    // .pipe(sourcemaps.init()) // we are not presently minifying
    .pipe(less().on('error', function(err){
      console.log('error', err)
        gutil.log(err);
        this.emit('end');
    }))
    .pipe(size())
    .pipe(rename('custom.less.css'))
    // .pipe(sourcemaps.write('./stylesheets')) // we are not presently minifying
    .pipe(gulp.dest('./stylesheets'))
    .pipe(notify('Custom LESS compiled'));
});
