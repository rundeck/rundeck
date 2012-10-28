% Node Resource Sources

Rundeck can integrate with external data by configuring the use of *Providers* or *Sources*.  Providers are third-party services or systems that export data that Rundeck can import. Additionally, Rundeck supports an external Editor for Node data.

Rundeck makes use of common data formats (XML, JSON & YAML).  Third-party software may produce these formats natively, however it is typical to have to massage the output of one system into the appropriate format to be consumed by Rundeck.  Since URLs and HTTP are a lowest-common-denominator for communication, Rundeck only requires that the data Providers make this data available as a file at a URL or on the local disk.

There are a few types of external integration:

[*Resource Model Source*](node-resource-sources.html#resource-model-source)
:   Provides a set of Nodes in XML or YAML format. E.g. a CMDB or hosted virtual machines service. Rundeck can be configured to use a different provider for each Project, and can refresh the Resources it uses from this provider.

[*Resource Editor*](node-resource-sources.html#resource-editor)
:   Provides a web-based editor to manage the Node definitions. Rundeck can link to this editor from the Run page, and has optional JavaScript interactions to make editing externally-managed Node resources integrated with the Rundeck GUI.

## Resource Model Source

The Resource model source is a way to transfer Node definitions from other systems, tools or services into Rundeck. The means of providing the Resource model data can be done in whichever way suits your environment best.

Rundeck supports plugins to provide different kinds of sources, but has built-in
support for URLs or File based sources.

Resource model data is a set of Node descriptors, each with a uniquely identifying name.  In addition to Name, some pieces of metadata are required (like `hostname`, and `username`), and some are optional.

(See [Resource Model Document Formats](../manual/rundeck-basics.html#resource-model-document-formats) for more information on what format the files need to be in.)

The Resource model data is stored on the server as a set of files.  Each Project in Rundeck has at least a single Resources file, and may have multiple additional sources (such as a URL or a directory containing multiple files). All of these sources will be combined into the set of all Nodes that are available for the project.  Each node's metadata can define how to connect to it and run commands.

### Requirements ###

In order to provide the Resource model data to Rundeck:

1. The data must be in one of the supported [Resource Model Document Formats](../manual/rundeck-basics.html#resource-model-document-formats)
2. Each Node entry must have a unique `name` value. You may have to convert the external system's identifier to be unique, or create one yourself.

This means you can provide the data in the way that best suits your specific use-case.  Some examples:

* Hand-crafted XML/YAML data, which you could store in a version control system.  The URL for the file in the VCS repository would be provided to Rundeck.
    * To update the data you would commit changes to your VCS, and then tell Rundeck to refresh.
* Data generated from a custom CMDB or other software, and stored on disk.
    * You could do this with a cron-job, or via some external trigger.  Rundeck will simply read the resource.xml/resource.yaml file identified in the configuration file.
* Data generated from a simple CGI script which interfaces with another third-party or external service.
    * You could run Apache and host a simple CGI script.  The CGI script would communicate to some other system and then return the XML/YAML content.  You could tell Rundeck to refresh the Resource model, which would in turn cause the CGI to access the external data and return the reformatted content.
* Using a [Resource Model Source Plugin](../manual/plugins.html#resource-model-source-plugins), the data could come from some other external source

The Resource model data does not have to include the Rundeck server Node, as it is implicitly included.

### Configuration ###

Resource model sources are defined on a per-project basis, in the [`project.properties`](configuration.html#project.properties) file.

The only required configuration value is `project.resources.file`, which defines a single file containing resource model data stored on-disk.  Each new project will have a good default location, but you may change either the location or the file extension. The file
extension determines the format of the data. (See [Resource Model Document Formats](../manual/rundeck-basics.html#resource-model-document-formats).)

    project.resources.file = ..
    
This file path is where Rundeck will read the contents from, and also where it will store it to if refreshing from a remote URL.

You may also specify a URL, which will be automatically retrieved and stored in a cache file. The Resource Model data within the content is merged with the previous file.

    project.resources.url = http://...
    
This configures the remote URL for loading the Resources model data.

In addition, multiple [Resource Model Source Plugin](../manual/plugins.html#resource-model-source-plugins) can
be configured to add additional sources of Resource Model data.

### Implementations and Examples ###


[curl]: http://curl.haxx.se/
[xmlstarlet]: http://xmlstar.sourceforge.net/
[CMDB]: http://en.wikipedia.org/wiki/Configuration_management_database
[AJAX]: http://en.wikipedia.org/wiki/Ajax_(programming)

#### Simple VCS URL resource model source

Putting the resources.xml/resources.yaml file under the control of a source code
management tool is a simple solution to controlling and tracking
change to the resources.xml file. 
Any changes will be committed and the commit messages become an audit
log. Most source code management tools provide a web interface to
retrieve file revisions based on a URL and thus make it accessible as
a resource model source.

Going back to the [Acme Anvils Example](../manual/rundeck-by-example.html#acme-anvils) section, imagine the
administrator decides the VCS approach is a good first step to
control versioning for the anvils resource model. Acme is a [subversion] user
and installed [viewvc] to give web access to the repository.

First, the current resources.xml is added to the repository and committed:

    svn add resources.xml http://svn.acme.com/ops/anvils/resources.xml
    svn commit -m "added resource model for anvils" resources.xml

To test access, the administrator downloads the latest revision (ie,
"HEAD") via the "viewvc" interface.

     curl http://svn.acme.com/viewvc/ops/anvils/resources.xml?revision=HEAD

Next, the anvils project.properties configuration file is modified to
reference the URL to retrieve the "HEAD" revision:

    project.resources.file = /etc/rundeck/projects/anvils/resources.xml
    project.resources.url  = http://svn.acme.com/viewvc/ops/anvils/resources.xml?revision=HEAD

This configuration specifies the anvils resource model will be retrieved
from <code>project.resources.url</code> and then stored at
<code>project.resources.file</code>. Now, anytime Rundeck refreshes the
anvils resource model, it will request the resources.xml file from
the viewvc URL, obtaining the latest revision.

[subversion]: http://subversion.tigris.org/
[viewvc]: http://www.viewvc.org/

#### Amazon EC2 Nodes ####

[Amazon's EC2](http://aws.amazon.com/ec2/) (Elastic Cloud Compute) is a cloud service in wide use for dynamic infrastructure; it is easy to start up and shut down Node "Instances" in the cloud.  

For Rundeck, we would like to have a way of querying the EC2 service to see what EC2 Instances are available for use as Rundeck Nodes.

Amazon has a well-defined API for communication with their services, which would allow us to pull out the EC2 data, and generate XML if we wanted to. We could write a script that produces that data and use that script on a server to produce data via a URL, or we could use that script with the [script resource model source plugin](../manual/plugins.html#script-resource-model-source-configuration) to generate it. This would give us complete control of the output, but does require extra work.

However, there is already a plugin to do this for you: the [Rundeck EC2 Nodes Plugin](https://github.com/gschueler/rundeck-ec2-nodes-plugin).

* [rundeck-ec2-nodes-plugin](https://github.com/gschueler/rundeck-ec2-nodes-plugin) project source code on github
* [download the binary distribution](https://github.com/gschueler/rundeck-ec2-nodes-plugin/downloads).

Use is fairly simple:

1. Copy the plugin file "rundeck-ec2-nodes-plugin-1.2.jar" into your `$RDECK_BASE/libext` directory. The plugin contains all of the required dependencies.
2. Login to Rundeck with an administrator account, and click the "Admin" link in the page header for your project then click the "Configure Project" link, *or* create a new project.
3. In the project configuration page, under **Resource Model Sources** click the "Add Source" button.
4. Click "Add" for the "AWS EC2 Resources" type.
5. Enter the configuration details (see below) for the plugin and click "Save".
6. Click "Save" for the Project Configuration.

Minimal configuration details for the plugin includes your AWS access credentials you can find here <http://aws.amazon.com/security-credentials>.

*Access Key*
:    Specify your AWS Access key.

*Secret Key*
:    Specify your AWS Secret Key

Read about the other configuration details in the [readme](https://github.com/gschueler/rundeck-ec2-nodes-plugin/blob/master/Readme.md) for the rundeck-ec2-nodes-plugin.

Finally, within Rundeck, you can Refresh the Nodes from within the Run tab.  You should see a Node entry for each EC2 Instance that is available.

You can manage the set of Nodes that gets returned from the plugin by organizing your EC2 instances using EC2 Tags, as well as adding EC2 Filters to the plugin configuration.

The EC2 plugin will automatically add tags for the nodes based on an EC2 Instance Tag named "Rundeck-Tags", as well as the Instance's state.  You can also add "Mapping parameters" to the EC2 Plugin configuration to add additional tags.

You can add filters to the EC2 Plugin configuration under the "Filter Params" configuration area, with the sytanx of: `filter=value;filter2=value2`. The available filter names are listed in [AWS API - DescribeInstances](http://docs.amazonwebservices.com/AWSEC2/latest/APIReference/ApiReference-query-DescribeInstances.html).

You can also configure your EC2 Plugin manually or automatically by creating or modifying the [project.properties](configuration#project.properties) file, and defining a [Resource Model Source](../manual/plugins.html#resource-model-sources) provider, like this:

    resources.source.1.type=aws-ec2
    resources.source.1.config.accessKey=...
    resources.source.1.config.privateKey=...
    resources.source.1.config.filter=...

More configuration is available for the [rundeck-ec2-nodes-plugin project](https://github.com/gschueler/rundeck-ec2-nodes-plugin).

#### Third party URL resource model sources

URL Resource model sources can be developed by third parties to integrate Rundeck with their tools. 

Check the list on our wiki: [https://github.com/dtolabs/rundeck/wiki/Resource-model-providers](https://github.com/dtolabs/rundeck/wiki/Resource-model-providers).

## Resource Editor

The Resource Editor integration is a way to link to a third-party system used for managing Node definitions from within Rundeck. Each Node entry in the resources.xml or resources.yaml can define a URL to provide an "Edit" link that will appear in the Rundeck Run page for that Node.

This allows you to make use of the Resource Model Source in a more seamless way.  Rundeck will load the Resource model from the third-party Provider system, and users of Rundeck can click straight to the Editor for those Nodes.  The Provider and the Editor could be the same system, or they could both be custom CGI scripts that integrate with a third system.

Some teams have acquired or developed tools to manage information
about the hosts deployed in their networks. These tools have
interfaces to not just view but also modify the data about these
hosts. Though there is no widely used common standard adopted by users
of these tools, it is possible to map the data to meet the needs of
[Rundeck resource models](#resource-model-provider). 

### Definition ###

The [Rundeck resource model document format](../manpages/man5/resource-v13.html) and the [resource-yaml-v13](../manpages/man5/resource-yaml-v13.html) format provide two attributes that help connect the dots between the
Rundeck UI and the editing interface provided by the external data
management tool. They can use `editUrl` or `remoteUrl` attributes to specify the remote URL.  The URLs can embed properties about the node to expand prior to being loaded, which allows you to e.g. submit query parameters using the node name.

`editUrl`

:    Specifies a URL to a remote site which will allow editing of the Node.  When specified, the Node resource will display an "Edit" link in the Rundeck GUI and clicking it will open a new browser page for the URL.

[`remoteUrl`](node-resource-sources.html#using-remoteurl)

:    Specifies a URL for a remote site which will be loaded in an iframe within a Rundeck page.  Clicking the "Edit" link for the Node will load content from the site within the current Rundeck page, allow you to perform your edit at the remote site, and has optional JavaScript hooks to report the state of the editing process back to the Rundeck page for a more streamlined user interface. 

### Using properties ###

Properties of the Node can be embedded in the URL and expanded prior to use.  The syntax is:

    ${node.property}

Available properties are:

`name`, `hostname`, `os-name`, `os-version`, `os-family`, `os-arch`, `username`, `description`, `tags`, `project`

You can embed these properties within the url like this:

    http://mycmdb:8080/node/edit?name=${node.name}

### Using remoteUrl ###

Using the `remoteUrl` lets you embed another site into an iframe within the Rundeck page, and optionally allows communication back to the Rundeck page about the state of the editing process.

If you want to embed the remote site without having to make any changes to the remote page content, you can do so simply by specifying the `remoteUrl` to use.  When the user clicks "Edit" the site will load within an iframe, and the user can perform whatever actions on the site are necessary.  After they are done they will have to manually click the "Close" button on the Rundeck page to close the iframe.

If you want the user interface in Rundeck to be more streamlined, you will have to be able to modify the web pages produced by the remote site to add simple Javascript calls to communicate back to the Rundeck page.  The JavaScript hooks are designed to not add much burden to the developer of the remote site or system, so they are optional.

#### Streamlining the interface ####

If the remote site implements some Javascript messaging conforming to a simple optional protocol, then the user interface between Rundeck and the remote site can be made more seamless.

Rundeck lets the remote site inform it when the following steps occur: 

* The user begins editing a Node
* The user saves the Node changes successfully and is finished
* The user cancels the Node changes, or otherwise has finished without saving
* An error occurs and an error message should be shown.

Due to web browser security restrictions, direct communication between different webpages can only be done through use of the [postMessage](http://www.whatwg.org/specs/web-apps/current-work/#crossDocumentMessages) method.  

The remote page can send these messages simply with this javascript:

    <script type="text/javascript">
        if(window.self!=window.parent){
            window.parent.postMessage("...message...","http://rundeckserver:port");
        }
    </script>

`window.parent` will be the enclosing browser window when the site is loaded within an iframe.  This script simply checks whether the page is loaded in an iframe before sending the message.
    
The first argument to `postMessage` is one of the message codes shown below.  The second argument is the expected "origin", meaning the URL scheme, server and port of the server receiving the message.  You can specify "*" to include any site that may be loading the content, but you may want to restrict it to your Rundeck installation's scheme, hostname and port.

Rundeck can receive the following messages sent by the remote site:

`rundeck:node:edit:started`
  ~ Sent as soon as the remote edit URL is loaded and indicates that the remote Site understands the messaging protocol and has loaded the correct edit page.  You would probably send this on the "edit" or "form" page for the targetted node.

`rundeck:node:edit:error` or `rundeck:node:edit:error:An error message`
  ~ Sent if some error occurs.  The remote editing form will close and the error message (if any) will be shown.  You would probably send this on the "edit" or "view" page if there is an error locating the targeted Node or loading anything required for the edit process.
 
The next two messages are only valid after the "started" message has already been received:

`rundeck:node:edit:finished:true`
  ~ Sent after the remote form has been saved without errors.  This indicates that the editing process is done and has completed with saved changes.  You would probably send this on the "view" or "show" page for the targeted node if the save operation was successful.
  
`rundeck:node:edit:finished:false`
  ~ Sent after the remote form has been either cancelled or discarded without changes.  This indicates that the editing process is done but no changes were made.  You would probably send this on the "view" or "show" for the targeted node (if your site simply shows the node view again) or "list" page (if your site goes back to a list of resources) if the user hits "cancel".
  
Any message not shown here that is received by Rundeck after it has received the "started" message will be considered unexpected and the editing process will close the iframe.

The user will also have the option to close and cancel the remote editing process at any time.

Note that sending the "error" or "finished" message will close the editing session and all subsequent messages will be ignored.

TODO: The JavaScript code to communicate back to Rundeck could be bundled into a simple widget script file for easier inclusion on remote sites.

### Examples ###

Here are some examples of using the `editUrl` and `remoteUrl` in a resources.xml/resources.yaml file:

Specify a simple URL for editing, which will simply produce a link:

    <node name="venkman" editUrl="http://mycmdb:8080/node/edit" ... />
   
Specify a URL for editing, with embedded "name" property as a parameter:

    <node name="venkman" editUrl="http://mycmdb:8080/node/edit?name=${node.name}" ... />
   
Specify a remote URL with embedded "name" and "project" properties as parameters:

    <node name="venkman" remoteUrl="http://mycmdb:8080/node/edit?name=${node.name}&amp;project=${node.project}" ... />
   
Specify a remote URL with embedded "name" property as part of the path:

    <node name="venkman" remoteUrl="http://mycmdb:8080/node/edit/${node.name}"  ... />

In YAML, some examples:

Specify a remote URL with embedded "name" and "project" properties as parameters:

    venkman:
      nodename: venkman
      remoteUrl: http://mycmdb:8080/node/edit?name=${node.name}&amp;project=${node.project}

Specify a remote URL with embedded "name" property as part of the path:

    venkman:
      nodename: venkman
      remoteUrl: "http://mycmdb:8080/node/edit/${node.name}

#### Simple site integration ####

The [ndbtest](https://github.com/gschueler/ndbtest) project on github provides an example of how the remote Resource Editor can integrate with Rundeck using JavaScript.

This project is a simple [Grails](http://grails.org) application which provides a database of Node data.  The standard web-based user flow is:

* List all nodes.
* Edit a Node with the edit page. From here the User can:
    * Cancel the Node changes
        * Goes to the Node show page
    * Save the Node changes
        * Result is successful
            * Goes to the Node show page
        * Result fails, so display an Error message (either on the edit page or the list page)

We want the Node's "edit" link in Rundeck to go directly to an edit page, so the `remoteUrl` for each Node entry then should be a URL to link to this page, for example:

    remoteUrl="http://localhost:8080/node/edit?name=${node.name}&amp;project=${node.project}"

The code below shows that the `name` & `project` are used to select the correct node from the database, even though the built-in identifier is an ID number:

* [NodeController.groovy:51](https://github.com/gschueler/ndbtest/blob/master/grails-app/controllers/com/dtolabs/ndb/NodeController.groovy#L51).

    * Note that if there is no Node found with the specified values, then the response will be to set an error message and then show the list page.

So the JavaScript for integrating with Rundeck is then added to the following pages in this system:

* [node/edit.gsp](https://github.com/gschueler/ndbtest/blob/master/grails-app/views/node/edit.gsp)
    * If an error has occurred, it posts an error message starting on [Line 27](https://github.com/gschueler/ndbtest/blob/master/grails-app/views/node/edit.gsp#L27)
    * Otherwise, it posts the `started` message starting [on line 34](https://github.com/gschueler/ndbtest/blob/master/grails-app/views/node/edit.gsp#L34)
* [node/show.gsp](https://github.com/gschueler/ndbtest/blob/master/grails-app/views/node/show.gsp)
    * If the node save was successful, send the `finished:true` message, starting at [line 21](https://github.com/gschueler/ndbtest/blob/master/grails-app/views/node/show.gsp#L21).
    * Otherwise send the `finished:false` message starting at [line 28](https://github.com/gschueler/ndbtest/blob/master/grails-app/views/node/show.gsp#L28).
* [node/list.gsp](https://github.com/gschueler/ndbtest/blob/master/grails-app/views/node/list.gsp)
    * If an error has occurred, it posts an error message starting on [line 20](https://github.com/gschueler/ndbtest/blob/master/grails-app/views/node/list.gsp#L20).

To complete the round-trip of editing a Node and then showing the results back in Rundeck, the ndbtest project would have to export XML formatted Resource data, and then your Rundeck project.properties file would have to point to the appropriate URL.  (This is left as an exercise to the reader.)


