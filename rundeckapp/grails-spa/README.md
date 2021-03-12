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

### Quick Start

* Start rundeck in Development mode
* Run `nvm use`
* Run `npm run dev` to start building `packages/ui` in watch mode


### Working with UI Trellis

* Start rundeck in development mode
* Run `nvm use`
* Run `npm run dev:all` and wait for initial build


## UI Trellis Storybook Development

### Setup

* In the `rundeck-runtime/server/config/rundeck-config.properties` file
add `grails.cors.enabled=true` to allow Storybook access to the API.
* Start Rundeck in development mode.
* Enter the `./packages/ui-trellis` directory; this will be your workdir.
* Copy `.env.dist` to `.env` and replace the values to match your environment:  

`STORYBOOK_RUNDECK_TOKEN=`: A Rundeck API token for your development target  
`STORYBOOK_RUNDECK_URL=`: The base URL for your Rundeck development target

* Run `npm run storybook:next`
* Develop and get realtime feedback in Storybook