% RESOURCE-YAML 
% Greg Schueler
% February 25, 2011

# NAME

resource-yaml-v13 - describes the Rundeck resource model document Yaml format

The `resource` YAML document declares a resource model used by a Rundeck Project to define the set of Nodes that are available.

See <http://yaml.org> for more information about YAML.

## Structure

The resource format supports two structures: either a Yaml Sequence of [Node definitions](#node-definition), or a Yaml Map of node names with [Node definitions](#node-definition) as values.

Example:

~~~~~~~~ {.yaml}
- somenode: ...
  ...
- somenode2: ...
~~~~~~~~ 

OR

~~~~~~~~ {.yaml}
somenode:
   hostname: ...
   ...
somenode2:
   hostname: ...
~~~~~~~~ 

In the second case, the `nodename` entry is not required.

## Node Definition

A Node definition consists of a Map with some required and some optional entries.

Required Entries:

`nodename`

:    Name of the node, this must be a unique Identifier across nodes within a project

`hostname`

:    Hostname of the node.  This can be any IP Address or hostname to address the node.
     Since hostname is used to make SSH connections, it is possible to overload the value
     to include port number. (eg, hostname:port).

`username`

:    User name to connect to the node via SSH.

Optional Entries:

`description`

:    A description of the Node

`tags`

:    A literal with comma-separated tag strings, or a Sequence of literals. Tags are used for filtering nodes and often represent server role, environment, class, or group.

`osFamily`

:    OS Family

`osArch`

:    OS Architecture

`osName`

:    OS Name

`osVersion`

:    OS Version

`editUrl`

:    URL to an external resource model service.

`remoteUrl`

:    URL to an external resource model editor service.

*Anything*

:    Specify any custom attributes about node using a string value.

* For more information about using the `editUrl`/`remoteUrl` see the [Rundeck User Manual](../administration/managing-node-sources.html#resource-editor).

## Examples

Here's a node, Venkman.local, described with several of the required and optional
attributes discussed above.

~~~~~~~~ {.yaml}
Venkman.local:
  description: Rundeck server node
  hostname: Venkman.local
  nodename: Venkman.local
  osArch: x86_64
  osFamily: unix
  osName: Mac OS X
  osVersion: 10.6.6
  tags: ''
  username: greg
~~~~~~~~ 

Tags are very useful for node filtering and can be used
to describe the role, environment, group, or class to which a node belongs.
The Homestar.local example below describes a node that plays the role of a redis server in the
production environment. 

~~~~~~~~ {.yaml}
Homestar.local:
  description: The production redis server.
  hostname: Homestar.local
  nodename: Homestar.local
  osArch: x86_64
  osFamily: unix
  osName: Linux
  tags: 'redis_server,production'
  username: greg
~~~~~~~~ 

Here's a node, bartholemew, that specifies some custom attributes (app-port, https-port):

~~~~~~~~ {.yaml}
bartholemew:
  description: Webapp node
  hostname: bartholemew
  nodename: bartholemew
  tags: 'web,app'
  username: greg
  app-port: 8080
  https-port: 8443
~~~~~~~~ 

Here is the same two nodes but in the sequence style syntax:

~~~~~~~~ {.yaml}
- Homestar.local:
  description: The production redis server.
  hostname: Homestar.local
  nodename: Homestar.local
  tags: 'redis_server,production'
  username: greg
- bartholemew:
  description: Webapp node
  hostname: bartholemew
  nodename: bartholemew
  tags: 'web,app'
  username: greg
  app-port: 8080
  https-port: 8443
~~~~~~~~ 


The Rundeck source code and all documentation may be downloaded from
<https://github.com/rundeck/rundeck/>.
