% RESOURCE-YAML-V12(5) RunDeck User Manuals | Version 1.2
% Greg Schueler
% February 25, 2011

# NAME

resource-yaml-v12 - describes the RunDeck resource model document Yaml format

The `resource` YAML document declares a resource model used by a RunDeck Project to define the set of Nodes that are available.

See <http://yaml.org> for more information about YAML.

## Structure

The resource format supports two structures: either a Yaml Sequence of [Node definitions](#node-definition), or a Yaml Map of node names with [Node definitions](#node-definition) as values.

Example:

    - somenode: ...
      ...
    - somenode2: ...

OR

    somenode:
       hostname: ...
       ...
    somenode2:
       hostname: ...

In the second case, the `nodename` entry is not required.

## Node Definition

A Node definition consists of a Map with some required and some optional entries.

Required Entries:

`nodename`

:    Name of the node, this must be a unique Identifier across nodes within a project

`hostname`

:    Hostname of the node.  This can be any IP Address or hostname to address the node.

`username`

:    User name to connect to the node via SSH.

Optional Entries:

`description`

:    A description of the Node

`tags`

:    A literal with comma-separated tag strings, or a Sequence of literals

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

* For more information about using the `editUrl`/`remoteUrl` see the [RunDeck Guide](RunDeck-Guide.html#resource-editor).

## Examples

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


The RunDeck source code and all documentation may be downloaded from
<https://github.com/dtolabs/rundeck/>.
