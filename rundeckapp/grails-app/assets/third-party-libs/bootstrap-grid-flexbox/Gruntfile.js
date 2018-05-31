/*!
 * Bootstrap Grid Flexbox Gruntfile
 * https://github.com/ngengs/bootstrap-grid-flexbox
 * Copyright 2016 Rizky Kharisma.
 * Licensed under MIT (https://github.com/ngengs/bootstrap-grid-flexbox/blob/master/LICENSE)
 */

module.exports = function (grunt) {
  'use strict';

  // Force use of Unix newlines
  grunt.util.linefeed = '\n';

  // Project configuration.
  grunt.initConfig({

    // Metadata.
    pkg: grunt.file.readJSON('package.json'),

    // Task configuration.
    clean: {
      dist: 'dist'
    },

    less: {
      compileCore: {
        options: {
          strictMath: true,
          sourceMap: false,
          outputSourceFiles: true,
          sourceMapURL: '<%= pkg.name %>.css.map',
          sourceMapFilename: 'dist/css/<%= pkg.name %>.css.map'
        },
        src: 'less/<%= pkg.name %>.less',
        dest: 'dist/css/<%= pkg.name %>.css'
      }
    },

    cssmin: {
      options: {
        // TODO: disable `zeroUnits` optimization once clean-css 3.2 is released
        //    and then simplify the fix for https://github.com/twbs/bootstrap/issues/14837 accordingly
        compatibility: 'ie8',
        keepSpecialComments: '*',
        sourceMap: true,
        sourceMapInlineSources: true,
        advanced: false
      },
      minifyCore: {
        src: 'dist/css/<%= pkg.name %>.css',
        dest: 'dist/css/<%= pkg.name %>.min.css'
      },
      minifyDocs: {
        src: 'docs/css/docs-style.css',
        dest: 'docs/css/docs-style.min.css'
      }
    },

    sass: {
      options: {
        precision: 6,
        outputStyle: 'expanded',
        sourceMap: true,
        sourceMapContents: true
      },
      compileCore: {
        src: 'sass/<%= pkg.name %>.scss',
        dest: 'dist/css/<%= pkg.name %>.css'
      },
      compileDocs: {
        src: 'sass/docs/docs.scss',
        dest: 'docs/css/docs-style.css'
      }
    },

    postcss: {
      options: {
        map: true,
        processors: [
          require('autoprefixer')({
            browsers: [
              'Android 2.3',
              'Android >= 4',
              'Chrome >= 20',
              'Firefox >= 24',
              'Explorer >= 8',
              'iOS >= 6',
              'Opera >= 12',
              'Safari >= 6'
            ]
          })
        ]
      },
      dist: {
        src: 'dist/css/<%= pkg.name %>.css'
      },
      docs: {
        src: 'docs/css/docs-style.css'
      }
    },

    csscomb: {
      options: {
        config: '.csscomb.json'
      },
      dist: {
        expand: true,
        cwd: 'dist/css/',
        src: ['*.css', '!*.min.css'],
        dest: 'dist/css/'
      },
      docs: {
        src: 'docs/css/docs-style.css',
        dest: 'docs/css/docs-style.css'
      }
    },

    csslint: {
      options: {
        csslintrc: '.csslintrc'
      },
      dist: [
        'dist/css/<%= pkg.name %>.css'
      ],
      docs: [
        'docs/css/docs-style.css'
      ]
    },

    copy: {
      docs:{
        expand: true,
        cwd: 'dist/css',
        src: ['*.min.css','*.min.css.map'],
        dest: 'docs/css',
        flatten: true,
        filter: 'isFile'
      }
    },

    htmllint: {
      options: {
        ignore: [
          'Attribute "autocomplete" not allowed on element "button" at this point.',
          'Attribute "autocomplete" is only allowed when the input type is "color", "date", "datetime", "datetime-local", "email", "hidden", "month", "number", "password", "range", "search", "tel", "text", "time", "url", or "week".',
          'Element "img" is missing required attribute "src".'
        ]
      },
      src: 'docs/*.html'
    },

    watch: {
      less: {
        files: 'less/**/*.less',
        tasks: 'less'
      },
      sass: {
        files: 'sass/**/*.scss',
        tasks: 'sass'
      }
    },

    exec: {
      npmUpdate: {
        command: 'npm update'
      }
    },

    compress: {
      main: {
        options: {
          archive: '<%= pkg.name %>-<%= pkg.version %>-dist.zip',
          mode: 'zip',
          level: 9,
          pretty: true
        },
        files: [
          {
            expand: true,
            cwd: 'dist/',
            src: ['**'],
            dest: '<%= pkg.name %>-<%= pkg.version %>-dist'
          }
        ]
      }
    }

  });


  // These plugins provide necessary tasks.
  require('load-grunt-tasks')(grunt);
  require('time-grunt')(grunt);


  // Watch File Changes
  grunt.loadNpmTasks('grunt-contrib-watch');

  // CSS distribution task.
  grunt.registerTask('less-compile', ['less:compileCore']);
  grunt.registerTask('sass-compile', ['sass:compileCore']);
  grunt.registerTask('dist-css', ['sass-compile', 'postcss:dist', 'csscomb:dist', 'cssmin:minifyCore']);

  // Full distribution task.
  grunt.registerTask('dist', ['clean:dist', 'dist-css']);

  // Documentation task
  grunt.registerTask('docs', ['sass:compileDocs', 'postcss:docs', 'csscomb:docs', 'cssmin:minifyDocs', 'copy:docs']);

  // Test task
  grunt.registerTask('test', ['dist', 'csslint:dist', 'docs', 'csslint:docs', 'htmllint']);

  // Default task.
  grunt.registerTask('default', ['dist']);

  grunt.registerTask('prep-release', ['dist', 'docs', 'compress']);
};
