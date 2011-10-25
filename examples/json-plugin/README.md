Example JSON Resource Format Plugin
================================

This is an example plugin for RunDeck that provides JSON format support for resource definitions.

This example demonstrates these aspects of the RunDeck plugin system:

* A [Java plugin implementation](http://rundeck.org/docs/RunDeck-Guide.html#java-plugin-development)
    * including an embedded [third-party jar dependency](http://rundeck.org/docs/RunDeck-Guide.html#jar-dependencies)
* A [Resource Format plugin](http://rundeck.org/docs/RunDeck-Guide.html#resource-format-plugins), including parser (`ResourceFormatParser`) and generator (`ResourceFormatGenerator`)
    * see [Resource Format Parser and Generator Providers](http://rundeck.org/docs/RunDeck-Guide.html#resource-format-parser-and-generator-providers)

This Resource Format Plugin provides the format name:

* Format name: `resourcejson`
* Recognized file extensions: `.json`
* Recognized MIME types: `application/json`

Build
-----

Prerequisites: the rundeck-core-1.4.0.jar must be available in the relative path

    ../../core/build/libs/rundeck-core-1.4.0.jar

Then execute:

    ./gradlew

or

    gradlew.bat

The result:

    build/libs/rundeck-json-plugin-1.0.jar

Install
------

Place the `rundeck-json-plugin-1.0.jar` in the `$RDECK_BASE/libext` directory of your rundeck server.

Usage
-----

1. You can export your resource definitions in JSON format using the HTTP API:

        http://server/api/3/project/NAME/resources?authtoken=TOKEN&format=resourcejson


2. You can provide JSON resource data for any Resource Model Source, by specifying either the format name `resourcejson`, or a file extension of `.json`.

Example `project.properties` configuration to add a JSON resource file:

    resources.source.1.type=file
    resources.source.1.config.file=/Users/greg/rundeck140/resources.json
    resources.source.1.config.format=resourcejson

Note: the `format` does not have to be specified if the file extension is `.json`.

Format
------

    {
      "nodes": {
        "node4": {
          "tags": [
            "test","mankle"
          ],
          "attributes": {
            "username": "testuser",
            "description": "a test node",
            "hostname": "testnode1"
          }
        },
        "node5": {
          "tags": [
            "test",
            "blah"
          ],
          "attributes": {
            "username": "testuser",
            "description": "a test node",
            "hostname": "testnode1"
          }
        },
        "node6": {
          "tags": [
            "test",
            "blee",
            "blah"
          ],
          "attributes": {
            "username": "testuser",
            "description": "a test node",
            "hostname": "testnode1"
          }
        }
      }
    }
    