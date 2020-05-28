# Rundeck Vue UI

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

### Quick Start

* Start rundeck in Development mode
* Run `nvm use`
* Run `nvm run dev` to start building `packages/ui` in watch mode


### Working with UI Trellis

* Start rundeck in development mode
* Run `nvm use`
* Run `npm run dev:ui-trellis` and wait for initial build
* Run `npm run dev`