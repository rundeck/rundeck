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

#trap 'die $? "*** bootstrap failed. ***"' ERR

set -o nounset -o pipefail

REPO_URL=$1
API_KEY=$2


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

#configure static api key
echo "admin=${API_KEY}" >> /etc/rundeck/keys.properties
echo "rundeck.tokens.file=/etc/rundeck/keys.properties" >> /etc/rundeck/framework.properties
chown rundeck:rundeck /etc/rundeck/keys.properties
chown -R rundeck:rundeck /var/rundeck/projects

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
        if ! grep  "Started SelectChannelConnector@" /var/log/rundeck/service.log
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
        if ! grep  "Started SelectChannelConnector@" /var/log/rundeck/service.log
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

ls /var/lib/rundeck/data/rundeckdb.h2.db || die "Rundeck data file not found at /var/lib/rundeck/data/rundeckdb.data.db"

