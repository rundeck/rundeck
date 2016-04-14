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

See [Bundled Plugins](bundled-plugins.html).
