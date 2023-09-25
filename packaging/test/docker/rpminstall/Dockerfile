# original https://hub.docker.com/r/bwits/rundeck-build/
FROM rdpro-centos7-util:latest

RUN useradd rundeck
#USER rundeck

ENV USERNAME=rundeck \
    USER=rundeck \
    HOME=/home/rundeck \
    LOGNAME=$USERNAME \
    TERM=xterm-256color

ENV JAVA_HOME=/etc/alternatives/java_sdk

COPY --chown=rundeck:root rundeck.rpm /rundeck/rundeck.rpm
COPY entry.sh /entry.sh
RUN chmod +x /entry.sh

VOLUME $HOME/rundeck
WORKDIR $HOME/rundeck


EXPOSE 4440
ENTRYPOINT ["/entry.sh"]
