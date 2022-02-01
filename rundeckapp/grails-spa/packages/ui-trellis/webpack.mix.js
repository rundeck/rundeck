let path = require('path')
const walk = require('walk')

let mix = require('laravel-mix');
const nodeExternals = require('webpack-node-externals');

mix
  .sass('./theme/scss/app.scss', 'style')
  .ts('./src/components/containers/copybox/CopyBox.vue', './dist/src/components/containers/copybox/CopyBox.vue.js')
  .ts('./src/components/containers/drawer/Drawer.vue', './dist/src/components/containers/drawer/Drawer.vue.js')
  .ts('./src/components/containers/tabs/index.ts', './dist/src/components/containers/tabs/index.js')
  .ts('./src/components/execution-log/interfaces/ExecutionData.ts', './dist/src/components/execution-log/interfaces/ExecutionData.js')
  .ts('./src/components/execution-log/logBuilder.ts', './dist/src/components/execution-log/logBuilder.js')
  .ts('./src/components/execution-log/logEntry.vue', './dist/src/components/execution-log/logEntry.vue.js')
  .ts('./src/components/execution-log/logEntryFlex.vue', './dist/src/components/execution-log/logEntryFlex.vue.js')
  .ts('./src/components/execution-log/logViewer.vue', './dist/src/components/execution-log/logViewer.vue.js')

  .ts('./src/components/filter-list/FilterList.vue', './dist/src/components/filter-list/FilterList.vue.js')
  .ts('./src/components/first-run/FirstRun.vue', './dist/src/components/first-run/FirstRun.vue.js')
  .ts('./src/components/inputs/Switch.vue', './dist/src/components/inputs/Switch.vue.js')

  .ts('./src/components/navbar/NavBar.vue', './dist/src/components/navbar/NavBar.vue.js')
  .ts('./src/components/navbar/NavBarContainer.vue', './dist/src/components/navbar/NavBarContainer.vue.js')
  .ts('./src/components/navbar/NavBarDrawer.vue', './dist/src/components/navbar/NavBarDrawer.vue.js')
  .ts('./src/components/navbar/NavBarItem.vue', './dist/src/components/navbar/NavBarItem.vue.js')

  .ts('./src/components/plugins/DynamicFormPluginProp.vue', './dist/src/components/plugins/DynamicFormPluginProp.vue.js')
  .ts('./src/components/plugins/JobConfigPicker.vue', './dist/src/components/plugins/JobConfigPicker.vue.js')
  .ts('./src/components/plugins/KeyStorageSelector.vue', './dist/src/components/plugins/KeyStorageSelector.vue.js')
  .ts('./src/components/plugins/pluginConfig.vue', './dist/src/components/plugins/pluginConfig.vue.js')
  .ts('./src/components/plugins/pluginInfo.vue', './dist/src/components/plugins/pluginInfo.vue.js')
  .ts('./src/components/plugins/pluginPropEdit.vue', './dist/src/components/plugins/pluginPropEdit.vue.js')
  .ts('./src/components/plugins/pluginPropVal.vue', './dist/src/components/plugins/pluginPropVal.vue.js')
  .ts('./src/components/plugins/pluginPropView.vue', './dist/src/components/plugins/pluginPropView.vue.js')
  .ts('./src/components/plugins/ProjectPicker.vue', './dist/src/components/plugins/ProjectPicker.vue.js')

  .ts('./src/components/skeleton/Skeleton.vue', './dist/src/components/skeleton/Skeleton.vue.js')
  .ts('./src/components/svg/RundeckLogo.vue', './dist/src/components/svg/RundeckLogo.vue.js')

  .ts('./src/components/utility-bar/Popper.vue', './dist/src/components/utility-bar/Popper.vue.js')
  .ts('./src/components/utility-bar/UtilityActionItem.vue', './dist/src/components/utility-bar/UtilityActionItem.vue.js')
  .ts('./src/components/utility-bar/UtilityBar.vue', './dist/src/components/utility-bar/UtilityBar.vue.js')
  .ts('./src/components/utility-bar/UtilityBarItem.vue', './dist/src/components/utility-bar/UtilityBarItem.vue.js')
  .ts('./src/components/utility-bar/UtilityWidgetItem.vue', './dist/src/components/utility-bar/UtilityWidgetItem.vue.js')

  .ts('./src/components/utils/AceEditor.vue', './dist/src/components/utils/AceEditor.vue.js')
  .ts('./src/components/utils/AceEditorVue.ts', './dist/src/components/utils/AceEditorVue.js')
  .ts('./src/components/utils/Expandable.vue', './dist/src/components/utils/Expandable.vue.js')
  .ts('./src/components/utils/ExtendedDescription.vue', './dist/src/components/utils/ExtendedDescription.vue.js')
  .ts('./src/components/utils/OffsetPagination.vue', './dist/src/components/utils/OffsetPagination.vue.js')
  .ts('./src/components/utils/PageConfirm.vue', './dist/src/components/utils/PageConfirm.vue.js')
  .ts('./src/components/utils/Pagination.vue', './dist/src/components/utils/Pagination.vue.js')

  .ts('./src/components/version/Copyright.vue', './dist/src/components/version/Copyright.vue.js')
  .ts('./src/components/version/RundeckVersionDisplay.vue', './dist/src/components/version/RundeckVersionDisplay.vue.js')
  .ts('./src/components/version/ServerDisplay.vue', './dist/src/components/version/ServerDisplay.vue.js')
  .ts('./src/components/version/VersionDateDisplay.vue', './dist/src/components/version/VersionDateDisplay.vue.js')
  .ts('./src/components/version/VersionDisplay.vue', './dist/src/components/version/VersionDisplay.vue.js')
  .ts('./src/components/version/VersionIconNameDisplay.vue', './dist/src/components/version/VersionIconNameDisplay.vue.js')

  .ts('./src/components/widgets/news/News.vue', './dist/src/components/widgets/news/News.vue.js')

  .ts('./src/components/widgets/project-select/ProjectSelect.vue', './dist/src/components/widgets/project-select/ProjectSelect.vue.js')
  .ts('./src/components/widgets/project-select/ProjectSelectButton.vue', './dist/src/components/widgets/project-select/ProjectSelectButton.vue.js')

  .ts('./src/components/widgets/rundeck-info/RundeckInfo.vue', './dist/src/components/widgets/rundeck-info/RundeckInfo.vue.js')
  .ts('./src/components/widgets/rundeck-info/RundeckInfoWidget.vue', './dist/src/components/widgets/rundeck-info/RundeckInfoWidget.vue.js')
  .ts('./src/components/widgets/theme-select/ThemeSelect.vue', './dist/src/components/components/widgets/theme-select/ThemeSelect.vue.js')

  .ts('./src/components/widgets/webhook-select/WebhookSelect.vue', './dist/src/components/widgets/webhook-select/WebhookSelect.vue.js')

  .ts('./src/interfaces/AppLinks.ts', './dist/src/interfaces/AppLinks.js')
  .ts('./src/interfaces/JobReference.ts', './dist/src/interfaces/JobReference.js')
  .ts('./src/interfaces/PluginValidation.ts', './dist/src/interfaces/PluginValidation.js')
  .ts('./src/interfaces/rundeckWindow.ts', './dist/src/interfaces/rundeckWindow.js')

  .ts('./src/modules/filterPrefs.ts', './dist/src/modules/filterPrefs.js')
  .ts('./src/modules/generators.ts', './dist/src/modules/generators.js')
  .ts('./src/modules/InputUtils.ts', './dist/src/modules/InputUtils.js')
  .ts('./src/modules/pluginService.ts', './dist/src/modules/pluginService.js')
  .ts('./src/modules/requests.ts', './dist/src/modules/requests.js')
  .ts('./src/modules/rundeckCLient.ts', './dist/src/modules/rundeckCLient.js')
  .ts('./src/modules/tokens.ts', './dist/src/modules/tokens.js')

  .ts('./src/stores/ExecutionOutput.ts', './dist/src/stores/ExecutionOutput.js')
  .ts('./src/stores/NavBar.ts', './dist/src/stores/NavBar.js')
  .ts('./src/stores/News.ts', './dist/src/stores/News.js')
  .ts('./src/stores/Plugins.ts', './dist/src/stores/Plugins.js')
  .ts('./src/stores/Projects.ts', './dist/src/stores/Projects.js')
  .ts('./src/stores/Releases.ts', './dist/src/stores/Releases.js')
  .ts('./src/stores/RootStore.ts', './dist/src/stores/RootStore.js')
  .ts('./src/stores/System.ts', './dist/src/stores/System.js')
  .ts('./src/stores/Tabs.ts', './dist/src/stores/Tabs.js')
  .ts('./src/stores/Theme.ts', './dist/src/stores/Theme.js')
  .ts('./src/stores/UtilityBar.ts', './dist/src/stores/UtilityBar.js')
  .ts('./src/stores/Webhooks.ts', './dist/src/stores/Webhooks.js')
  .ts('./src/stores/Workflow.ts', './dist/src/stores/Workflow.js')

  .ts('./src/types/JobTree.ts', './dist/src/types/JobTree.js')
  .ts('./src/types/TreeItem.ts', './dist/src/types/TreeItem.js')

  .ts('./src/utilities/Async.ts', './dist/src/utilities/Async.js')
  .ts('./src/utilities/Clipboard.ts', './dist/src/utilities/Clipboard.js')
  .ts('./src/utilities/component.ts', './dist/src/utilities/component.js')
  .ts('./src/utilities/ExecutionLogConsumer.ts', './dist/src/utilities/ExecutionLogConsumer.js')
  .ts('./src/utilities/JobWorkflow.ts', './dist/src/utilities/JobWorkflow.js')
  .ts('./src/utilities/RundeckVersion.ts', './dist/src/utilities/RundeckVersion.js')
  .ts('./src/utilities/uivi18n.ts', './dist/src/utilities/uivi18n.js')
  .ts('./src/utilities/vueEventBus.ts', './dist/src/utilities/vueEventBus.js')
  .js('./src/utilities/xhrRequests.js', './dist/src/utilities/xhrRequests.js')

  .ts('./src/index.ts', './dist/src')
  .ts('./src/rundeckService.ts', './dist/src')
  .options({
    processCssUrls: false,
    terser: {
      extractComments: false,
    }
  })
  .copyDirectory('./dist/src', './lib')
  .vue()
  .setPublicPath('./dist')
  .webpackConfig({
    externals: [
      nodeExternals(),
      function (context, request, callback) {
       
        if (/^\..*\.vue$/.test(request)) // Components requiring other components
          return callback(null, request)

        /** Inline CSS */
        if (context.startsWith(process.cwd())
          && request.includes('.scss')) {
          return callback()
        }

        /** Bundle javascript code inside components */
        if (request.startsWith('./')
          && !context.endsWith('.scss')
          && !context.includes('node_modules')
          && !request.includes('.vue')) {
          return callback(null, request)
        }

        if (request.startsWith('.')
            && !request.includes('?') // These are typically compile time generated files in flight
            && !context.includes('node_modules') // Runtime stuff still getting required from node_modules
            && !request.includes('node_modules')) {
          return callback(null, request)
        }

        callback()
      }
    ],
    optimization: {
      minimize: false,
      splitChunks: false
    },
    module: {
      rules: [
          {
              test: /\.ts$/,
              loader: 'ts-loader',
              options: {
                transpileOnly: false,
                onlyCompileBundledFiles: false,
                configFile : 'tsconfig.webpack.json',
                compilerOptions : {
                  declarationDir: './dist',
                }
              }
          }
      ]
    },
    output: {
      library: 'rundeckUiTrellis',
      libraryTarget: 'commonjs2',
      // filename: (x) => {
      //   return '[name].js'
      // },
    },
  })

// mixGlob
//   .ts(['./src/**/*.vue','./src/**/*.ts'], './lib', null, {
//       base: './lib'
//   })
