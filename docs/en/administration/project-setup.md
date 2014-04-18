
## Overview 

A *project* is a place to separate management activity.
All Rundeck activities occur within the context of a project.
Multiple projects can be maintained on the same Rundeck server.
Projects are independent from one another, so you can use them to
organize unrelated systems within a single Rundeck
installation. This can be useful for managing different infrastructures,
environments or applications.
Projects can be archived and imported to other Rundeck servers to help
promote changes between environments.

A new installation will not contain any projects so Rundeck will present
you with a dialog to create one. 

## Project Setup 

A project can be set up either from the graphical console or using the
[rd-project] shell tool.


In the graphical console, you will notice a Project
menu in the top navigation bar. If no projects exist, you will be prompted to 
create a new project.

Press the "New Project" button to create a project.  Project names can contain letters and numbers but do not use spaces or special characters.

![Create project prompt](../figures/fig0203-a.png)


After entering your project name, Rundeck initializes it and returns
you to the "Jobs" page.

Projects can be created at any time by going back to the Project menu 
and selecting the "Create a new project..." item.

![Create project menu](../figures/fig0203.png)


The project setup process generates Project configuration in the server, and
a bootstrap resource model containing information about the rundeck server node.

![Run page after new project](../figures/fig0204.png)

The server host is  distinguished with the word "server" in red text.

### Resource model setup

The Resource Model is the set of available Nodes that
Rundeck can dispatch commands to, and their associated metadata. 
Each Rundeck Project has its own Resource Model.

The initial resource model will contain
information just about the Rundeck server host and is useful just for
running local commands on the Rundeck server. 
You can browse the project resource model by going to the "Jobs" page.


Node resources have standard properties, such as "hostname" but these
can be extended via attributes. One of the more useful attributes
is "tags". A *tag* is a text label that you give to the
Node, perhaps denoting a classification, a role the node plays in the
environment, or group membership. A list of tags can be defined for
a given node. 

The output above shows the "strongbad" node currently has an empty
tags property: `tags: ''`. 

It is important to start thinking about node tagging for the nodes you manage
because you will use them later when specifying node filtering
options.

Each Project has a configuration file called 
[project.properties](../administration/configuration-file-reference.html#project.properties),
located at this path:

* rpm/deb: /var/rundeck/projects/_project_/etc/project.properties
* launcher: $RDECK_BASE/projects/_project_/etc/project.properties

This configuration file contains two basic properties for accessing and
storing resource model data:

* `project.resources.file`: A local file path to read a resource model document

In addition, multiple pluggable "Resource Model Sources" can be configured for a project
to retrieve additional Resource Model content from other sources. 
See [Resource Model Sources](managing-node-sources.html#resource-model-source).

You can configure Rundeck to retrieve and store resource model data
from any source, so long as it can produce one of the Rundeck resource model
document formats. (See 
[Resource Model Document formats](../plugins-user-guide/resource-model-source-plugins.html#resource-model-document-formats).) 

Here's the XML document stored for the "examples" project that corresponds
to the output printed by the `dispatch -v` shown earlier:

~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~ {.xml}
<project>
  <node name="strongbad" 
    description="the Rundeck server host" tags="" 
    osArch="x86_64" osFamily="unix" osName="Mac OS X" osVersion="10.6.2"
    hostname="strongbad"  username="alexh" />
</project>
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~ 

You'll notice a single node entry for "strongbad". 
The `node` element has a few
required such as `name`, `osFamily` and `tags`. 

Additional node descriptors can be
added by defining new `node` elements inside the `project` root element. 

The strongbad host does not have any tags defined for it. One or
more tags can be defined. Use comma for the delimiter (e.g, `tags="tag1,tag2"`).

Here's an example of a node called "homestar" with tags defined: 

~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~ {.xml}
<node name="homestar" 
  tags="tag1,tag2"
  osFamily="unix"
  hostname="192.168.1.02" 
  username="alexh" />
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~ 

The `hostname` and `username` values are used for the SSH connection
while the `name` and `type` are used to define Node identity in the
resource model. It is possible to overload the hostname value to include
port information (eg, hostname="somehost:2022"). 
This is useful if your run SSH on a different port. 
	  
Chances are you maintain information about your hosts within
another tool, perhaps Chef, Puppet, Nagios, Amazon EC2, RightScale or
even an in-house database. One of these tools might be
considered the authority of knowledge about the nodes
deployed in your network. Therefore, it is best to create an interface
to the authoritative tool and expose it as Rundeck URL resource model source. This
can be done as a simple CGI script that does a transformation from
the tool's format to the one Rundeck understands. You can also
develop a plugin to interface with the external source.

Of course, a rudimentary alternative is to maintain this information
as an XML document, storing it in a source repository that is
periodically exported to Rundeck. This method would be practical if
your host infrastructure infrequently changes.

Check the Rundeck web site for URL resource model sources. If you are
interested in creating your own, see the
[Resource model source](managing-node-sources.html#resource-model-source) chapter.

### Resource Model Document formats

Rundeck currently has two resource model document formats built in: 

* XML: [resource-XML](../man5/resource-xml.html).  Format name: `resourcexml`.
* Yaml: [resource-YAML](../man5/resource-yaml.html). Format name: `resourceyaml`.

You can enable more formats using [Resource Format Plugins](../plugins-user-guide/resource-model-source-plugins.html).

### Pluggable Resource Model Sources

Each project can have multiple sources for Resource model information, and
you can use or write plugins to enable new sources for entries in the Resource model.

You can configure the sources via the GUI from the Admin page, see
[Configure Page](../manual/configure.html), 
or by modifying the project.properties file.

## Related Command Line Tools

The [rd-project] shell tool can also be used to create a
project.

On the Rundeck server, execute the `rd-project` command and
specify a project name, here we use "examples":

~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~ {.bash}
rd-project -a create -p examples
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~ 

You can also add configuration properties when you create the project.
Here the `project.ssh-keypath` property is specified:

~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~ {.bash}
rd-project -a create -p examples --project.ssh-keypath=/private/ssh.key
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~ 

In the shell, you can list the Node resources in a resource
model using the shell tool, `dispatch`. 
Specify project name using the `-p project` option.

Here the `dispatch` command lists the registered server for
the "examples" project after the project setup. The `-v` gives
a verbose listing that includes more detail:

~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~ {.bash}
$ dispatch -p examples -v 
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~ 

     strongbad:
        hostname: strongbad
        osArch: x86_64
        osFamily: unix
        osName: Mac OS X
        osVersion: 10.6.2
        tags: ''

[dispatch]: ../man1/dispatch.html
[rd-project]: ../man1/rd-project.html

