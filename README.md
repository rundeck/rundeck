RunDeck
========

RunDeck is a command dispatcher with a modern web console.
It lets you easily run commands across a set of nodes.

For more information, mailing lists, IRC channel, visit <http://rundeck.org>

To Build:
=====

    make

Creates the rundeck-launcher.jar

Build the RPM:

    make rpm

To build clean:

    make clean

Installation
======

There are two install options, a self-contained jar file, or RPM.

To start from the rundeck-launcher.jar, put it in a directory named ~/rundeck, then execute:

    java -jar rundeck-launcher-1.1.0.jar

If you'd like to install via RPM, you can use Yum:

    rpm -Uvh http://rundeck.org/latest.rpm
    yum install rundeck

OR install directly from RPM:

    rpm -ivh rundeck-1.1.0-xxx.rpm

Once the RPM is installed, execute:

    sudo /etc/init.d/rundeckd start

The server should launch on port 4440, with default username/password of `admin/admin`.

* For more info and configuration information, see the [RunDeck Guide](http://rundeck.org/docs/RunDeck-Guide.html)

Requirements
=======

Java 5 (openjdk, sun)

To build docs (required for rpm, not required for launcher):

[pandoc](http://johnmacfarlane.net/pandoc/)

*Note, to build docs for launcher, do `make clean docs`, then `make`*

Documentation
======

Available online at <http://rundeck.org/docs>

Development
======

* [Issue tracker](http://rundeck.lighthouseapp.com/projects/59277-development) at Lighthouseapp.com
* [Fresh builds](http://build.rundeck.org) served by Jenkins

Do you have changes to contribute? Please see the [Development](https://github.com/dtolabs/rundeck/wiki/Development) wiki page.

License
======

Copyright 2011 DTO Solutions

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

   http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
