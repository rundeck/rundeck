% UI Plugins
% Greg Schueler
% December 4, 2017


## About

UI Plugins provide a way to include Javascript and CSS files
in various parts of the Rundeck GUI's HTML pages.
You can define which pages of the GUI should include your plugin files,
and use the Javascript and CSS to modify or add new features to the GUI. 

UI Plugins that are installed are loaded on all applicable pages by default.

UI Plugins can either be packaged as Zip files or Java Jar files.
The simplest is to use a Zip plugin type.

Your Javascript can make use if a simple [Javascript API](#javascript-api) for additional features.

## Behavior

When Rundeck renders a page, it evaluates whether any UI plugins are applicable, and
what resources of those plugins to load for the page. If some plugins declare that they `require`
other plugins, the ordering is arranged to load required plugins first, if possible. Note: any plugin that you put in the `requires` section must also be configured to load on the
same pages, otherwise Rundeck will not load it.

For Zip UI plugins Rundeck looks at the `plugin.yaml` data which declares the page configurations to determine applicability. For Java plugins, Rundeck calls the `doesApply` method.

When loading a plugin for the page, Rundeck will link to the script and stylesheets for the
page.  For Zip UI Plugins, the Page configuration determines which scripts and stylesheets
to load.  For Java plugins, the `scriptResourcesForPath` and `styleResourcesForPath` methods
will be called.

This is the list of available pages which can load UI plugins:

* `menu/jobs`,
* `menu/home`
* `menu/projectHome`
* `menu/executionMode`
* `menu/projectExport`
* `menu/projectImport`
* `menu/projectDelete`
* `menu/projectAcls`
* `menu/editProjectAclFile`
* `menu/createProjectAclFile`
* `menu/saveProjectAclFile`
* `menu/logStorage`
* `menu/securityConfig`
* `menu/acls`
* `menu/editSystemAclFile`
* `menu/createSystemAclFile`
* `menu/saveSystemAclFile`
* `menu/systemInfo`
* `menu/systemConfig`
* `menu/metrics`
* `menu/plugins`
* `menu/welcome`
* `menu/storage`
* `scheduledExecution/show`
* `scheduledExecution/edit`
* `scheduledExecution/delete`
* `scheduledExecution/create`
* `execution/show`
* `framework/nodes`
* `framework/adhoc`
* `framework/createProject`
* `framework/editProject`
* `framework/editProjectConfig`
* `framework/editProjectFile`
* `scm/index`
* `reports/index`

## Zip Plugin Type

See the [Plugin Development - Script plugin zip structure](plugin-development.html#script-plugin-zip-structure)
for the basics of Zip plugin structure.

The structure of the zip file is:

    [name]-plugin.zip
    \- [name]-plugin/ -- root directory of zip contents, same name as zip file
       |- plugin.yaml -- plugin metadata file
       \- resources/  -- i18n resources, icons, (and for UI plugins: scripts/css)
       	  |- icon.png
          |- i18n/
          |  |- messages.properties
          |  |- messages_es_419.properties
          |  \- ...
          |- js/
          |  |- myfile.js
          |  \- ...
          \- css/
             |- mystyles.css
             \- ...

### UI Plugin Provider Declaration

The following is required in the `providers:` section of the `plugin.yaml`:

~~~~~~~ {.yaml}
# yaml plugin metadata

name: plugin name
version: plugin version
rundeckPluginVersion: 1.2
author: author name
date: release date (ISO8601)
url: website URL
providers:
    - service: UI
      name: my-provider-id
      plugin-type: ui
      title: "My Plugin Title"
      description: "My Plugin Description"
      ui:
        - pages: '*'
          scripts:
            - js/my-init.js
            - js/lib/some-dependency.js
            - js/lib/my-main.js
          requires: 'some-other-provider'
        - pages: ['some/path']
          styles: 
          	- css/mystyles.css
~~~~~~~~~~~~

For a UI plugin, define a `service: UI` and a `plugin-type: ui`.
(The `providers` section can also define other providers, of any type.)

For `name:` use a unique provider ID for your plugin.

Within the `ui:` section is a list of UI Plugin Page Configurations.

UI Plugin Page Configurations consist of:

* `pages:` A list of applicable Page paths. This can be a single path string, a list of paths, or a `*` which will match all paths.
* `scripts:` A single file or list of files.  These are relative to the `resources` directory in your zip file.
* `styles:` A single file, or a list of files.  Thes are relative to the `resources` directory in your zip file.
* `requires:` A single or list of other UI plugin provider IDs.

See [Javascript API](#javascript-api) for additional Javascript information.

### Localization

For the basics of zip plugin localization see: [Plugin Development - Internationalization/Localization for Zip files][].

### Icon

See [Plugin Icons][].

## Java Plugin Type

* *Note*: Refer to [Java Development](plugin-development.html#java-plugin-development) for information about developing a Java plugin for Rundeck.

The plugin interface is [UIPlugin][].

~~~{.java}

public interface UIPlugin {
    /**
     * @param path
     *
     * @return true if this plugin applies at the path
     */
    boolean doesApply(String path);

    /**
     *
     * @param path
     * @return list of resources available at the path
     */
    List<String> resourcesForPath(String path);

    /**
     * @param path
     *
     * @return list of javascript resources to load at the path
     */
    List<String> scriptResourcesForPath(String path);

    /**
     * @param path
     *
     * @return list of css stylesheets to load at the path
     */
    List<String> styleResourcesForPath(String path);

    /**
     * @param path
     *
     * @return list of plugin names this plugin depends on for the specified path
     */
    List<String> requires(String path);
}

~~~

The methods of the plugin are used as follows:

* `doesApply`: should return `true`, if the plugin applies to the given path
* `resourcesForPath`: return the list of resources for the given path
* `scriptResourcesForPath`: return the list of Javascript resources for the path
* `styleResourcesForPath`: return the list of CSS resources for the path
* `requires`: return a list of other UI plugin names which this plugin requires, used to order the loading of plugin resources.

Resources should be included in your plugin Jar file under a `resources/` directory. 

### Localization

For the basics of Java plugin localization see: [Plugin Development - Plugin Localization][].

### Icon

See [Plugin Icons][].

## Javascript API

When loaded in a Rundeck GUI page, your Javascript code can use a simple Javascript API to 
get more information about the Rundeck application, and your plugin.

Note: Rundeck makes use of [Knockout][] and [jQuery][] on all GUI pages, so they can be used by your plugins. Knockout is useful to understand when interacting with the JS already included on a Rundeck page.

Rundeck creates a window object called `rundeckPage` with these methods:

* `project()`: returns the name of the current project, if available
* `path()`: the page path
* `lang()`: current user locale and language code
* `pluginBaseUrl(pluginId)`: returns the base URL for loading file resources for a plugin with provider ID "pluginId". Append a resources path to retrieve any plugin resource files.
* `pluginBasei18nUrl(pluginId)`: returns the base URL for loading i18n resources for a plugin with provider ID "pluginId". Append a resources path to retrieve i18n resources.

Note: the `rundeckPage` object may have other methods, but any methods not documented here are subject to change.


### Loading resources

You can load other resources from your plugin by using the `pluginBaseUrl` for your plugin. 

Example using jQuery:

~~~{.js}
function loadHtmlTemplate(file){
	//assuming my zip plugin has a resources/html/myfile.html
	var myProvider='com.mycompany.rundeck.myplugin';
	var pluginUrl = rundeckPage.pluginBaseUrl(myProvider);
    var fullUrl = pluginUrl + '/html/' + file + ".html";
    jQuery.get(fullUrl, function (text) {
    	//do something with the HTML contents         
    });
}
~~~

### Loading i18n Resources

The `rundeckPage.pluginBasei18nUrl(..)` method will return the base URL for loading i18n resources.

Rundeck Plugin Localization/Internationalization uses java `.properties` formatted files. (See [Plugin Localization][Localization]). However, your i18n resources don't have to be `.properties` files.

Requesting resources via this URL provides two features to help with i18n:

1. Locale resolution.  Requesting a path such as `rundeckPage.pluginBasei18nUrl('myprovider')+'/myfile.txt'`, will attempt to resolve the file by looking for a file based on the current User's locale/language settings.  E.g. if their language is set to Spanish (code `es_419`), the request will resolve to a file `i18n/myfile_es_419.txt` if it exists. It will fall back to the language (e.g. `es`), then any default locale (e.g. `en_us`), default language (e.g. `en`) and finally the original file path.
2. Conversion of Java .properties to JSON.  If you request a `.properties` file, and append a `?format=json` to the URL, Rundeck will load the Java Properties formatted data, and return the JSON for the data.

Examples:

Loads plugin i18n messages into the `window.Messages` object.
If my zip plugin has a file `resources/i18n/messages_es_419.properties`
and user's current lang is `es_419`, this would load the Spanish messages:

~~~{.js}
function loadi18nMessages(file){
	var myProvider='com.mycompany.rundeck.myplugin';
	var plugini18nBase = rundeckPage.pluginBasei18nUrl(myProvider);
    jQuery.ajax({
        url: plugini18nBase + '/messages.properties?format=json',
        success: function (data) {
            if (typeof(window.Messages) != 'object') {
                window.Messages = {};
            }
            jQuery.extend(window.Messages, data);
        }
    });
}
~~~

This example is similar to the first example, but loads a HTML file specific to the Language/Locale of the user. If the locale is `es_419` this would load the `resources/i18n/html/myfile_es_419.html` file:

~~~{.js}

function loadi18nHtmlTemplate(file){
	var myProvider='com.mycompany.rundeck.myplugin';
	var plugini18nBase = rundeckPage.pluginBasei18nUrl(myProvider);
    var fullUrl = plugini18nBase + '/html/' + file + ".html";
    jQuery.get(fullUrl, function (text) {
    	//do something with the HTML contents         
    });
}
~~~

## Example Plugin


Here are some [UI Plugin Examples][example-code].



[UIPlugin]: ../javadoc/com/dtolabs/rundeck/plugins/rundeck/UIPlugin.html
[Plugin Development - Plugin Localization]: plugin-development.html#plugin-localization
[Localization]: plugin-development.html#plugin-localization
[Plugin Development - Internationalization/Localization for Zip files]: plugin-development.html#internationalizationlocalization-for-zip-files
[Plugin Icons]: plugin-development.html#plugin-icons
[Knockout]: http://knockoutjs.com/
[example-code]: https://github.com/rundeck-plugins/ui-plugin-examples/