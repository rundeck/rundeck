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
./bin/deck test --suite selenium --url http://`hostname`
```

Run API tests against a newly provisioned cluster
```
./bin/deck test -s api --provision
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

### Configuration
The default settings in `config.yml` can be overriden in a `config.user.yml` file.
This can make it easier to run and develop tests against a custom location or setup.

For example, you may want to debug a selenium test against Rundeck running locally in dev
mode. By setting overriding the default `url` it will not be required to supply it at the
command line, and running the debug launch configuration will connect to the correct location.

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

### Debug
This project includes debug launch configurations for VSCode. `Debug and Watch` will run Jest
in watch mode and attach the debugger. The default test filter `noop` will prevent any tests
from running immmediately. Simply change the filter to your desired test(s) from the Jest watch
console when it is ready.