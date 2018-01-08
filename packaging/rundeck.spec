name: rundeck
version: %{_version}
release: %{_release}%{?_buildnumber:.%{_buildnumber}}%{?_alphatag:.%{_alphatag}}
license: APL
summary: It Slices, it Dices, it Takes Out Your Garbage
group: System
requires(post): chkconfig
requires(preun): chkconfig
requires(postun): initscripts
requires: openssh
requires: rundeck-config

%description
RunDeck, is no ordinary wooden deck. You can build a bon fire on this deck.
Rundeck provides a single console for dispatching commands across many resources.

%changelog
* Tue Nov 22 2016 Greg Schueler <greg@simplifyops.com> 2.7.0-0
	- Remove old CLI tools
* Wed Nov 16 2016 Greg Schueler <greg@simplifyops.com> 2.7.0-0
	- Upgraded jetty jar dependencies
* Tue Jan 20 2015 Greg Schueler <greg@simplifyops.com> 2.5-0
	- Fix cli jar file versions, fix changelog date
* Sat Apr 19 2014 Diomidis Spinellis <dds@aueb.gr> 2.0-4
	- Correct .ssh permissions #743
* Mon Dec 2 2013 Alex Honor <alexhonor@yahoo.com> 2.0-0
	- Remove java dependency. #601
* Sun Jan 6 2013 Jordi Llonch <llonchj@gmail.com> 1.4-0
	- Soft-coded version numbers
* Thu Jan 13 2011 Greg Schueler <greg@dtosolutions.com> 1.1-0
	- Soft-coded version numbers
* Wed Dec 15 2010 Noah Campbell <noahcampbell@gmail.com> 1.0-1
	- Run the service as the rundeck user.

%pre
getent group rundeck >/dev/null || groupadd rundeck
getent passwd rundeck >/dev/null || useradd -d /var/lib/rundeck -m -g rundeck rundeck

%post
if [ ! -e ~rundeck/.ssh/id_rsa ]; then
	su -c "ssh-keygen -q -t rsa -C '' -N '' -f ~rundeck/.ssh/id_rsa" rundeck
fi
/sbin/chkconfig --add rundeckd

%preun
if [ "$1" = 0 ]; then
    /sbin/service rundeckd stop >/dev/null 2>&1
	/sbin/chkconfig --del rundeckd
fi
%postun
if [ "$1" -ge "1" ] ; then
    /sbin/service rundeckd condrestart >/dev/null 2>&1 || :
fi

%files
%defattr(0644, rundeck, rundeck, 0775)

# System Integration
%attr(755, root, root) /etc/rc.d/init.d/rundeckd

%dir /etc/rundeck

%attr(6775, rundeck, rundeck) %dir /var/log/rundeck
%attr(0755, rundeck, rundeck) %dir /var/lib/rundeck
%attr(0700, rundeck, rundeck) %dir /var/lib/rundeck/.ssh
%dir /var/lib/rundeck/logs
%dir /var/lib/rundeck/data
%dir /var/lib/rundeck/work
%dir /var/rundeck/projects
%dir /tmp/rundeck
/var/lib/rundeck/libext

# Rundeck VAR directory.
%dir /var/lib/rundeck/var
%dir /var/lib/rundeck/var/tmp
%dir /var/lib/rundeck/var/tmp/pluginJars

# The Rundeck WebApp.  The Exploded War Goes Here.
%dir /var/lib/rundeck/exp
/var/lib/rundeck/exp/webapp

# Server Bootstrap
%dir /var/lib/rundeck/bootstrap
/var/lib/rundeck/bootstrap/javax.servlet-3.0.0.v201112011016.jar
/var/lib/rundeck/bootstrap/jetty-all-9.0.7.v20131107.jar
/var/lib/rundeck/bootstrap/jetty-security-9.0.7.v20131107.jar
/var/lib/rundeck/bootstrap/jetty-server-9.0.7.v20131107.jar
/var/lib/rundeck/bootstrap/jetty-util-9.0.7.v20131107.jar
/var/lib/rundeck/bootstrap/jetty-io-9.0.7.v20131107.jar
/var/lib/rundeck/bootstrap/jetty-http-9.0.7.v20131107.jar
/var/lib/rundeck/bootstrap/jetty-jaas-9.0.7.v20131107.jar
/var/lib/rundeck/bootstrap/log4j-1.2.16.jar
/var/lib/rundeck/bootstrap/jna-3.2.2.jar
/var/lib/rundeck/bootstrap/libpam4j-1.5.jar
/var/lib/rundeck/bootstrap/rundeck-jetty-server-%{_vname}.jar
/var/lib/rundeck/bootstrap/not-yet-commons-ssl-0.3.17.jar

