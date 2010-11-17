name: rundeck
version: 1.0
release: 0
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

%dir /var/log/rundeck
%dir /var/run/rundeck
%dir /var/run/rundeck/logs
%dir /var/run/rundeck/data
%dir /var/run/rundeck/work
%dir /var/rundeck/projects

# The Rundeck WebApp.  The Exploded War Goes Here.
%dir /var/run/rundeck/exp
/var/run/rundeck/exp/webapp

# Server Bootstrap
%dir /var/run/rundeck/lib
/var/run/rundeck/lib/jetty-6.1.21.jar
/var/run/rundeck/lib/jetty-naming-6.1.21.jar
/var/run/rundeck/lib/jetty-plus-6.1.21.jar
/var/run/rundeck/lib/jetty-util-6.1.21.jar
/var/run/rundeck/lib/rundeck-server-1.0.0.jar
/var/run/rundeck/lib/servlet-api-2.5-20081211.jar

# CLI Lib Support
%dir /var/lib/rundeck/
/var/lib/rundeck/ant-1.8.1.jar
/var/lib/rundeck/ant-jsch-1.8.1.jar
/var/lib/rundeck/ant-launcher-1.8.1.jar
/var/lib/rundeck/commons-beanutils-1.8.0.jar
/var/lib/rundeck/commons-cli-1.0.jar
/var/lib/rundeck/commons-codec-1.3.jar
/var/lib/rundeck/commons-collections-3.2.1.jar
/var/lib/rundeck/commons-httpclient-3.0.1.jar
/var/lib/rundeck/commons-lang-2.4.jar
/var/lib/rundeck/commons-logging-1.1.jar
/var/lib/rundeck/dom4j-1.6.1.jar
/var/lib/rundeck/jaxen-1.1.jar
/var/lib/rundeck/jsch-0.1.42.jar
/var/lib/rundeck/log4j-1.2.15.jar
/var/lib/rundeck/rundeck-core-1.0.0.jar
/var/lib/rundeck/xerces-2.6.0.jar
/var/lib/rundeck/xml-apis-2.6.0.jar




%changelog
