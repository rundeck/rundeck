% Installing Plugins

## Installation
Installation of plugins is simple:

Put the plugin file, such as `plugin.jar` or `some-plugin.zip`, into the Rundeck 
server's libext dir:

    cp some-plugin.zip $RDECK_BASE/libext

The plugin is now enabled, and any providers it defines can be used by nodes
or projects.

The Rundeck server does not have to be restarted.

## Uninstalling or Updating 

You can simply remove the plugin files from `$RDECK_BASE/libext` to uninstall
them.

You can overwrite an old plugin with a newer version to update it.




## Included Plugins

Several plugin files are included with the default Rundeck installation for your use in testing or development. 

* [Stub plugin](script-plugins.html#stub-plugin): simply prints the command or script instead of running it.
* [Script plugin](script-plugins.html#script-plugins): executes an external script file to perform the command, useful for developing your own plugin with the [Script Plugin Development](../developer/plugin-development.html#script-plugin-development) model.

Plugins for Rundeck contain new Providers for some of the Services used by
the Rundeck core.

Rundeck comes with some Built-in plugins for these services, but Plugins
let you write your own, or use third-party implementations.