# CLI Lib Support
%dir /var/lib/rundeck/cli
/var/lib/rundeck/cli/ant-1.8.3.jar
/var/lib/rundeck/cli/ant-jsch-1.8.3.jar
/var/lib/rundeck/cli/ant-launcher-1.8.3.jar
/var/lib/rundeck/cli/commons-beanutils-1.9.3.jar
/var/lib/rundeck/cli/commons-cli-1.0.jar
/var/lib/rundeck/cli/commons-codec-1.5.jar
/var/lib/rundeck/cli/commons-collections-3.2.2.jar
/var/lib/rundeck/cli/commons-httpclient-3.0.1.jar
/var/lib/rundeck/cli/commons-lang-2.6.jar
/var/lib/rundeck/cli/commons-logging-1.2.jar
/var/lib/rundeck/cli/dom4j-2.1.0.jar
/var/lib/rundeck/cli/guava-15.0.jar
/var/lib/rundeck/cli/jaxen-1.1.6.jar
/var/lib/rundeck/cli/jna-4.1.0.jar
/var/lib/rundeck/cli/jna-platform-4.1.0.jar
/var/lib/rundeck/cli/jsch.agentproxy.connector-factory-0.0.9.jar
/var/lib/rundeck/cli/jsch.agentproxy.core-0.0.9.jar
/var/lib/rundeck/cli/jsch.agentproxy.jsch-0.0.9.jar
/var/lib/rundeck/cli/jsch.agentproxy.pageant-0.0.9.jar
/var/lib/rundeck/cli/jsch.agentproxy.sshagent-0.0.9.jar
/var/lib/rundeck/cli/jsch.agentproxy.usocket-jna-0.0.9.jar
/var/lib/rundeck/cli/jsch.agentproxy.usocket-nc-0.0.9.jar
/var/lib/rundeck/cli/jsch-0.1.54.jar
/var/lib/rundeck/cli/log4j-1.2.17.jar
/var/lib/rundeck/cli/rundeck-core-%{_vname}.jar
/var/lib/rundeck/cli/rundeck-storage-api-%{_vname}.jar
/var/lib/rundeck/cli/rundeck-storage-conf-%{_vname}.jar
/var/lib/rundeck/cli/rundeck-storage-data-%{_vname}.jar
/var/lib/rundeck/cli/snakeyaml-1.9.jar
/var/lib/rundeck/cli/jackson-annotations-2.8.10.jar
/var/lib/rundeck/cli/jackson-core-2.8.10.jar
/var/lib/rundeck/cli/jackson-databind-2.8.10.jar
/var/lib/rundeck/cli/converter-jackson-2.2.0.jar
/var/lib/rundeck/cli/okhttp-3.6.0.jar
/var/lib/rundeck/cli/okio-1.11.0.jar
/var/lib/rundeck/cli/retrofit-2.2.0.jar



# CLI Tools
%attr(755, root, root) /usr/bin/rd-acl

%package config
summary: RunDeck configuration package
group: System
requires: rundeck

%description config
All configuration related artifacts are stored in this package.

%pre config
getent group rundeck >/dev/null || groupadd rundeck
getent passwd rundeck >/dev/null || useradd -d /var/lib/rundeck -m -g rundeck rundeck

%files config
%defattr(0640, rundeck, rundeck, 0750)
# Client Configuration
%config(noreplace) /etc/rundeck/framework.properties
%config(noreplace) /etc/rundeck/admin.aclpolicy
%config(noreplace) /etc/rundeck/apitoken.aclpolicy
%config(noreplace) /etc/rundeck/log4j.properties
%config(noreplace) /etc/rundeck/cli-log4j.properties
%config(noreplace) /etc/rundeck/profile
%config(noreplace) /etc/rundeck/project.properties

# Server Configuration
%config(noreplace) /etc/rundeck/jaas-loginmodule.conf
%config(noreplace) /etc/rundeck/realm.properties
%config(noreplace) /etc/rundeck/rundeck-config.properties

# SSL Configuration
%dir /etc/rundeck/ssl
%config /etc/rundeck/ssl/ssl.properties

%post config
# TODO: put hostname in framework.properties and rundeck-config.properties
# generate server uuid
DIR=/etc/rundeck
if  ! grep -E '^\s*rundeck.server.uuid\s*=\s*.{8}-.{4}-.{4}-.{4}-.{12}\s*$' $DIR/framework.properties ; then
    uuid=$(uuidgen)
    echo -e "\n# ----------------------------------------------------------------" >> $DIR/framework.properties
    echo "# Auto generated server UUID: $uuid" >> $DIR/framework.properties
    echo "# ----------------------------------------------------------------" >> $DIR/framework.properties
    echo "rundeck.server.uuid = $uuid" >> $DIR/framework.properties
fi
