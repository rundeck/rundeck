#!/bin/bash
JAVA_OPTS=" -Dserver.port=9090 -Djava.security.auth.login.config=web-app/WEB-INF/jaas.conf -Xmx512m -Xms256m -server -Dant.home=$ANT_HOME -Drdeck.base=$RDECK_BASE  \
grails prod run-app
