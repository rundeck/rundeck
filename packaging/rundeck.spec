name: rundeck
version: %{_version}
release: %{_release}%{?_buildnumber:.%{_buildnumber}}%{?_alphatag:.%{_alphatag}}
license: APL
summary: It Slices, it Dices, it Takes Out Your Garbage
group: System
requires(post): chkconfig
requires(postun): chkconfig
requires: java-1.6.0-openjdk
requires: openssh
requires: rundeck-config

%description
RunDeck, is no ordinary wooden deck. You can build a bon fire on this deck.
Rundeck provides a single console for dispatching commands across many resources.

%changelog
* Thu Jan 13 2011 Greg Schueler <greg@dtosolutions.com> 1.1-0
	- Soft-coded version numbers
* Wed Dec 15 2010 Noah Campbell <noahcampbell@gmail.com> 1.0-1
	- Run the service as the rundeck user.

%pre
getent group rundeck >/dev/null || groupadd rundeck
getent passwd rundeck >/dev/null || useradd -m -g rundeck rundeck
if [ ! -e ~rundeck/.ssh/rundeck.id_rsa ]; then
	su -c "ssh-keygen -q -t rsa -C '' -N '' -f ~rundeck/.ssh/rundeck.id_rsa" rundeck
fi

%post
/sbin/chkconfig --add rundeckd

%preun
if [ "$1" = 0 ]; then
	/sbin/chkconfig --del rundeckd
fi

%files
%defattr(0644, rundeck, rundeck, 0775)

# System Integration
%attr(755, root, root) /etc/rc.d/init.d/rundeckd

%dir /etc/rundeck

%attr(6775, rundeck, rundeck) %dir /var/log/rundeck
%dir /var/lib/rundeck
%dir /var/lib/rundeck/logs
%dir /var/lib/rundeck/data
%dir /var/lib/rundeck/work
%dir /var/rundeck/projects
%dir /tmp/rundeck
/var/lib/rundeck/libext

# Rundeck VAR directory.
%dir /var/lib/rundeck/var
%dir /var/lib/rundeck/var/tmp

# Docs
%doc /usr/share/man

# The Rundeck WebApp.  The Exploded War Goes Here.
%dir /var/lib/rundeck/exp
/var/lib/rundeck/exp/webapp

# Server Bootstrap
%dir /var/lib/rundeck/bootstrap
/var/lib/rundeck/bootstrap/jetty-6.1.21.jar
/var/lib/rundeck/bootstrap/jetty-naming-6.1.21.jar
/var/lib/rundeck/bootstrap/jetty-plus-6.1.21.jar
/var/lib/rundeck/bootstrap/jetty-util-6.1.21.jar
/var/lib/rundeck/bootstrap/rundeck-jetty-server-%{version}.jar
/var/lib/rundeck/bootstrap/servlet-api-2.5-20081211.jar

# CLI Lib Support
%dir /var/lib/rundeck/cli
/var/lib/rundeck/cli/ant-1.7.1.jar
/var/lib/rundeck/cli/ant-jsch-1.7.1.jar
/var/lib/rundeck/cli/ant-launcher-1.7.1.jar
/var/lib/rundeck/cli/commons-beanutils-1.8.0.jar
/var/lib/rundeck/cli/commons-cli-1.0.jar
/var/lib/rundeck/cli/commons-codec-1.5.jar
/var/lib/rundeck/cli/commons-collections-3.2.1.jar
/var/lib/rundeck/cli/commons-httpclient-3.0.1.jar
/var/lib/rundeck/cli/commons-lang-2.4.jar
/var/lib/rundeck/cli/commons-logging-1.1.jar
/var/lib/rundeck/cli/dom4j-1.6.1.jar
/var/lib/rundeck/cli/jaxen-1.1.jar
/var/lib/rundeck/cli/jsch-0.1.45.jar
/var/lib/rundeck/cli/log4j-1.2.16.jar
/var/lib/rundeck/cli/rundeck-core-%{version}.jar
/var/lib/rundeck/cli/xercesImpl-2.6.0.jar
/var/lib/rundeck/cli/snakeyaml-1.9.jar

# CLI Tools
%attr(755, root, root) /usr/bin/run
%attr(755, root, root) /usr/bin/dispatch
%attr(755, root, root) /usr/bin/rd-jobs
%attr(755, root, root) /usr/bin/rd-project
%attr(755, root, root) /usr/bin/rd-queue

%package config
summary: RunDeck configuration package
group: System
requires: rundeck

%description config
All configuration related artifacts are stored in this package.

%files config
%defattr(0644, rundeck, rundeck, 0775)
# Client Configuration
%config(noreplace) /etc/rundeck/framework.properties
%config(noreplace) /etc/rundeck/admin.aclpolicy
%config(noreplace) /etc/rundeck/apitoken.aclpolicy
%config(noreplace) /etc/rundeck/log4j.properties
%config(noreplace) /etc/rundeck/profile
%config(noreplace) /etc/rundeck/project.properties

# Server Configuration
%config(noreplace) /etc/rundeck/jaas-loginmodule.conf
%config(noreplace) /etc/rundeck/realm.properties
%config(noreplace) /etc/rundeck/rundeck-config.properties

# SSL Configuration
%dir /etc/rundeck/ssl
%config /etc/rundeck/ssl/ssl.properties
