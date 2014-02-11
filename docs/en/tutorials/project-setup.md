
## Planning for the project

The administrator decides to create a project called "anvils."
to manage the activities of the anvils restart. 
The anvils project will contain definitions about the nodes used
by the anvils application, as well as, a set a jobs
that reference these nodes. The administrator will use Rundeck
access control policies to govern which teams have access to
perform each of the procedures.

The administrator decides to formalize the role of each node in
the application stack by introducing tags. Tags will be used within
the Jobs to target each kind of node rather than reference 
specific hosts names or IP addresses. This makes the jobs reusable
across different environments.

The administrator will also use the built in 
[SSH node executor](../plugins-user-guide/ssh-plugins.html) for
the project since SSH is already used to execute remote commands.

## Create the project

The administrator creates the project using the
[rd-project] command line tool. Logged in on the
rundeck server as the user "rundeck", the administrator executes:

~~~~~~~~ {.bash}
rd-project -p anvils --action create
~~~~~~~~

This initializes the "anvils" project in Rundeck.

Of course, the administrator could also create the project 
from the projects menu in the Rundeck GUI.

The project has been created  but contains no jobs and only
one node definition, one entry for the Rundeck server node. 

## Declare node definitions
Modeling the anvils nodes deployed in the live environment is 
administrator's next step.

The anvils application environment has several components spread
across different servers. Anvils is a three tier
application and has web, application and database components,
each component installed on a separate host.

Additionally, the administrator decides to incorporate a recent
convention to use different unix logins to execute commands
to control each functional application component. 
Each component will run using under a separate unix login
to help isolate each component at the system level.

With this information in hand, the administrator prepares the project
resource model using the [resource-XML]
document format. The file listing
below contains the node definitions for the five nodes -- 
www1, www2, app1, app2, db1:

File listing: /var/rundeck/projects/anvils/nodes/resources.xml

~~~~~~~~ {.xml .numberLines}
<?xml version="1.0" encoding="UTF-8"?>
<project>
  <node name="www1.anvils.com" description="A www server node." tags="www" 
    hostname="192.168.50.2" username="www1" 
    osFamily="unix" osName="Linux">
    <attribute name="anvils-location" value="US-East"/>
    <attribute name="anvils-customer" value="acme.com"/>
  </node>
  <node name="www2.anvils.com" description="A www server node." tags="www" 
    hostname="192.168.50.2" username="www2" 
    osFamily="unix" osName="Linux">
    <attribute name="anvils-location" value="US-East"/>
    <attribute name="anvils-customer" value="acme.com"/>
  </node>
  <node name="app1.anvils.com" description="A app server node." tags="app" 
    hostname="192.168.50.2" username="app1" 
    osFamily="unix" osName="Linux"> 
    <attribute name="anvils-location" value="US-East"/>
    <attribute name="anvils-customer" value="acme.com"/>
  </node>
  <node name="app2.anvils.com" description="A app server node." tags="app" 
    hostname="192.168.50.2" username="app2" 
    osFamily="unix" osName="Linux"> 
    <attribute name="anvils-location" value="US-East"/>
    <attribute name="anvils-customer" value="acme.com"/>
  </node>
  <node name="db1.anvils.com" description="A db server node." tags="db" 
    hostname="192.168.50.2" username="db" 
    osFamily="unix" osName="Linux"> 
    <attribute name="anvils-location" value="US-East"/>
    <attribute name="anvils-customer" value="acme.com"/>
  </node>
</project>
~~~~~~~~~~~~~~~~~~~~~~~~~~

Reviewing the XML content one sees the XML data define
several nodes and tags describing the three kinds of application components.

A logical name for each node is defined
with the `name` attribute (eg name="www1.anvils.com"). 
The address used by SSH is set with `hostname`  while the login
used to execute  commands has been specified with the
`username` attribute (username="www1" vs
username="db"). The value for the `tags` attribute
reflects the server role  (tags="web" vs tags="app").

You will also notice there are two ways to define attributes using inline attribute names like `osName` or a separate xml element like `anvils-location`. It's purely up to your preference which format you use.

> Note, this tutorial is a trivial sized example so to conserve space (and VMs) the nodes are located on the same VM (each node uses the same hostname but a different username). 

A node in rundeck can model a single host on the
network and represents a single management endpoint. In the end,
the ssh node executor plugin formulates an ssh command string similar to:
"ssh username@hostname command". The ssh identity is resolved via configuration
at run time. In this example, the project default is used as specified in the
[project.properties] configuraiton file. 
(e.g.,  project.ssh-keypath=/var/lib/rundeck/.ssh/id_rsa).

See [ssh-plugins guide](../plugins-user-guide/ssh-plugins.html#configuring-remote-machine-for-ssh) to learn about configuring remote machines for ssh.

This example uses the built in ssh plugin but you are not restricted to using
ssh. There are other node executor plugins that invoke remote actions via
other tools (eg, salt, mcollective, winrm, chef knife, etc). 



## Configure the model source

To make Rundeck aware of the new resource file containing the anvils nodes, 
the administrator modifies the
[project.properties](../administration/configuration-file-reference.html#project.properties)
configuration file to declare a new resource model source.

Below a directory type source is configured:

~~~~~~~~~~~
resources.source.2.config.directory=/var/rundeck/projects/anvils/nodes
resources.source.2.type=directory
~~~~~~~~~~~~

This is the second source (hence: resource.source.2) as the first one defined
was created by the rd-project create action.
  
New project sources can also be added in the Configure page:

![Anvils model sources](../figures/fig0609.png)

## Managing Node Sources

See [Managing Node Sources](../administration/managing-node-sources.html)
to learn more about configuring Rundeck to read node data from external providers.




[resource-XML]: ../man5/resource-xml.html
[rd-project]: ../man1/rd-project.html
[tip1]: http://www.thegeekstuff.com/2008/11/3-steps-to-perform-ssh-login-without-password-using-ssh-keygen-ssh-copy-id/
