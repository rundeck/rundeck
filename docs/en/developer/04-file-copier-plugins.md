% File Copier Plugin
% Greg Schueler, Alex Honor
% November 20, 2010

## About

A File Copier provider copies a file or script to a remote or local node.



## Java Plugin Type

Your provider class must implement the interface
[DestinationFileCopier](../javadoc/com/dtolabs/rundeck/core/execution/service/DestinationFileCopier.html):

~~~~~ {.java}
public interface DestinationFileCopier extends FileCopier {

    public String copyFileStream(ExecutionContext context, 
                                 InputStream input, 
                                 INodeEntry node, 
                                 String destination) throws FileCopierException;

    public String copyFile(ExecutionContext context, 
                           File file, 
                           INodeEntry node, 
                           String destination) throws FileCopierException;

    public String copyScriptContent(ExecutionContext context, 
                                    String script, 
                                    INodeEntry node, 
                                    String destination) throws FileCopierException;
}
~~~~~~~~~

### Plugin properties


See [Plugin Development - Java Plugins - Descriptions](plugin-development.html#plugin-descriptions)
to learn how to create configuration properties for your plugin.

> Note, the `destination` parameter may or may not be specified. If it is not null, it indicates that the file must be copied to the requested destination filepath. If it is null, it indicates that the copied file is likely a script file, and it should be copied to a temporary file location. In either case, the resulting file path must be returned as the result of the method call.


## Script Plugin Type

See the [Script Plugin Development](plugin-development.html#script-plugin-development) 
for the basics of developing script-based plugins for Rundeck.

### Additional data context properties

The data context used in the script plugin definition can use these additional properties:

`${file-copy.file}`

  : The local path to the file that needs to be copied.

`${file-copy.destination}`

  : The remote destination path that is requested, if available.

### Provider Script Requirements

The specific service has expectations about the way your provider script behaves:

#### Script Exit Code

* Exit code of 0 indicates success.
* Any other exit code indicates failure.

#### Script Output

The first line of output of `STDOUT` MUST be the filepath of the file copied to the target node. Other output is ignored. All output to `STDERR` will be captured for the job's output.

