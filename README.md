Rundeck Step Disabler - PD Hackweek '23 Project
========

## Technical Debt To Date

1. Fix the log output in EngineWorkflowExecutor.logSkippedOperations to be more accurate about the skip cause.
2. Externalize CSS on the step dashboards

## Tests To Assess Basic Functionality

1. Basic enable/disable jobs
2. Export to file
3. Import from file
4. Export to another instance
5. Export from older than disabler version
6. SCM export from older than disabler version

by:

* Rodrigo Navarro ([@ronaveva](https://github.com/ronaveva))
* Darwis Narvaez ([@darwisnarvaezdev](https://github.com/darwisnarvaezdev))
* Jesús Osuna ([@jesus-osuna-m](https://github.com/jesus-osuna-m))
* Antony Velásquez ([@avelasquezr](https://github.com/avelasquezr))
* Reiner Acuña ([@MegaDrive68k](https://github.com/MegaDrive68k))

Abstract
========

When creating a Rundeck workflow job, we frequently find ourselves needing to add and test specifics steps. Since you often want to explore multiple approaches to completing the same step, it would be nice to maintain the step that has already been completed without fully losing it.

The ability to enable and disable steps would speed up the process of building jobs on the fly.

Objective
========

Add the ability to disable steps (without deleting them) in Rundeck OSS.

Rundeck
========

| Travis | Deb | RPM | War |
|--------|-----|-----|-----|
|[![Travis CI](https://travis-ci.org/rundeck/rundeck.svg?branch=master)](https://travis-ci.org/rundeck/rundeck/builds#)|[Download](https://www.rundeck.com/downloads)|[Download](https://www.rundeck.com/downloads)|[Download](https://www.rundeck.com/downloads)|

Rundeck is an open source automation service with a web console,
command line tools and a WebAPI.
It lets you easily run automation tasks across a set of nodes.

* Site: <https://www.rundeck.com>

* Latest documentation: <https://docs.rundeck.com/docs/>

* Get Help: <https://docs.rundeck.com/docs/introduction/getting-help.html>

* Installation: <https://docs.rundeck.com/docs/administration/install/installing-rundeck.html>


See the [Release Notes](https://docs.rundeck.com/docs/history/) for the latest version information.


How To Build:
=====

Primary build is supported with gradle. More info in the [wiki](https://github.com/rundeck/rundeck/wiki/Building-and-Testing).

Requirements: Java 1.8, NodeJs 16

Build with Gradle
---

Produces: `rundeckapp/build/libs/rundeck-X.Y.war`

    ./gradlew build

Docker Build
---

Uses the war artifact and produces a docker image.

Creates image `rundeck/rundeck:SNAPSHOT`, you can define `-PdockerTags` to add additional tags

    ./gradlew :docker:officialBuild

Documentation
======

Available online at <https://docs.rundeck.com/docs>

FAQ: <https://github.com/rundeck/rundeck/wiki/FAQ>

Development
======

Refer to the [IDE Development Environment](https://github.com/rundeck/rundeck/wiki/IDE-Development-Environment) to get set up using IntelliJ IDEA or Eclipse/STS.

* [Issue tracker](https://github.com/rundeck/rundeck/issues) at github.com

Do you have changes to contribute? Please see the [Development](https://github.com/rundeck/rundeck/wiki/Development) wiki page.

License
======

Copyright 2023 Rundeck, Inc.

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

   http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
