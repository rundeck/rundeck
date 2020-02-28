Selenium
========
> Much feared, little understood

This test sub-project utilizes `webdirverjs` from the Selenium project
to provide broad, high-level UI validation.

The goal is to validate common workflows and detect visual changes
and regressions.

## Getting started

### Installing dependencies
> Follow the dependency name links to navigate to installation instructions.


[**Node.js**](https://github.com/creationix/nvm#install-script)  
It is highly recommended that node.js be installed and managed by nvm. Project was developed with node ```>=8.11.0```.
```
nvm install 12
```

[**Git LFS**](https://git-lfs.github.com/)  
Git LFS is required for storing and retrieving the image snapshots.

[**Chrome**](https://www.google.com/chrome/)  
Required for selenium tests. Travis-CI is configured to include the latest stable.
When the envar ```CI=true``` the [example](./__tests__/selenium-login.test.ts) will run Chrome in headless mode.

Install node modules:  
```
npm install
npm install -g ts-node typescript
```

Start rundeck:

to get rundeck running on loopback on Linux.
```bash
# From repo root
cd rundeckapp && ./gradlew bootRun
```

## Quick Start
Run selenium:
```
npm run selenium
```

Run in watch mode:
```
npm run selenium:watch
```

Run in watch mode with debugging:
```
npm run selenium:debug:watch
```

Run  visual regression tests:
```
npm run selenium:viz
```

Update image snapshots:
```
npm run selenium:viz:update
```

## Usage

### CLI
The npm scripts wrap a CLI. You can access this CLI by running:
```
./bin/deck
```

Checkout the [npm scripts](./package.json) to see some usage examples.

### Selenium Webdriver Javascript
[Javescript API Documentation](https://selenium.dev/selenium/docs/api/javascript/module/selenium-webdriver/index_exports_WebDriver.html)

### Image Regresion Testing
> Image regression testing requires git lfs and docker.

The image regression testing always runs headless in a docker container; locally and on CI.
When not running headless the regression tests are NOOPs that return successful.