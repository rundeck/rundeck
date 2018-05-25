#!/bin/bash

NAME=$1;shift
REST="$@"
cat <<END
<project>
<node name="$NAME" hostname="$NAME" description="docker node" osFamily="unix"
$REST
/>
<node name="${NAME}-stored"
 description="remote node using stored ssh key" 
 ssh-key-storage-path="/keys/id_rsa.pem"
  hostname="${NAME}" 
   osFamily="unix" 
   osName="Linux"
    osArch="x86_64" 
username="${USERNAME}"
tags="remote-stored" 
/>
</project>
END