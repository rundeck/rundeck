# Bootstrap Grid Flexbox [![Build Status](https://travis-ci.org/ngengs/bootstrap-grid-flexbox.svg?branch=master)](https://travis-ci.org/ngengs/bootstrap-grid-flexbox)
Plugins flexbox grid system for bootstrap 3.

### Story Behind
Before that when I developing a website with [Bootstrap](http://getbootstrap.com) sometimes I get crazy because the difference height of grid can destroying my layout, so I want to moving to use Flexbox because most modern browser now can support Flexbox ([check here](http://caniuse.com/#feat=flexbox)) .

I choose to style the grid not to overide Bootstrap grid but just for add on to Bootstrap or as Plugins. Why?
Sometimes I still need bootstrap grid, so i chose to not replace bootstrap grid.

## Quick start
Several quick start options are available:

* [Download the latest release](https://github.com/ngengs/bootstrap-grid-flexbox/releases).
* Clone the repo: `git clone --recursive https://github.com/ngengs/bootstrap-grid-flexbox.git`.


### What's included

Within the download you'll find the following directories and files, logically grouping common assets and providing both compiled and minified variations. You'll see something like this:

```
bootstrap-grid-flexbox/
├──  dist/
│   └── css/
│         ├── bootstrap-grid-flexbox.css
│         └── bootstrap-grid-flexbox.min.css
├── less/
│   ├── bootstrap-grid-flexbox.less
│   ├── grid.less
│   ├── mixins.less
│   └── variables.less
└── sass/
    ├── bootstrap-grid-flexbox.scss
    ├── _grid.scss
    ├── _mixins.scss
    └── _variables.scss
```

The **dist** directory is for compiled css, and you can use that, but if you want build you can use file in **less** directory and compile the **[bootstrap-grid-flexbox.less](less/bootstrap-grid-flexbox.less)** or **sass** directory and compile the **[bootstrap-grid-flexbox.scss](sass/bootstrap-grid-flexbox.scss)**


## Bugs and feature requests

Have a bug or a feature request? Please first search for existing and closed issues. If your problem or idea is not addressed yet, [please open a new issue](https://github.com/ngengs/bootstrap-grid-flexbox/issues/new).

## Documentation
The documentation is not ready yet.

## Usage
Add the css after the Bootstrap css
```
<head>
    <!-- Bootstrap -->
    <link href="css/bootstrap.min.css" rel="stylesheet">
    
    <!-- Bootstrap Grid Flexbox -->
    <link href="css/bootstrap-grid-flexbox.min.css" rel="stylesheet">
</head>
```
After that basically you just can use the class for **row** and **col**.

**Is this just for bootstrap? Hell No.** You can still using the grid without bootstrap. But you still need create your own container

Basically using the grid and row is the same as bootstrap at [here](http://getbootstrap.com/css/#grid), you can use ```offset``` ```pull``` ```push``` and responsive grid like ```xs``` ```sm``` ```md``` ```lg``` at [here](http://getbootstrap.com/css/#grid-options).

But I replace ```row``` and ```col``` with my class preference to avoid overide bootstrap row and grid.
To use you just replace the ```.row``` with ```.row-flex``` and ```.col-``` with ```.col-flex-```

**Example:**
```
<div class="row-flex">
    <div class="col-flex-md-12">.col-flex-md-12</div>
    <div class="col-flex-md-2">.col-flex-md-2</div>
    <div class="col-flex-md-10">.col-flex-md-10</div>
    <div class="col-flex-md-4">col-flex-md-4</div>
    <div class="col-flex-md-8">.col-flex-md-8</div>
    <div class="col-flex-md-6">.col-flex-md-6</div>
    <div class="col-flex-md-6">.col-flex-md-6</div>
</div>
```

**Offset:**
```
<div class="row-flex">
    <div class="col-flex-md-2 col-flex-md-offset-10">.col-flex-md-2</div> 
    <div class="col-flex-md-4 col-flex-md-offset-8">.col-flex-md-4</div> 
    <div class="col-flex-md-6 col-flex-md-offset-6">.col-flex-md-6</div> 
    <div class="col-flex-md-8 col-flex-md-offset-4">.col-flex-md-8</div> 
    <div class="col-flex-md-10 col-flex-md-offset-2">.col-flex-md-10</div> 
    <div class="col-flex-md-12 col-flex-md-offset-0">.col-flex-md-12</div> 
</div>
```

**Nested:**
```
<div class="row-flex"> 
  <div class="col-flex-md-12"> .col-flex-md-12 
    <div class="row-flex"> 
      <div class="col-flex-md-2">.col-flex-md-2</div> 
      <div class="col-flex-md-10"> .col-flex-md-10 
        <div class="row-flex"> 
          <div class="col-flex-md-6">.col-flex-md-6</div> 
          <div class="col-flex-md-6">.col-flex-md-6</div> 
        </div> 
      </div> 
    </div>
  </div> 
</div>
```

### Improvement
Using flexbox we can improve the grid system.
#### Aligment Grid
I add classes to align elements to the start or end of a row as well as the top, bottom, or center of a column.
And it be like this.

**.start-**
```
<div class="row-flex start-xs">
    <div class="col-flex-xs-6">
        .col-flex-xs-6
    </div>
</div>
```

**.center-**
```
<div class="row-flex center-xs">
    <div class="col-flex-xs-6">
        .col-flex-xs-6
    </div>
</div>
```

**.end-**
```
<div class="row-flex end-xs">
    <div class="col-flex-xs-6">
        .col-flex-xs-6
    </div>
</div>
```

**.top-**
```
<div class="row-flex top-xs">
    <div class="col-flex-xs-6">
        .col-flex-xs-6<br>.col-flex-xs-6
    </div>
    <div class="col-flex-xs-6">
        .col-flex-xs-6
    </div>
</div>
```

**.middle-**
```
<div class="row-flex middle-xs">
    <div class="col-flex-xs-6">
        .col-flex-xs-6<br>.col-flex-xs-6
    </div>
    <div class="col-flex-xs-6">
        .col-flex-xs-6
    </div>
</div>
```

**.bottom-**
```
<div class="row-flex bottom-xs">
    <div class="col-flex-xs-6">
        .col-flex-xs-6<br>.col-flex-xs-6
    </div>
    <div class="col-flex-xs-6">
        .col-flex-xs-6
    </div>
</div>
```

As you see you can add ``xs`` ``md`` ``lg`` to the aligment

#### Reverse Grid
You can reverse the order of grid

**.reverse**
```
<div class="row-flex reverse">
    <div class="col-flex-xs-4">
        1
    </div>
    <div class="col-flex-xs-4">
        2
    </div>
    <div class="col-flex-xs-4">
        3
    </div>
</div>
```
And because the reverse the grid order will be **3** - **2** - **1**. The ```.reverse``` class must use with the ```.row-flex```.

## BUILD
I use Grunt to build
1. Install Node.js [download](https://nodejs.org/en/)
2. Install Gruntjs

    ```
    npm install -g grunt-cli
    ```
3. Install package

   ```
   npm install
   ```

4. Run build process

    ```
    grunt dist
    ```


## Inspired
- [Bootstrap](http://getbootstrap.com)
- [Flexboxgird](http://flexboxgrid.com/)

## Creators
**Rizky Kharisma**

* <https://twitter.com/ngengs>
* <https://github.com/ngengs>
* <http://ngengs.com>

## License
Code release under [the MIT license](LICENSE)
