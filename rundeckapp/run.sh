#!/bin/bash
#JAVA_OPTS="-XX:MaxPermSize=256m -Djava.security.auth.login.config=web-app/WEB-INF/jaas.conf -Xmx512m -Xms256m -server -Dant.home=$ANT_HOME -Drdeck.base=$RDECK_BASE \
JAVA_OPTS="-XX:MaxPermSize=256m -Djava.security.auth.login.config=web-app/WEB-INF/jaas.conf -Xmx512m -Xms256m -server " \
grails "$@"  -Dserver.port=9090  run-app
