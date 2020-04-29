Function Testing feat. Selenium
========
This test project is focused on functional black-box-ish testing of Rundeck
through the API and GUI.

## Getting started

### Installing dependencies
> Follow the dependency name links to navigate to installation instructions.

[**Node.js 12+**](https://github.com/creationix/nvm#install-script)  
It is highly recommended that node.js be installed and managed by nvm.
"Use" the project node version:
```
nvm use
```

[**Git LFS**](https://git-lfs.github.com/)  
Git LFS is required for storing and retrieving the image snapshots.

[**Chrome**](https://www.google.com/chrome/)  
Required for Selenium tests. Travis-CI is configured to include the latest stable.
When the envar ```CI=true``` the [example](./__tests__/selenium-login.test.ts) will run Chrome in headless mode.

### Bootstrap
```
./bin/deck bootstrap
```

Start rundeck:

to get rundeck running on loopback on Linux.
```bash
# From repo root
cd rundeckapp && ./gradlew bootRun
```

## Quick Start
Bootstrap:
```
./bin/deck bootstrap
```

Run selenium:
```
./bin/deck test --suite selenium
```

Run API tests against a provisioned cluster
```
./bin/deck test -s api --provision -u http://localhost
```

Run in watch mode:
```
./bin/deck test --suite selenium --watch
```

## Usage

### CLI
```
./bin/deck --help
```

### Test Suites

Tests are located under `src/__tests__/` and are organized hierarchically via
folder structure. Top level groups are `selenium` and `api`.

API testing can be used inside Selenium tests

### Selenium Webdriver Javascript
[Javescript API Documentation](https://selenium.dev/selenium/docs/api/javascript/module/selenium-webdriver/index_exports_WebDriver.html)

### Image Regresion Testing
> Image regression testing requires git lfs and docker.

The image regression testing always runs headless in a docker container; locally and on CI.
When not running headless the regression tests are NOOPs that return successful.