% Plugin Annotations
% Greg Schueler, Alex Honor
% November 20, 2010

## About
Some Rundeck Plugins allow you to use annotations to add 
[description metadata](plugin-development.html#plugin-descriptions) 
about your plugin to the class
definition itself, and Rundeck will extract that metadata for use in
displaying the plugin information and configuration properties in the GUI, as
well as for applying the runtime configuration values to your plugin class
instance when it is being executed.


>Note, ResourceModelSource, NodeExecutor and FileCopier plugins currently do not support description annotations.

## Plugin Description
You can define the display name, and descriptive text about your plugin by adding a 
[PluginDescription](../javadoc/com/dtolabs/rundeck/plugins/descriptions/PluginDescription.html) annotation to your plugin class.

Attributes of `@PluginDescription`:

* `title` - the display name for your plugin
* `description` - descriptive text shown next to the display name

Example:

~~~~~~ {.java}
@Plugin(name="myplugin", service=ServiceNameConstants.WorkflowStep)
@PluginDescription(title="My Plugin", description="Performs a custom step")
public class MyPlugin implements StepPlugin{
    ...
}
~~~~~~~~

*Note:* If you do not add this annotation, the plugin display name will be the same as the provider name, and will have 
no descriptive text when displayed.

## Plugin Properties

You can annotate individual fields in your class to define the configuration
properties of your class.  These are the supported Java types for annotated fields:

* String
* Boolean/boolean
* Integer/integer, Long/long

When your plugin is executed, the fields will be set to the appropriate values
based on their default value, scope, and any value set by the user in the
workflow configuration.

These annotation classes are used:

* [PluginProperty](../javadoc/com/dtolabs/rundeck/plugins/descriptions/PluginProperty.html) - Declares a class field as a plugin configuration property
* [SelectValues](../javadoc/com/dtolabs/rundeck/plugins/descriptions/SelectValues.html) - Declares a String property to be a "Select" property, which defines a set of input values that can be chosen from a list
* [TextArea](../javadoc/com/dtolabs/rundeck/plugins/descriptions/TextArea.html) - Declares a String property to be rendered as a multi-line text area in the Rundeck GUI.

Attributes:

* `@PluginProperty`
    * `name` - the property identifier name
    * `title` - the property display name
    * `description` - descriptive text
    * `defaultValue` - default value
    * `required` - (boolean) whether the property is required to have an input value. Default: false.
    * `scope` (PropertyScope) the resolution scope for the property value
* `@SelectValues`
    * `values` (String[]) the set of values that can be chosen
    * `freeSelect` (boolean) whether the user can enter values not in the list. Default: false.

Examples:

~~~~~~ {.java}
@PluginProperty(title = "Name", description = "What is your name?", required = true)
private String name;
 
@PluginProperty(title = "Age", description = "How old are you?")
private int amount;
 
@PluginProperty(title = "Favorite Fruit",
                description = "What is your favorite fruit?",
                defaultValue = "banana")
@SelectValues(values = {"banana", "lemon", "orange"}, freeSelect = true)
private String fruit;
~~~~~~~~~~

## Property Scopes

You can define the scope for a property by adding `scope` to the PluginProperty annotation.  Refer to the class [PropertyScope](../javadoc/com/dtolabs/rundeck/core/plugins/configuration/PropertyScope.html).  

The default effective scope if you do not specify it in the annotation is `InstanceOnly`.

For more information see [Property Scopes](plugin-development.html#property-scopes).
