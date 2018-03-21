# original https://hub.docker.com/r/bwits/rundeck-build/
FROM centos:6
MAINTAINER Bill W
RUN rpm -Uvh  http://dl.fedoraproject.org/pub/epel/6/x86_64/epel-release-6-8.noarch.rpm
RUN yum -y update
RUN yum -y install java-1.8.0-openjdk java-1.8.0-openjdk-devel 
RUN useradd rundeck
#USER rundeck

ENV USERNAME=rundeck \
    USER=rundeck \
    HOME=/home/rundeck \
    LOGNAME=$USERNAME \
    TERM=xterm-256color


ENV JAVA_HOME=/etc/alternatives/java_sdk
ADD entry.sh /entry.sh
RUN chmod +x /entry.sh
VOLUME $HOME/rundeck
WORKDIR $HOME/rundeck


EXPOSE 4440
ENTRYPOINT ["/entry.sh"]