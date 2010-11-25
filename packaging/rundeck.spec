name: rundeck
version: 1.0
release: 0%{?_buildnumber:.%{_buildnumber}}%{?_alphatag:.%{_alphatag}}
license: APL
summary: It Slices, it Dices, it Takes Out Your Garbage
group: System
requires(post): chkconfig
requires(postun): chkconfig
requires: java-1.6.0-openjdk

%description
RunDeck, is no ordinary wooden deck. You can build a bon fire on this deck.
Rundeck provides a single console for dispatching commands across many resources.

%pre
getent group rundeck >/dev/null || groupadd rundeck
getent passwd rundeck >/dev/null || useradd -m -g rundeck rundeck

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
%dir /etc/rundeck/client
%dir /etc/rundeck/server

# Client Configuration
%config /etc/rundeck/client/framework.properties
%config /etc/rundeck/client/admin.aclpolicy
%config /etc/rundeck/client/log4j.properties
%config /etc/rundeck/client/profile
%config /etc/rundeck/client/project.properties

# Server Configuration
%config /etc/rundeck/server/jaas-loginmodule.conf
%config /etc/rundeck/server/log4j.properties
%config /etc/rundeck/server/realm.properties
%config /etc/rundeck/server/rundeck-config.properties

%attr(1775, rundeck, rundeck) %dir /var/log/rundeck
%dir /var/lib/rundeck
%dir /var/lib/rundeck/logs
%dir /var/lib/rundeck/data
%dir /var/lib/rundeck/work
%dir /var/rundeck/projects

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
/var/lib/rundeck/bootstrap/rundeck-server-1.0.0.jar
/var/lib/rundeck/bootstrap/servlet-api-2.5-20081211.jar

# CLI Lib Support
%dir /var/lib/rundeck/cli
/var/lib/rundeck/cli/ant-1.8.1.jar
/var/lib/rundeck/cli/ant-jsch-1.8.1.jar
/var/lib/rundeck/cli/ant-launcher-1.8.1.jar
/var/lib/rundeck/cli/commons-beanutils-1.8.0.jar
/var/lib/rundeck/cli/commons-cli-1.0.jar
/var/lib/rundeck/cli/commons-codec-1.3.jar
/var/lib/rundeck/cli/commons-collections-3.2.1.jar
/var/lib/rundeck/cli/commons-httpclient-3.0.1.jar
/var/lib/rundeck/cli/commons-lang-2.4.jar
/var/lib/rundeck/cli/commons-logging-1.1.jar
/var/lib/rundeck/cli/dom4j-1.6.1.jar
/var/lib/rundeck/cli/jaxen-1.1.jar
/var/lib/rundeck/cli/jsch-0.1.42.jar
/var/lib/rundeck/cli/log4j-1.2.15.jar
/var/lib/rundeck/cli/rundeck-core-1.0.0.jar
/var/lib/rundeck/cli/xerces-2.6.0.jar
/var/lib/rundeck/cli/xml-apis-2.6.0.jar

# CLI Tools
%attr(755, root, root) /usr/bin/run
%attr(755, root, root) /usr/bin/dispatch
%attr(755, root, root) /usr/bin/rd-jobs
%attr(755, root, root) /usr/bin/rd-project
%attr(755, root, root) /usr/bin/rd-queue

%changelog
