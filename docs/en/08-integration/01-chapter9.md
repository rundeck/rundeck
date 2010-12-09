Integration with External Data Providers
====

RunDeck can integrate with external data by configuring the use of *Providers*.  Providers are third-party services or systems that export data that RunDeck can import. Additionally, RunDeck supports an external Editor for Node data.

RunDeck makes use of common data formats (XML & JSON).  Third-party software may produce these formats natively, however it is typical to have to massage the output of one system into the appropriate format to be consumed by RunDeck.  Since URLs and HTTP are a lowest-common-denominator for communication, RunDeck only requires that the data Providers make this data available as a file at a URL or on the local disk.

There are a few types of external integration:

[*Resource Model Provider*](#resource-model-provider)
:   Provides a set of Nodes in XML format. E.g. a CMDB or hosted virtual machines service. RunDeck can be configured to use a different provider for each Project, and can refresh the Resources it uses from this provider.

[*Resource Editor*](#resource-editor)
:   Provides a web-based editor to manage the Node definitions. RunDeck can link to this editor from the Resources page, and has optional JavaScript interactions to make editing externally-managed Node resources integrated with the RunDeck GUI.

[*Option Model Provider*](#option-model-provider)
:   Provides a dataset in JSON format, used as input option values for Jobs. Each Job Option can be configured to load the set of allowed input values from a remote service, and the RunDeck GUI will prompt the user to choose from those values when running a Job.

Resource Model Provider
-------------

The Resource model provider is a way to transfer Node definitions from other systems, tools or services into RunDeck. The means of providing the Resource model data can be done in whichever way suits your environment best.

Resource model data is a set of Node descriptors, each with a uniquely identifying name.  In addition to Name, some pieces of metadata are required (like `hostname`, and `username`), and some are optional.

(See [resource.xml - node](resource-v10.html#node) for more detail about the `node` entry.)

The Resource model data, commonly referred to as resources.xml, is stored on the server as a file.  Each Project in RunDeck has its own resources.xml file, and this file is used to determine what Nodes are available and how to connect to those Nodes and run commands. The Resource model data is assumed to be a static file, unless a Provider URL is configured, in which case an admin can tell the RunDeck server to refresh the Resource model from the URL.

### Requirements ###

In order to provide the Resource model data to RunDeck:

1. The data must be stored in XML in the specific [resource-v10.xml format](resource-v10.html). ^[Currently XML is required, but JSON support is planned.]
2. Each Node entry must have a unique `name` value. You may have to convert the external system's identifier to be unique, or create one yourself.
3. The data must be *either*: 
    * accessible on-disk from the RunDeck server, 
    * OR accessible via a HTTP URL

This means you can provide the data in the way that best suits your specific use-case.  Some examples:

* Hand-crafted XML data, which you could store in a version control system.  The URL for the file in the VCS repository would be provided to RunDeck.
    * To update the data you would commit changes to your VCS, and then tell RunDeck to refresh.
* XML generated from a custom CMDB or other software, and stored on disk.
    * You could do this with a cron-job, or via some external trigger.  RunDeck will simply read the resource.xml file identified in the configuration file.
* XML generated from a simple CGI script which interfaces with another third-party or external service.
    * You could run Apache and host a simple CGI script.  The CGI script would communicate to some other system and then return the XML content.  You could tell RunDeck to refresh the Resource model, which would in turn cause the CGI to access the external data and return the reformatted content.

The Resource model data does not have to include the RunDeck server Node, as it is implicitly included.

### Configuration ###

Resource model providers are defined on a per-project basis, in the [`project.properties`](#project.properties) file.

Define the file where the resource.xml will be stored on-disk.  Each new project will have a good default location, but you may change it.

    project.resources.file = ..
    
This file path is where RunDeck will read the XML contents from, and also where it will store it to if refreshing from a remote URL.

    project.resources.url = http://...
    
This configures the remote URL for loading the resources.xml.  

### Implementations and Examples ###


[curl]: http://curl.haxx.se/
[xmlstarlet]: http://xmlstar.sourceforge.net/
[CMDB]: http://en.wikipedia.org/wiki/Configuration_management_database
[AJAX]: http://en.wikipedia.org/wiki/Ajax_(programming)

#### Simple VCS resource model provider

Putting the resources.xml file under the control of a source code
management tool is a simple solution to controlling and tracking
change to the resources.xml file. 
Any changes will be committed and the commit messages become an audit
log. Most source code management tools provide a web interface to
retrieve file revisions based on a URL and thus make it accessible as
a resource model provider.

Going back to the [Acme Anvils Example](#acme-anvils) section, imagine the
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
<code>project.resources.file</code>. Now, anytime RunDeck refreshes the
anvils resource model, it will request the resources.xml file from
the viewvc URL, obtaining the latest revision.

[subversion]: http://subversion.tigris.org/
[viewvc]: http://www.viewvc.org/

#### Amazon EC2 Nodes ####

[Amazon's EC2](http://aws.amazon.com/ec2/) (Elastic Cloud Compute) is a cloud service in wide use for dynamic infrastructure; it is easy to start up and shut down Node "Instances" in the cloud.  

For RunDeck, we would like to have a way of querying the EC2 service to see what EC2 Instances are available for use as RunDeck Nodes.

Amazon has a well-defined API for communication with their services, which allows us to pull out the EC2 data, and generate XML.

For this purpose, DTO Labs has created a Java-based implementation of this mechanism as the [java-ec2-nodes](https://github.com/dtolabs/java-ec2-nodes) project.

* [java-ec2-nodes](https://github.com/dtolabs/java-ec2-nodes) project source code
* [download the binary distribution](https://github.com/dtolabs/java-ec2-nodes/archives/master).

Use is fairly simple:

1. Unpack the distribution file "java-ec2-nodes-0.1-bin.zip".  This contains the required java Libs and a Perl based CGI script.
2. Create an `AWSCredentials.properties` to specify your AWS credentials. (Available at [this page](http://aws.amazon.com/security-credentials).) Place the file within the expanded java-ec2-nodes directory.
    This file should contain:

        accessKey=<your access key>
        secretKey=<your secret key>
        
4. Place the generatenodes.cgi file within a webroot folder of an Apache server, and configure Apache to allow `Options +ExecCGI` for that folder.
4. Modify the "$basedir" variable in generatenodes.cgi to point to the dir containing the zip contents you unpacked.

Finally, you should be able to do HTTP GET for the CGI (e.g. `http://myserver/scripts/generatenodes.cgi`) and see an XML file returned.  Note that the CGI allows query parameters to be used as API filters, e.g. `?tag:mytag=myvalue`.  (These filters are specific to the [EC2 API](http://docs.amazonwebservices.com/AWSEC2/latest/APIReference/ApiReference-query-DescribeInstances.html).)

Once you have the CGI producing valid XML, you can set the `project.resources.url` property in your project's project.properties file to be the URl to the CGI.  

Finally, within RunDeck, you can Refresh the Nodes from within the Resources tab.  You should see a Node entry for each EC2 Instance that is available.

You can easily manage the set of Nodes that gets returned from EC2 by organizing them by use of EC2 Tags, and applying query Filters to the EC2 API query.

In the EC2 interface, modify an Instance and add a Tag.  Set the Tag name to "RunDeck-Project" and the value to "MyProject" (or your project name).  

Then modify the URL used as your project.resources.url, to specify a query parameter of `?tag:RunDeck-Project=MyProject`.  Then RunDeck will only see those Instances with that tag as Nodes within that RunDeck project.

More configuration is available for the [java-ec2-nodes project](https://github.com/dtolabs/java-ec2-nodes).


Option model provider
-------

The Option model provider is a mechanism to allow the Options defined for a Job to have some of the possible input values provided by a remote service or database.  

Option model providers are configured on a per-Option basis (where a Job may have zero or more Options).

### Requirements ###

1. Options model data must be [JSON formatted](http://www.json.org).
2. It must be accessible via HTTP or on the local disk for the RunDeck server.
3. It must be in one of two JSON structures, *either*:
    * An array of string values
    * OR, an array of Maps, each with two entries, `name` and `value`.

### Configuration ###

Each Option entry for a Job can be configured to get the set of possible values from a remote URL.  If you are authoring the Jobs via [job.xml file format](job-v20.html#option), simply add a `valuesUrl` attribute for the `<option>`.  If you are modifying the Job in the RunDeck web GUI, you can entry a URL in the "Remote URL" field for the Option.

e.g.:

    <option valuesUrl="http://site.example.com/values.json" ...

*Note*: File URL scheme is also acceptable (e.g, `file:/path/to/job/options/optA.json`).

The value data must be returned in JSON data format described below.

### JSON format

Two styles of return data are supported: simple list and a name/value list. The values will be displayed in a pop-up list when running the Job.  If name/value pairs are returned, then the `name` will be displayed in the list, but the `value` will be used as the input.

*Examples*

Simple List:

    ["x value for test","y value for test"]

This will populate the select menu with the given values.

Name Value List:
 
    [
      {name:"X Label", value:"x value"},
      {name:"Y Label", value:"y value"},
      {name:"A Label", value:"a value"}
    ] 


### Variable expansion in remote URLs

The URL declared for the "valuesUrl" can embed variables which will be
filled with certain job context items when making the remote request. This
helps make the URLs more generic and contextual to the Job.

Two types of expansions are available, Job context, and Option
context.

To include job information in the URL, specify a variable of the form
${job._property_}.

Properties available for Job context:

* `name`: Name of the Job
* `group`: Group of the Job
* `description`: Job description
* `project`: Project name
* `argString`: Default argument string for a job

To include Option information in the URL, specify a variable of the
form ${option._property_}:

Properties available for Option context:

* `name`: Name of the current option

*Examples*

    http://server.com/test?name=${option.name}

Passes the option name as the "name" query parameter to the URL.

    http://server.com/test?jobname=${job.name}&jobgroup=${job.group}

Passes the job name and group as query parameters.

### Remote request failures

If the request for the remote option values fails, then the GUI form
will display a warning message:

![](figures/fig0901.png)
    
In this case, the option will be allowed to use a textfield to set the value.

### Implementations and Examples ###

The following two sections describe examples using simple CGI scripts
that act as option model providers.
 
#### Hudson artifacts option provider 

An end-to-end release process often requires obtaining build artifacts
and publishing them to a central repository for later distribution.
A continuous integration server like [Hudson] makes identifying the
build artifacts a simple Job configuration step. The [Hudson API]
provides a network interface to obtain the list of artifacts from
successful builds via a simple HTTP GET request.

Acme builds its artifacts as RPMs and has confiugred their build job
to identify them. The operations team wants to create Jobs that would
allow them to choose a version of these artifacts generated by the
automated build.

A simple CGI script that requests the information from Hudson and then
generates a [JSON] document is sufficient to accomplish this. The CGI
script can use query paramaters to specify the Hudson server, hudson job
and artifact path. Job writers can then specify the paramaterized URL
to the CGI script to obtain the artifacts list as an options model
and present the results as a menu to Job users.

The code listing below shows the the CGI script essentially does a
call to the [curl] command to retreive the XML document
containing the artifacts information and then parses it using
[xmlstarlet].
 
File listing: hudson-artifacts.cgi
 
    #!/bin/bash
    # Requires: curl, xmlstarlet
    # Returns a JSON list of key/val pairs
    #
    # Query Params and their defaults
    hudsonUrl=https://build.acme.com:4440/job
    hudsonJob=ApplicationBuild
    artifactPath=/artifact/bin/dist/RPMS/noarch/
    
    echo Content-type: application/json
    echo ""
    for VAR in `echo $QUERY_STRING | tr "&" "\t"`
    do
      NAME=$(echo $VAR | tr = " " | awk '{print $1}';);
      VALUE=$(echo $VAR | tr = " " | awk '{ print $2}' | tr + " ");
      declare $NAME="$VALUE";
    done

    curl -s -L -k $hudsonUrl/${hudsonJob}/api/xml?depth=1 | \
      xmlstarlet sel -t -o "{" \
        -t -m "//build[result/text()='SUCCESS']" --sort A:T:L number  \
        -m . -o "&quot;Release" -m changeSet/item -o ' ' -v revision -b \
        -m . -o ", Hudson Build " -v number -o "&quot;:" \
        -m 'artifact[position() = 1]' -o "&quot;" -v '../number' -o $artifactPath -o "{" -b \
        -m 'artifact[position() != last()]' -v 'fileName' -o "," -b \
        -m 'artifact[position() = last()]' -v 'fileName' -o "}&quot;," \
        -t -o "}"

After deploying this script to a CGI enabled directory on the
operations web server, it can be tested directly by requesting it using `curl`.

    curl -d "hudsonJob=anvils&artifactPath=/artifact/bin/dist/RPMS/noarch/" \
        --get http://opts.acme.com/cgi/hudson-artifacts.cgi

The server response should return JSON data resembling the example below:

    [ 
      {name:"anvils-1.1.rpm", value:"/artifact/bin/dist/RPMS/noarch/anvils-1.1.rpm"}, 
      {name:"anvils-1.2.rpm", value:"/artifact/bin/dist/RPMS/noarch/anvils-1.2.rpm"} 
    ]	

Now in place, jobs can request this option data like so:

     <option name="package" enforcedvalues="true" required="true"
        valuesUrl="http://ops.acme.com/cgi/hudson-artifacts.cgi?hudsonJob=anvils"/> 

The RunDeck UI will display the package names in the menu and once
selected the Job will have the path to the build artifact on the
Hudson server.

[Hudson]: http://hudson-ci.org/
[Hudson API]: http://wiki.hudson-ci.org/display/HUDSON/Remote+access+API
[JSON]: http://www.json.org/

#### Yum repoquery option model provider

[Yum] is a great tool for automating [RPM] package management. With Yum,
administrators can publish packages to the repository and then use the
yum client tool to automate the installation of packages along with
their declared dependencies. Yum includes a command
called [repoquery] useful for
querying Yum repositories similarly to rpm queries.

Acme set up their own Yum repository to distribute application release
packages. The Acme administrator wants to provide an option model to Jobs that
need to know what packages provide a given capability.

The code listing below shows it is a simple wrapper around the
repoquery command that formats the results as JSON data.

File listing: yum-repoquery.cgi
    
    #!/bin/bash
    # Requires: repoquery
    # 
    # Query Params and their defaults
    repo=acme-staging
    label="Anvils Release"
    package=anvils
    max=30
    #
    echo Content-type: application/json
    echo ""
    for VAR in `echo $QUERY_STRING | tr "&" "\t"`
    do
      NAME=$(echo $VAR | tr = " " | awk '{print $1}';);
      VALUE=$(echo $VAR | tr = " " | awk '{ print $2}' | tr + " ");
      declare $NAME="$VALUE";
    done

    echo '{'
    repoquery --enablerepo=$repo --show-dupes \
      --qf='"${label} %{VERSION}-%{RELEASE}":"%{NAME}-%{VERSION}-%{RELEASE}",' \
      -q --whatprovides ${package} | sort -t - -k 4,4nr | head -n${max}
    echo '}'

After deploying this script to the CGI enabled directory on the
operations web server, it can be tested directly by requesting it using `curl`.

    curl -d "repo=acme&label=Anvils&package=anvils" \
        --get http://ops.acme.com/cgi/yum-repoquery.cgi
 
The server response should return JSON data resembling the example below:

    TODO: include JSON example
 
Now in place, jobs can request the option model data like so:

     <option name="package" enforcedvalues="true" required="true"
        valuesUrl="http://ops.acme.com/cgi/yum-repoquery.cgi?package=anvils"/> 

The RunDeck UI will display the package names in the menu and once
selected, the Job will have the matching package versions.
 
[Yum]: http://yum.baseurl.org/
[RPM]: http://www.rpm.org/
[repoquery]: http://linux.die.net/man/1/repoquery
 

Resource Editor
------

The Resource Editor integration is a way to link to a third-party system used for managing Node definitions from within the RunDeck Resources listing. Each Node entry in the resources.xml can define a URL to provide an "Edit" link that will appear in the RunDeck Resources listing for that Node.

This allows you to make use of the Resource Model Provider in a more seamless way.  RunDeck will load the resource.xml from the third-party Provider system, and users of RunDeck can click straight to the Editor for those Nodes.  The Provider and the Editor could be the same system, or they could both be custom CGI scripts that integrate with a third system.

Some teams have acquired or developed tools to manage information
about the hosts deployed in their networks. These tools have
interfaces to not just view but also modify the data about these
hosts. Though there is no widely used common standard adopted by users
of these tools, it is possible to map the data to meet the needs of
[RunDeck resource models](#resource-model-provider). 

### Definition ###

The [RunDeck resource model document format](resource-v10.html) provides two attributes belonging to
the <code>node</code> tag that help connect the dots between the
RunDeck UI and the editing interface provided by the external data
management tool. They can use `editUrl` or `remoteUrl` attributes to specify the remote URL.  The URLs can embed properties about the node to expand prior to being loaded, which allows you to e.g. submit query parameters using the node name.

`editUrl`

:    Specifies a URL to a remote site which will allow editing of the Node.  When specified, the Node resource will display an "Edit" link in the RunDeck GUI and clicking it will open a new browser page for the URL.

[`remoteUrl`](#using-remoteurl)

:    Specifies a URL for a remote site which will be loaded in an iframe within a RunDeck page.  Clicking the "Edit" link for the Node will load content from the site within the current RunDeck page, allow you to perform your edit at the remote site, and has optional JavaScript hooks to report the state of the editing process back to the RunDeck page for a more streamlined user interface. 

### Using properties ###

Properties of the Node can be embedded in the URL and expanded prior to use.  The syntax is:

    ${node.property}

Available properties are:

`name`, `hostname`, `os-name`, `os-version`, `os-family`, `os-arch`, `username`, `description`, `tags`, `type`, `project`

You can embed these properties within the url like this:

    http://mycmdb:8080/node/edit?name=${node.name}

### Using remoteUrl ###

Using the `remoteUrl` lets you embed another site into an iframe within the RunDeck page, and optionally allows communication back to the RunDeck page about the state of the editing process.

If you want to embed the remote site without having to make any changes to the remote page content, you can do so simply by specifying the `remoteUrl` to use.  When the user clicks "Edit" the site will load within an iframe, and the user can perform whatever actions on the site are necessary.  After they are done they will have to manually click the "Close" button on the RunDeck page to close the iframe.

If you want the user interface in RunDeck to be more streamlined, you will have to be able to modify the web pages produced by the remote site to add simple Javascript calls to communicate back to the RunDeck page.  The JavaScript hooks are designed to not add much burden to the developer of the remote site or system, so they are optional.

#### Streamlining the interface ####

If the remote site implements some Javascript messaging conforming to a simple optional protocol, then the user interface between RunDeck and the remote site can be made more seamless.

RunDeck lets the remote site inform it when the following steps occur: 

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
    
The first argument to `postMessage` is one of the message codes shown below.  The second argument is the expected "origin", meaning the URL scheme, server and port of the server receiving the message.  You can specify "*" to include any site that may be loading the content, but you may want to restrict it to your RunDeck installation's scheme, hostname and port.

RunDeck can receive the following messages sent by the remote site:

`rundeck:node:edit:started`
  ~ Sent as soon as the remote edit URL is loaded and indicates that the remote Site understands the messaging protocol and has loaded the correct edit page.  You would probably send this on the "edit" or "form" page for the targetted node.

`rundeck:node:edit:error` or `rundeck:node:edit:error:An error message`
  ~ Sent if some error occurs.  The remote editing form will close and the error message (if any) will be shown.  You would probably send this on the "edit" or "view" page if there is an error locating the targetted Node or loading anything required for the edit process.
 
The next two messages are only valid after the "started" message has already been received:

`rundeck:node:edit:finished:true`
  ~ Sent after the remote form has been saved without errors.  This indicates that the editing process is done and has completed with saved changes.  You would probably send this on the "view" or "show" page for the targetted node if the save operation was successful.
  
`rundeck:node:edit:finished:false`
  ~ Sent after the remote form has been either cancelled or discarded without changes.  This indicates that the editing process is done but no changes were made.  You would probably send this on the "view" or "show" for the targetted node (if your site simply shows the node view again) or "list" page (if your site goes back to a list of resources) if the user hits "cancel".
  
Any message not shown here that is received by RunDeck after it has received the "started" message will be considered unexpected and the editing process will close the iframe.

The user will also have the option to close and cancel the remote editing process at any time.

Note that sending the "error" or "finished" message will close the editing session and all subsequent messages will be ignored.

TODO: The JavaScript code to communicate back to RunDeck could be bundled into a simple widget script file for easier inclusion on remote sites.

### Examples ###

Here are some examples of using the `editUrl` and `remoteUrl` in a resources.xml file:

Specify a simple URL for editing, which will simply produce a link:

    <node name="venkman" editUrl="http://mycmdb:8080/node/edit" ... />
   
Specify a URL for editing, with embedded "name" property as a parameter:

    <node name="venkman" editUrl="http://mycmdb:8080/node/edit?name=${node.name}" ... />
   
Specify a remote URL with embedded "name" and "project" properties as parameters:

    <node name="venkman" remoteUrl="http://mycmdb:8080/node/edit?name=${node.name}&amp;project=${node.project}" ... />
   
Specify a remote URL with embedded "name" property as part of the path:

    <node name="venkman" remoteUrl="http://mycmdb:8080/node/edit/${node.name}"  ... />

#### Simple site integration ####

The [ndbtest](https://github.com/gschueler/ndbtest) project on github provides an example of how the remote Resource Editor can integrate with RunDeck using JavaScript.

This project is a simple [Grails](http://grails.org) application which provides a database of Node data.  The standard web-based user flow is:

* List all nodes.
* Edit a Node with the edit page. From here the User can:
    * Cancel the Node changes
        * Goes to the Node show page
    * Save the Node changes
        * Result is successful
            * Goes to the Node show page
        * Result fails, so display an Error message (either on the edit page or the list page)

We want the Node's "edit" link in RunDeck to go directly to an edit page, so the `remoteUrl` for each Node entry then should be a URL to link to this page, for example:

    remoteUrl="http://localhost:8080/node/edit?name=${node.name}&amp;project=${node.project}"

The code below shows that the `name` & `project` are used to select the correct node from the database, even though the built-in identifier is an ID number:

* [NodeController.groovy:51](https://github.com/gschueler/ndbtest/blob/master/grails-app/controllers/com/dtolabs/ndb/NodeController.groovy#L51).

    * Note that if there is no Node found with the specified values, then the response will be to set an error message and then show the list page.

So the JavaScript for integrating with RunDeck is then added to the following pages in this system:

* [node/edit.gsp](https://github.com/gschueler/ndbtest/blob/master/grails-app/views/node/edit.gsp)
    * If an error has occurred, it posts an error message starting on [Line 27](https://github.com/gschueler/ndbtest/blob/master/grails-app/views/node/edit.gsp#L27)
    * Otherwise, it posts the `started` message starting [on line 34](https://github.com/gschueler/ndbtest/blob/master/grails-app/views/node/edit.gsp#L34)
* [node/show.gsp](https://github.com/gschueler/ndbtest/blob/master/grails-app/views/node/show.gsp)
    * If the node save was successful, send the `finished:true` message, starting at [line 21](https://github.com/gschueler/ndbtest/blob/master/grails-app/views/node/show.gsp#L21).
    * Otherwise send the `finished:false` message starting at [line 28](https://github.com/gschueler/ndbtest/blob/master/grails-app/views/node/show.gsp#L28).
* [node/list.gsp](https://github.com/gschueler/ndbtest/blob/master/grails-app/views/node/list.gsp)
    * If an error has occurred, it posts an error message starting on [line 20](https://github.com/gschueler/ndbtest/blob/master/grails-app/views/node/list.gsp#L20).

To complete the round-trip of editing a Node and then showing the results back in RunDeck, the ndbtest project would have to export XML formatted Resource data, and then your RunDeck project.properties file would have to point to the appropriate URL.  (This is left as an exercise to the reader.)