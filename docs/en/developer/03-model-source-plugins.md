% Resource Model Source Plugin
% Greg Schueler, Alex Honor
% November 20, 2010

## About 

Resource Model Sources provide the means to retrieve Node resources for a Project.
You can implement a Resource Model Source using a [Java Plugin Type](#java-plugin-type)
or a [Script Plugin Type](#script-plugin-type).

## Plugin configuration

The `ResourceModelSource`
[(javadoc)](../javadoc/com/dtolabs/rundeck/core/resources/ResourceModelSource.html) service allows the plugins to be configured via the Rundeck Web GUI. You are thus able to declare configuration properties for
your plugin, which will be displayed as a web form when the Project is configured, or can be manually configured in the `project.properties` file.


## Java Plugin Type

A ResourceModelSource provider is actually a Factory class.  
An instance of your ResourceModelSource provider will be
re-used, so each time a new ResourceModelSource with a new configuration is required, 
your Factory class will be invoked to produce it.

Your provider class must implement the interface
[ResourceModelSourceFactory](../javadoc/com/dtolabs/rundeck/core/resources/ResourceModelSourceFactory.html):

~~~~~~{.java}
public interface ResourceModelSourceFactory {
    /**
     * Return a resource model source for the given configuration
     */
    public ResourceModelSource createResourceModelSource(Properties configuration) 
       throws ConfigurationException;
}
~~~~~~~~~


### Plugin properties 

See [Plugin Development - Java Plugins - Descriptions](plugin-development.html#plugin-descriptions)
to learn how to create configuration properties for your ResourceModelSource plugin.

## Script Plugin Type

See the [Script Plugin Development](plugin-development.html#script-plugin-development) 
for the basics of developing script-based plugins for Rundeck.

### Instance scope properties

Instance scoped properties for ResourceModelSources are loaded from the project's Resource Model Source entries.  A project can define multiple entries, and at execution time, the Instance scoped values come from those entries.  

### Example

Here is an example `plugin.yaml` script-based ResourceModelSource plugin 
declaring a provider clled "mysource" that produces resource-format `resourceyaml` output.
The provider declares three config properties (account, url, region)
and illustrates the use
of three different types (Integer, String, FreeSelect).

Example: plugin.yaml

~~~~~~ {.yaml .numberLines}
name: My Resource Model Source
version: 1.0
rundeckPluginVersion: 1.0
author: alexh
date: 05/10/12
providers:
    - name: mysource
      service: ResourceModelSource
      plugin-type: script
      script-interpreter: bash -c
      script-file: nodes.sh
      resource-format: resourceyaml
      config:
        - type: Integer
          name: account
          title: Account
          description: Enter the account number.
        - type: String
          name: url
          title: URL
          description: Enter the URL to the inventory service.
        - type: FreeSelect
          name: region
          title: Region
          description: Select a region.
          required: true
          default: east
          values: east,north,south,west
~~~~~~~~~~

The `script-file` entry on line 11 references a script called "nodes.sh" referencing
the plugin properties (see script below).

Example script-file: nodes.sh

~~~~~~~ {.bash}
#!/usr/bin/env bash

# variables set by plugin properties:
: ${RD_CONFIG_ACCOUNT:?"account plugin property not specified"}
: ${RD_CONFIG_REGION:?"region plugin property not specified"}
: ${RD_CONFIG_URL:?"url plugin property not specified"}

#
# Generate node data here.
#
exit $?
~~~~~~~~~~~

### Provider Script Requirements

The ResourceModelSource service has expectations about the way your provider script behaves.

Exit code:

* Exit code of 0 indicates success.
* Any other exit code indicates failure.


Script output:

* All output on `STDOUT` will be captured and passed to a 
[ResourceFormatParser](model-format-parser-and-generator-plugin.html#resourceformatparser) for the specified `resource-format` to create the Node definitions.

