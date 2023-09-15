#!/bin/bash
HOME=/usr/local/tomcat/webapps/rundeck/rundeck



exec bash -c "catalina.sh run | tee /usr/local/tomcat/webapps/rundeck/rundeck/var/log/service.log"