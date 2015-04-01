
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

## Project Definitions

In Rundeck 2.4 and earlier, all project definitions and configuration files were stored
on the filesystem.

Starting in Rundeck 2.5, you have the option to store project definitions and
configuration files in the database.

The storage type can be changed by a configuration flag in the `rundeck-config.properties` file:

    rundeck.projectsStorageType=db/filesystem

If you wish to use db storage you must add this configuration entry.  

If you have existing filesystem-based projects, and you start Rundeck 
with the `db` storage type, those projects will be automatically imported to the Database.  
The import process copies the contents of `etc/project.properties`, `readme.md` and `motd.md` (if they exist).
Finally, the `etc/project.properties` file will be renamed on disk to `etc/project.properties.imported`.

The DB storage type also uses the Rundeck **Storage Facility** to store the file contents, which can be
configured to use an Encryption plugin.  See [Storage Facility - Using Encryption](storage-facility.html#using-encryption).

### Configuration file

When using *filesystem* storage type, each Project has a configuration file called 
[project.properties](configuration-file-reference.html#project.properties),
located at this path:

* rpm/deb: /var/rundeck/projects/_project_/etc/project.properties
* launcher: $RDECK_BASE/projects/_project_/etc/project.properties

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



The project setup process generates Project configuration in the server, and
a bootstrap resource model containing information about the rundeck server node.


### Project readme.md

You can create a readme file that welcomes your users and provides an overview about the project.
This readme file can contain markdown text letting you format or embed images.


![Project readme](../figures/fig0203.png)


#### Filesystem based readme

If using the *filesystem* storage type only, you can create the file in the project base directory:

* launcher: $RDECK_BASE/projects/{project}/readme.md
* rpm/deb: /var/rundeck/projects/{project}/readme.md

If using the *db* storage type, you must use the [API](#api-usage).

### Project Nodes

The Resource Model is the set of available Nodes that
Rundeck can dispatch commands to, and their associated metadata. 
Each Rundeck Project has its own Resource Model.

After project creation, an initial resource model will contain
information just about the Rundeck server host and is useful just for
running local commands on the Rundeck server. 
You can browse the project resource model by going to the "Nodes" page.

Node resources have common attributes, such as "hostname". 
One of the more useful attributes
is "tags". A *tag* is a text label that you give to the
Node, perhaps denoting a classification, a role the node plays in the
environment, or group membership. A list of tags can be defined for
a given node. You can also invent your own attributes as a way
to know other details about your nodes. 


It is important to start thinking about node tagging for the nodes you manage
because you will use them later when specifying node filtering
options.

#### Default resources
After initial project setup, the project will have a file resource model source.
The project.properties file will contain a setting specifying this file:

* `project.resources.file`: A local file path to read a resource model document

Typically the path to this default resources file is:

* launcher: $RDECK_BASE/projects/{project}/etc/resources.xml
* deb/rpm: /var/rundeck/projects/{project}/etc/resources.xml

It's possible to configure multiple  "Resource Model Sources" for a project
to retrieve additional Resource Model content from other sources. 

You can configure Rundeck to retrieve and store resource model data
from any source, so long as it can produce one of the Rundeck resource model
document formats. (See 
[Resource Model Document formats](../plugins-user-guide/resource-model-source-plugins.html#resource-model-document-formats).) 

See [Resource Model Sources](managing-node-sources.html#resource-model-source).

##### Defining a node

Here's a sample XML document that defines a node called "orion":

~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~ {.xml}
<project>
  <node name="orion" 
    description="a foodazzler service host" tags="staging,us-east" 
    osFamily="unix" osName="Linux"
    hostname="orion"  username="alexh" 
    />
</project>
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~ 

The `node` element has a few
required such as `name`, `osFamily` and `tags`. 

You can add any number of nodes in this document. Here's a second node
called homestar:

~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~ {.xml}
<project>
  <node name="orion" 
    description="a foodazzler service host" tags="staging,us-east" 
    osFamily="unix" osName="Linux"
    hostname="orion"  username="alexh" 
    />
  <node name="homestar" 
    description="a humdinger" tags="integration,us-west"
    osFamily="unix" hostname="192.168.1.02"  username="alexh">
    <attribute name="flavor" value="medium"/>
    <attribute name="package:version" value="2.0"/>
  </node>
</project>
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~ 

The `hostname` and `username` values are used for the SSH connection
while the `name` and `type` are used to define Node identity in the
resource model. It is possible to overload the hostname value to include
port information (eg, hostname="somehost:2022"). 
This is useful if your run SSH on a different port. 


You can also tell there are two different ways to declare an attribute
using the XML format.
The "flavor" attribute is defined as a separate XML element:

    <attribute name="flavor" value="medium"/>


### External Resource Model Sources

Chances are you maintain information about your hosts within
another tool, perhaps Chef, Puppet, Nagios, Amazon EC2, RightScale or
even an in-house database. One of these tools might be
considered the authority of knowledge about the nodes
deployed in your network. 
Therefore, it is best to create an interface
to the authoritative tool and expose it as Rundeck a resource model source. This
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
Here the default SSH key setting is declared via the `project.ssh-keypath`:

~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~ {.bash}
rd-project -a create -p examples --project.ssh-keypath=/home/rundeck/.ssh/id_rsa
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~ 

You can specify a resource model source using command options, too.
Here a "directory" model source is declared.

~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~ {.bash}
rd-project -a create -p examples \
  --resources.source.2.type=directory \
  --resources.source.2.config.directory=/path/to/my/resources.d
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

Administrators can place multiple resource model files in this directory.


[dispatch]: ../man1/dispatch.html
[rd-project]: ../man1/rd-project.html

## API Usage

All Project creation, configuration, deletion, etc can be achieved via the [API](../api/index.html).

[Create projects](../api/index.html#project-creation):

    POST /api/13/projects
    Content-Type: application/json

    { "name": "myproject", "config": { "propname":"propvalue" } }

[Delete projects](../api/index.html#project-deletion):

    DELETE /api/13/project/myproject

[Project configuration](../api/index.html#project-configuration)

    PUT /api/13/project/myproject/config
    Content-Type: application/json

    {
        "key":"value",
        "key2":"value2..."
    }

[Project readme/motd modification](../api/index.html#project-readme-file)

    PUT /api/13/project/myproject/readme.md
    Content-Type: text/plain

    This project manages [acme-guitars.com](http://acme-guitars.com).
