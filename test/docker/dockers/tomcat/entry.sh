#!/bin/bash
HOME=/usr/local/tomcat/webapps/rundeck/rundeck

mkdir -p $HOME/etc

API_KEY=${API_KEY:-letmein99}
cat > $HOME/etc/tokens.properties <<END
admin: $API_KEY
END

PROJ=test
echo "setup test project: $PROJ in dir $DIR"
mkdir -p $HOME/projects/$PROJ/etc
cat >$HOME/projects/$PROJ/etc/project.properties<<END
project.name=test
project.nodeCache.delay=30
project.nodeCache.enabled=true
project.ssh-authentication=privateKey
#project.ssh-keypath=
resources.source.1.config.file=$HOME/projects/\${project.name}/etc/resources.xml
resources.source.1.config.format=resourcexml
resources.source.1.config.generateFileAutomatically=true
resources.source.1.config.includeServerNode=true
resources.source.1.config.requireFileExists=false
resources.source.1.type=file
service.FileCopier.default.provider=jsch-scp
service.NodeExecutor.default.provider=jsch-ssh
END


exec bash -c "catalina.sh run | tee /usr/local/tomcat/webapps/rundeck/rundeck/var/log/service.log"