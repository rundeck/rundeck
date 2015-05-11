Rundeck
========

Rundeck is an open source automation service with a web console, 
command line tools and a WebAPI.
It lets you easily run automation tasks across a set of nodes.

For more information, mailing lists, IRC channel, visit <http://rundeck.org>

See the [Release Notes](RELEASE.md) for the latest version information.

To Build:
=====

Primary build is supported with gradle. More info in the [wiki](https://github.com/rundeck/rundeck/wiki/Building-and-Testing).

Gradle Build
=====

    ./gradlew build

Artifacts: 

* `rundeckapp/target/rundeck-X.Y.war`
* `rundeck-launcher/launcher/build/libs/rundeck-launcher-X.Y.jar`

Note: some pom.xml files exist, but the Maven build is not currently working.


Other builds
======

The documentation can be built with [pandoc](http://johnmacfarlane.net/pandoc/).
    
Build the documentation. Artifacts in `docs/en/dist`:

    cd docs
    make

You can build .rpm or .deb files (requires pandoc to build the docs):

Build the RPM. Artifacts in `packaging/rpmdist/RPMS/noarch/*.rpm`

    make rpm
    
Build the .deb. Artifacts in `packaging/*.deb`:

    make deb

To build clean:

    make clean

Installation
======

There are several install options: a self-contained jar file, or RPM, or Debian.

To start from the rundeck-launcher.jar, put it in a directory named ~/rundeck, then execute:

    java -XX:MaxPermSize=256m -Xmx1024m -Xms256m -server -jar rundeck-launcher-2.0.0.jar

If you'd like to install via RPM, you can use Yum:

    rpm -Uvh http://repo.rundeck.org/latest.rpm 
    yum install rundeck

OR install directly from RPM:

    rpm -ivh rundeck-2.0.0-xxx.rpm rundeck-config-2.0.0-xxx.rpm

Once the RPM is installed, execute:

    sudo /etc/init.d/rundeckd start

The server should launch on port 4440, with default username/password of `admin/admin`.

For Debian, download the .deb from the [downloads page](http://rundeck.org/downloads.html), then run:

    dpkg -i rundeck-2.0.0-x.deb

* For more info and configuration information, see the [Rundeck docs](http://docs.rundeck.org).

Requirements
=======

Java 7 (openjdk, sun)

[Pandoc](http://johnmacfarlane.net/pandoc/) (documentation build only)

Documentation
======

Available online at <http://rundeck.org/docs>

FAQ: <https://github.com/rundeck/rundeck/wiki/FAQ>

Development
======

Refer to the [IDE Development Environment](https://github.com/rundeck/rundeck/wiki/IDE-Development-Environment) to get set up using IntelliJ IDEA or Eclipse/STS.

* [Issue tracker](https://github.com/rundeck/rundeck/issues) at github.com
* [Fresh builds](http://build.rundeck.org) served by Jenkins

Do you have changes to contribute? Please see the [Development](https://github.com/rundeck/rundeck/wiki/Development) wiki page.

License
======

Copyright 2015 SimplifyOps

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

   http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
