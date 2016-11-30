#!/usr/bin/env bash

die() {
   [[ $# -gt 1 ]] && { 
	    exit_status=$1
        shift        
    } 
    local -i frame=0; local info= 
    while info=$(caller $frame)
    do 
        local -a f=( $info )
        [[ $frame -gt 0 ]] && {
            printf >&2 "ERROR in \"%s\" %s:%s\n" "${f[1]}" "${f[2]}" "${f[0]}"
        }
        (( frame++ )) || :; #ignore increment errors (i.e., errexit is set)
    done

    printf >&2 "ERROR: $*\n"

    exit ${exit_status:-1}
}
setup_project(){
        local FARGS=("$@")
        local DIR=${FARGS[0]}
        local PROJ=${FARGS[1]}
        mkdir -p $DIR/projects/$PROJ/etc
        cat >$DIR/projects/$PROJ/etc/project.properties<<END
project.name=$PROJ
project.nodeCache.delay=30
project.nodeCache.enabled=true
project.ssh-authentication=privateKey
#project.ssh-keypath=
resources.source.1.config.file=$DIR/projects/\${project.name}/etc/resources.xml
resources.source.1.config.format=resourcexml
resources.source.1.config.generateFileAutomatically=true
resources.source.1.config.includeServerNode=true
resources.source.1.config.requireFileExists=false
resources.source.1.type=file
service.FileCopier.default.provider=jsch-scp
service.NodeExecutor.default.provider=jsch-ssh
END
}

#trap 'die $? "*** bootstrap failed. ***"' ERR

set -o nounset -o pipefail

REPO_URL=$1
shift

#get the ci repo
curl -# --fail -L -o /etc/yum.repos.d/bintray.repo "$REPO_URL" || die "failed downloading bintray.repo"

curl -# --fail -L -O http://dl.fedoraproject.org/pub/epel/6/x86_64/epel-release-6-8.noarch.rpm || die "failed downloading epel-release-6-8"

rpm -Uvh epel-release-6-8.noarch.rpm

yum check-update

# Install the JRE

#yum -y install java-1.6.0
yum -y install java-1.7.0-openjdk jq

# Install Rundeck core

# if local rpms exist, install those
ls /vagrant/rundeck-*.rpm >/dev/null
if [ $? -eq 0 ] ; then
    rpm -i /vagrant/rundeck-*.rpm
else
    yum -y install rundeck
fi

# Add vagrant user to rundeck group
usermod -g rundeck vagrant
# Add rundeck user to vagrant group
#usermod -g vagrant rundeck

# Disable the firewall so we can easily access it from the host
service iptables stop
#iptables -A INPUT -p tcp --dport 4440 -j ACCEPT
#service iptables save

# create test project
setup_project /var/rundeck test
chown -R rundeck:rundeck /var/rundeck/projects
cat >/etc/sysconfig/rundeckd <<END
LANG=en_US.UTF-8
RDECK_JVM_SETTINGS="-Dfile.encoding=utf-8 -Xmx1024m -Xms256m -XX:MaxPermSize=256m -server"
END
# Start up rundeck
mkdir -p /var/log/vagrant
if ! /etc/init.d/rundeckd status
then
    (
        exec 0>&- # close stdin
        /etc/init.d/rundeckd start 
    ) &> /var/log/vagrant/bootstrap.log # redirect stdout/err to a log.

    let count=0
    while true
    do
        if ! grep  "Started ServerConnector@" /var/log/rundeck/service.log
        then  printf >&2 ".";# progress output.
        else  break; # successful message.
        fi
        let count=$count+1;# increment attempts
        [ $count -eq 18 ] && {
            echo >&2 "FAIL: Execeeded max attemps "
            exit 1
        }
        sleep 10
    done
else
    let count=0
    while true
    do
        if ! grep  "Started ServerConnector@" /var/log/rundeck/service.log
        then  printf >&2 ".";# progress output.
        else  break; # successful message.
        fi
        let count=$count+1;# increment attempts
        [ $count -eq 18 ] && {
            echo >&2 "FAIL: Execeeded max attemps "
            exit 1
        }
        sleep 10
    done

fi

# test data file is in correct location

ls /var/lib/rundeck/data/rundeckdb.mv.db || die "Rundeck data file not found at /var/lib/rundeck/data/rundeckdb.mv.db"

