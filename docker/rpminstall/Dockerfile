# original https://hub.docker.com/r/bwits/rundeck-build/
FROM rdcentos6-util:latest
MAINTAINER Bill W
RUN rpm -Uvh  http://dl.fedoraproject.org/pub/epel/6/x86_64/epel-release-6-8.noarch.rpm
RUN yum -y update
# Grails 8: Java 21 required. This one had never moved past 8, so it was two releases behind rather
# than one.
# NOTE: unbuildable until the base image moves off centos:6, which reached EOL in 2020 -- its
# mirrors are gone and it never carried a JDK past 8.
RUN yum -y install java-21-openjdk java-21-openjdk-devel initscripts openssh
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