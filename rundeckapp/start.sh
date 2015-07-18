#!/bin/bash

export GRAILS_OPTS="-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=5005 -XX:MaxPermSize=256m -Xmx512m -Xms256m -server -Djava.security.auth.login.config=web-app/WEB-INF/jaas.conf -Drundeck.config.location=/Users/daplay/rundeck/rundeck-config.properties -Drdeck.base=/Users/daplay/rundeck/ "
grails -reloading run-app
