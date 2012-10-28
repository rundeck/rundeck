% SSH

Rundeck by default uses [SSH] for remote execution.
You do _not_ need to have root account access on either the server or
the remote hosts.  

[SSH]: http://en.wikipedia.org/wiki/Secure_Shell

See more about settings up Nodes and Jobs for SSH execution in the section about built-in providers: [SSH Provider](../manual/plugins.html#ssh-provider).

## SSH configuration requirements

* The SSH configuration requires that the Rundeck server machine can
  ssh commands to the client machines. 
* SSH is assumed to be installed and configured appropriately to allow
  this access.   
* SSH can be configured for either *password* based authentication or *public/private key* based authentication.
* For public/private key authentication:
    * There are many resources
available on how to configure ssh to use public key authentication
instead of passwords such as:
[Password-less logins with OpenSSH](http://www.debian-administration.org/articles/152) or [How-To: Password-less SSH](http://www.cs.wustl.edu/~mdeters/how-to/ssh/).
    * If your private key file has a passphrase, each Job definition that will execute on the node must be configured correctly.
* For password authentication:
    * each Node definition must be configured to allow password authentication
    * each Job definition that will use it must be configured correctly

For information on configuring Nodes, Jobs and Password options for jobs, see the [User Manual - Plugins - SSH Provider](../manual/plugins.html#ssh-provider) section.

## SSH key generation

* The Rundeck installation can be configured to use RSA _or_ DSA
  type keys.
  
Here's an example of SSH RSA key generation on a Linux system:

    $ ssh-keygen -t rsa
    Generating public/private rsa key pair.
    Enter file in which to save the key (/home/demo/.ssh/id_rsa): 
    Enter passphrase (empty for no passphrase): 
    Enter same passphrase again: 
    Your identification has been saved in /home/demo/.ssh/id_rsa.
    Your public key has been saved in /home/demo/.ssh/id_rsa.pub.
    The key fingerprint is:
    a7:31:01:ca:f0:62:42:9d:ab:c8:b7:9c:d1:80:76:c6 demo@ubuntu
    The key's randomart image is:
    +--[ RSA 2048]----+
    | .o . .          |
    |.  * . .         |
    |. = =   .        |
    | = E     .       |
    |+ + o   S .      |
    |.o o .   =       |
    |  o +   .        |
    |   +             |
    |                 |
    +-----------------+

## Configuring remote machine for SSH 

To be able to directly ssh to remote machines, the SSH public key of
the client should be shared to the remote machine.
  
Follow the steps given below to enable ssh to remote machines.

The ssh public key should be copied to the `authorized_keys` file of
the remote machine. The public key will be available in
`~/.ssh/id_rsa.pub` file.
  
The `authorized_keys` file should be created in the `.ssh` directory of
the remote machine.
  
The file permission of the authorized key should be read/write for
the user and nothing for group and others. To do this check the
permission and change it as shown below.

    $ cd ~/.ssh
    $ ls -la
    -rw-r--r--   1 raj  staff     0 Nov 22 18:14 authorized_keys

    $ chmod 600 authorized_keys 
    $ ls -la
    -rw-------   1 raj  staff     0 Nov 22 18:14 authorized_keys

The permission for the .ssh directory of the remote machine should
be read/write/execute for the user and nothing for the group and
others. To do this, check the permission and change it as shown
below.  

    $ ls -la
    drwxr-xr-x   2 raj  staff    68 Nov 22 18:19 .ssh
    $ chmod 700 .ssh
    $ ls -la
    drwx------   2 raj  staff    68 Nov 22 18:19 .ssh

If you are running Rundeck on Windows, we heartily recommend using
[Cygwin] on Windows as it includes SSH and a number of
Unix-like tools that are useful when you work in a command line
environment.

[Cygwin]: http://www.cygwin.org


## Passing environment variables through remote command

To pass environment variables through remote command
dispatches, it is required to properly configure the SSH server on the
remote end. See the `AcceptEnv` directive in the "sshd\_config(5)"
manual page for instructions. 

Use a wild card pattern to permit `RD_` prefixed variables to provide
open access to Rundeck generated environment variables.

Example in sshd_config:

    # pass Rundeck variables
    AcceptEnv RD_*
