
## Resource Model Sources

Rundeck includes these Built-in plugins in the core installation:

`file`

:    Uses a file on the file system, in any of the supported Resources formats.

`url`

:    GETs a URL, and expects one of the supported Resources formats.

`directory`

:    looks at all files in a directory for suppored file extensions, and internally uses the `file` provider for
     each file that matches.

`script`

:    Executes a script and parses the output as one of the supported formats

To configure these providers use the following configuration properties.

### File Resource Model Source Configuration

The `file` Resource Model Source provider reads a file in one of the supported
Resource Model Document Formats.

Name                          Value                           Notes
-----                         ------                          ------
`file`                        file path                       Path to a file on disk.
`format`                      format name                     Can be used to declare the format explicitly. Otherwise the format is determined from the `file`'s extension.
`requireFileExists`           true/false                      If true and the file is missing, causes a failure to load the nodes. (Default: false)
`includeServerNode`           true/false                      If true, include the Project's server node automatically. (Default: false)
`generateFileAutomatically`   true/false                      If true, create the file automatically if it is missing. (Default: false)
----------------------------

Table: Configuration properties for `file` Resource Model Source provider

The value of `format` must be one of the supported Resource Model Document Formats. The built-in formats are: `resourcexml` or `resourceyaml`, but any format provided by a [Resource Format Plugin](#resource-model-document-formats) can be specified as well.

*Example:*

    resources.source.1.type=file
    resources.source.1.file=/home/rundeck/projects/example/etc/resources2.xml
    resources.source.1.format=resourcexml
    resources.source.1.requireFileExists=true
    resources.source.1.includeServerNode=true
    resources.source.1.generateFileAutomatically=true

### URL Resource Model Source Configuration

The `url` Resource Model Source provider performs a HTTP GET request to retrieve the Nodes definition.

Configuration properties:

Name      Value       Notes
-----     ------      ------
`url`     URL         A valid URL, either `http:`, `https:` or `file:` protocol.
`cache`   true/false  If true, use ETag/Last-Modified information from the server to only download new content if it has changed. If false, always download the content. (Default: true)
`timeout` seconds     Number of seconds before request fails due to timeout. `0` means no timeout. (Default: 30) 
----------------------------

Table: Configuration properties for `url` Resource Model Source provider

The Resource Model Document Format that is used is determined by the MIME type
sent by the remote server. The built-in formats accept "\*/xml" and "\*/yaml" and "*/x-yaml". 

*Example:*

    resources.source.1.type=url
    resources.source.1.url=file:/home/rundeck/projects/example/etc/resources2.xml
    resources.source.1.cache=true
    resources.source.1.timeout=0

### Directory Resource Model Source Configuration

The `directory` Resource Model Source provider lists all files in a directory, and loads each one that has a supported file extension
as File Resource Model Source with all default configuration options.

Name                          Value                           Notes
-----                         ------                          ------
`directory`                   directory path                  All files in the directory that have a supported file extension will be loaded
----------------------------

Table: Configuration properties for `directory` Resource Model Source provider

*Example:*

    resources.source.2.type=directory
    resources.source.2.directory=/home/rundeck/projects/example/resources
    
### Script Resource Model Source Configuration

The `script` Resource Model Source provider executes a script file and reads
the output of the script as one of the supported [Resource Model Document Formats].

Name             Value                           Notes
-----            ------                          ------
`file`           Script file path                If required by the `interpreter`, the file should be executable
`interpreter`    Command or interpreter to use   e.g. "bash -c"
`args`           Additional arguments to pass    The arguments will be added after the script file name to the executed commandline
`format`         Format name                     Must be used to declare the format explicitly.
----------------------------

Table: Configuration properties for `script` Resource Model Source provider

The script will be executed in this way:

    [interpreter] file [args]

All output on STDOUT will be passed to a Resource Format Parser to parse.  The
format specified must be available.

*Example:*

    resources.source.2.type=script
    resources.source.2.file=/home/rundeck/projects/example/etc/generate.sh
    resources.source.2.interpreter=bash -c
    resources.source.2.args=-project example
    resources.source.2.format=resourceyaml

## Resource Model Document Formats

Resource Model Document Formats are defined by plugins that provide 
Generators and Parsers, typically in matched 
pairs, with both a parser and generator for the same format name.

### Resource Format Plugins

Rundeck includes two built-in plugins in the core installation:

`resourcexml`

:    Supports the Resource XML document format: [resource-XML](../man5/resource-xml.html).

    Supported MIME types:

    * Generator: "text/xml"
    * Parser: "*/xml"

    Supported File extensions:

    * ".xml"

`resourceyaml`

:    Supports the Resource YAML document format: [resource-YAML](../man5/resource-yaml.html).

    Supported MIME types:

    * Generator: "text/yaml", "text/x-yaml", "application/yaml", "application/x-yaml"
    * Parser: "\*/yaml", "\*/x-yaml"

    Supported File extensions:

    * ".yml", ".yaml"

`resourcejson`

:    Supports the Resource JSON document format: [resource-JSON](../man5/resource-json.html).

    Supported MIME types:

    * Generator: "application/json", "text/json"
    * Parser: "application/json", "text/json"

    Supported File extensions:

    * ".json"
