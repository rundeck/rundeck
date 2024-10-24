<p align="center">
<a href="https://www.rundeck.com#gh-light-mode-only">
  <img src="https://www.rundeck.com/hubfs/Pager%20Duty%20Branding/RundeckbyPagerDuty.svg" alt="Rundeck" width="300"/>
</a>
<a href="https://www.rundeck.com#gh-dark-mode-only">
  <img src="https://www.rundeck.com/hubfs/Pager%20Duty%20Branding/RundeckbyPagerDutyDM.svg" alt="Rundeck" width="300"/>
</a>
</p>
<h3 align="center">Execute workflows across your existing automations<br /> or quickly automate previously manual procedures.</h3>

<br />
<p align="center">
<a href="https://github.com/rundeck/rundeck/"><img src="https://img.shields.io/github/stars/rundeck/rundeck?style=social" alt="GitHub Stars"></a>
<a href="https://github.com/rundeck/rundeck/releases/latest"><img src="https://img.shields.io/github/release/rundeck/rundeck.svg" alt="Latest release"></a>

<div align="center">

| Release                                                                                                                 | Deb                                           | RPM                                           | War                                           |
| ---------------------------------------------------------------------------------------------------------------------- | --------------------------------------------- | --------------------------------------------- | --------------------------------------------- |
| [![Release Build](https://img.shields.io/github/actions/workflow/status/rundeck/rundeck/release.yml)](https://github.com/rundeck/rundeck/actions/workflows/release.yml) | [Download](https://www.rundeck.com/downloads) | [Download](https://www.rundeck.com/downloads) | [Download](https://www.rundeck.com/downloads) |

</div>

<br />

Rundeck by PagerDuty is an open source runbook automation service with a web console, command line tools and a WebAPI. It lets you easily standardize tasks to improve operational quality by deploying automation across a set of nodes.

- [Visit the Website](https://www.rundeck.com)

- [Read the latest documentation](https://docs.rundeck.com/docs/)

- [Get help from the Community](https://community.pagerduty.com/ask-a-product-question-2)

- [Install Rundeck](https://docs.rundeck.com/docs/administration/install/installing-rundeck.html)

<br />

See the [Release Notes](https://docs.rundeck.com/docs/history/) for the latest version information.

<br />

# How To Build:

Primary build is supported with gradle. More info in the [wiki](https://github.com/rundeck/rundeck/wiki/Building-and-Testing).

Requirements: Java 11, NodeJs 18

## Build with Gradle

Produces: `rundeckapp/build/libs/rundeck-X.Y.war`

    ./gradlew build

## Docker Build

Uses the war artifact and produces a docker image.

Creates image `rundeck/rundeck:SNAPSHOT`, you can define `-PdockerTags` to add additional tags

    ./gradlew :docker:officialBuild

<br />

# Documentation

Available online at <https://docs.rundeck.com/docs>

FAQ: <https://github.com/rundeck/rundeck/wiki/FAQ>

<br />

# Development

Refer to the [IDE Development Environment](https://github.com/rundeck/rundeck/wiki/IDE-Development-Environment) to get set up using IntelliJ IDEA or Eclipse/STS.

- [Issue tracker](https://github.com/rundeck/rundeck/issues) at github.com

Do you have changes to contribute? Please see the [Development](https://github.com/rundeck/rundeck/wiki/Development) wiki page.

<br />

# License

Copyright 2024 PagerDuty, Inc.

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
