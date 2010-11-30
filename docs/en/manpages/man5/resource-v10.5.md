% PROJECT-V10(1) RunDeck User Manuals | Version 1.0
% Alex Honor
% November 20, 2010

# NAME

project-v10 - describes the RunDeck resource model document

The `project` XML document declares a resource model that can also be
uploaded to a project hosted on the RunDeck server. This is a
demonstration document using all possible elements.

# Elements

## project

The root (aka "top-level") element of the project.xml file. 
Can contain the following elements:

[node](#node)

:   A node resource element.

[setting](#setting)

:   A setting resource element.

*Example*

    <project>
      <node .../>
      <setting ...  />
    </project>

## node

The node element defines a Node resource.

*Attributes*

type

:   The node type.
    
name

:   The node name. This is a logical identifier from the node. (required)

description

:   A brief description about the node.

hostname

:   The hostname or IP address of the remote host. (required)

osArch

:   The operating system architecture.

osFamily

:   The operating system family (e.g, unix or windows).

osName

:   The operating system name (e.g., Linux or Mac OS X).

tags

:   Comma separated list of filtering tags.

username

:   The username on the remote host to which RunDeck connects.

password

:   The remote connection password.

editUrl

:   URL to an external resource model editor service

remoteUrl

:   URL to an external resource model service

*Nested Elements*

[resources](#resources)

:   Associated resources.


*Examples*

Define a node named "strongbad":

    <node name="strongbad" type="Node"
        description="a development host"
        hostname="strongbad.local"
        osArch="i386" osFamily="unix" osName="Darwin" osVersion="9.2.2"
        username="alexh"/>

Define a node of the type LinuxNode that has a Port as a resource:

    <node type="LinuxNode" name="centos54" hostname="centos54.local"
          description="Sample Linux node" tags="sample,linux"	  
          >
        <resources>
          <resource type="Port" name="httpd"/>
        </resources>
    </node>

Define a node named that uses a non standard SSH port. The "hostname"
value is overloaded to include the port (192.168.1.106:4022):

    <node name="centos54" type="Node"
        description="a centos host"
        hostname="192.168.1.106:4022"
        username="deploy"/>
	
## setting

The setting element defines a Setting resource.

*Attributes*

type	

:    the resource type.

name

:    the setting name (required).

description

:    brief description.

settingType

:     the value type.

settingValue

:     the setting value (required).


*Example*

    <setting type="Port" name=""tomcatListenPort"" 
           description="The port tomcat listens" 
           settingValue="8080" />

## resources

The resources tag defines a set of resource relationships. Each
related resource is referenced via a [resource](#resource) element.

*Nested elements*

[resource](#resource)

:	An associated resource.

*Example*

    <resources>
       <resource type="Port" name="tomcatListenPort" />
    </resources>

## resource

A resource element is used to reference another resource in the
project model by type and name.

*Attributes*

type

:    the resource type.

name	

:    the resource name.

*Example*

Reference a Port setting named "tomcatListenPort".

    <resource type="Port" name="tomcatListenPort" />




