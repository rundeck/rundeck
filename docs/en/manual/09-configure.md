
The Rundeck GUI has a Configuration Page which contains lets you view and manage some configuration settings.  

If you have `admin` role access, when you log in you will see the "Configure" icon in the header:

![Configure page link](../figures/fig0701.png)

This page contains links to manage Project configuration as well as system level configuraiton information.

![Configure menu](../figures/fig0702.png)


## Project Configuration

The selected project will be displayed with basic configuration options, and the list of configure Resource Model Sources, as well as the default Node Executor and File Copier settings.

If you click on "Project Configuration", you will be taken to the Project Configuration form where you can view and edit the configuration.

![Project Configuration Form](../figures/fig0705.png)

There are several configuration sections: Resource Model Sources, Default Node Executor, and Default File Copier sections. Each section is described below:

### Resource Model Sources Configuration

This section lets you add and modify Resource Model Sources for the project.
These sources contain the node definitions for the project. 
One source will already be defined as a result of the inital project creation. 
You can figure as many as you need. Rundeck will aggregate the data from each
one providing a merged view.

To add a new one, click "Add Source". You are prompted to select a type of source. The list shown will include all of the built-in types of sources, as well as any Plugins you have installed.

![Add Resource Model Source](../figures/fig0706.png)

When you click "Add" for a type, you will be shown the configuration options for the type. 

![Configure Resource Model Source](../figures/fig0707.png)

You can then click "Cancel" or "Save" to discard or add the configuration to the list. 

Each item you add will be shown in the list:

![Configured Source](../figures/fig0708.png)

To edit an item in the list click the "Edit" button.  To delete an item in the list click the "Delete" button.

Each type of Resource Model Source will have different configuration settings of its own. The built-in Resource Model Source providers are shown below.

You can install more sources as plugins, see [Resource Model Source Plugins](../plugins-user-guide/resource-model-source-plugins.html#resource-model-source-plugins).

#### File Resource Model Source

This is the File Resource Model Source configuration form:

![File Resource Model Source](../figures/fig0707.png)

See [File Resource Model Source Configuration](../plugins-user-guide/resource-model-source-plugins.html#file-resource-model-source-configuration) for more configuration information.

#### Directory Resource Model Source

Allows a directory to be scanned for resource document files. All files
with an extension supported by one of the [Resource Model Document Formats](../plugins-user-guide/resource-model-source-plugins.html#resource-model-document-formats) are included.

![Directory Resource Model Source](../figures/fig0709.png)

See [Directory Resource Model Source Configuration](../plugins-user-guide/resource-model-source-plugins.html#directory-resource-model-source-configuration) for more configuration information.

#### Script Resource Model Source

This source can run an external script to produce the resource model 
definitions.

![Script Resource Model Source](../figures/fig0710.png)

See [Script Resource Model Source Configuration](../plugins-user-guide/resource-model-source-plugins.html#script-resource-model-source-configuration) for more configuration information.

#### URL Resource Model Source

This source performs a HTTP GET request on a URL to return the 
resource definitions.

![URL Resource Model Source](../figures/fig0711.png)

See [URL Resource Model Source Configuration](../plugins-user-guide/resource-model-source-plugins.html#url-resource-model-source-configuration) for more configuration information.

### Default Node Executor Configuration

When Rundeck executes a command on a node, it does so via a "Node Executor".
The most common built-in Node Executor is the "SSH" implementation, which uses
SSH to connect to the remote node, however other implementations can be used.

Select the Default Node Executor you wish to use for all remote Nodes for the project:

![Default Node Executor Choice](../figures/fig0712.png)

You can install more types of Node Executors as plugins, see [Node Execution Plugins](../plugins-user-guide/node-execution-plugins.html).

### Default File Copier Configuration

When Rundeck executes a script on a node, it does so by first copying the script as a file to the node, via a "File Copier". (It then uses a "Node Executor" to execute the script like a command.)

The most common built-in File Copier is the "SCP" implementation, which uses
SCP to copy the file to the remote node, however other implementations can be used.

Select the Default File Copier you wish to use for all remote Nodes for the project:

![Default File Copier Choice](../figures/fig0713.png)

You can install more types of File Copiers as plugins, see [Node Execution Plugins](../plugins-user-guide/node-execution-plugins.html).

## Key Storage

The Key Storage facility provides an [API](../api/rundeck-api.html#key-storage) and GUI to manage public, private keys and passwords.
These stored keys are used by both Node Executor and File Copier plugins.
Project configuration settings allow you to reference a key path for both the default Node Executor and File Copier 
([see above](configure.html#default-node-executor-configuration)). 

Keys can be stored either using an uploaded file or as text input. Public key data can be retrieved once stored. Private key and password data is only available to the node execution plugins.

### Key store organization

You can organize the keys similar to how you locate files in a file system, using convention to define the folder structure.
Here's an example convention that uses three fields to manage keys.

    keys/projects/{project}/nodes/{node}/{identity}.pem convention

The fields in the pattern provide slots to allow multiple projects and users.

* project: project name
* node: might be a specific node or could be a class name like 'www'.
* identity: the name of the user. 


### Add Private Key with text

The Key Store page provides a simple browser to navigate the folders and perform actions on the keys.

Press the "Add or Upload a Key" button.

![Key Storage browser](../figures/fig0720.png)

Next, choose "Private Key" for the Key type.

![Key Type menu](../figures/fig0722.png)

Key data can be added using the content of an uploaded file or as text input.

Choose the "Enter text" option. Paste the key content here.

You will also need to enter the storage path and give the key a name.


![Add a key and enter text](../figures/fig0721.png)

Here's the form with all the input.

Press the "Save" button.

![Key Storage UI](../figures/fig0724.png)

After the key has been saved, the browser will be updated.

![Key Storage page listing](../figures/fig0725.png)

### Add Private Key with file

![Key Storage file upload](../figures/fig0728.png)

### Add a password

![Key Storage UI](../figures/fig0726.png)

### Delete a key

![Key Storage UI](../figures/fig0727.png)

## System Configuration

This page displays key configuration settings. 
Modify the associated configuration file to change a configuration setting.

![System settings](../figures/fig0715.png)

## Security
System security is managed through configuration files. This page describes the current settings the files to change them.

![Security settings](../figures/fig0716.png)

## System Report

The System Report page gives you a breakdown of some of the Rundeck server's system statistics and information:

![System Report Page](../figures/fig0703.png)

This information is also available via the API: [API > System Info](../api/index.html#system-info)

## List Plugins
This page lists the installed and bundled plugins.

![Installed plugins](../figures/fig0714.png)

## Licenses

This page lists the Rundeck and third party library licenses.

