'use strict';

let gulp = require('gulp');
let sass = require('gulp-sass');
let size = require('gulp-size');
let notify = require('gulp-notify');
let concat = require('gulp-concat')
let rename = require('gulp-rename');
let sourcemaps = require('gulp-sourcemaps');

gulp.task('scss', function () {
  return gulp.src('./scss/app.scss')
    .pipe(sourcemaps.init())
    .pipe(sass({
      outputStyle: 'expanded'
    })).on('error', sass.logError)
    .pipe(sourcemaps.write())    
    .pipe(size())
    .pipe(rename('app.scss.css'))
    .pipe(gulp.dest('./stylesheets'))
    .pipe(notify('App SCSS compiled'));
});
