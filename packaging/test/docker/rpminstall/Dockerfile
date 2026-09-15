# original https://hub.docker.com/r/bwits/rundeck-build/
FROM rdpro-redhatubi8-util:latest

RUN useradd rundeck
#USER rundeck

ENV USERNAME=rundeck \
    USER=rundeck \
    HOME=/home/rundeck \
    LOGNAME=$USERNAME \
    TERM=xterm-256color

# Zulu registers alternatives for java, javac and friends, but not java_sdk -- that one comes
# from Red Hat's own -devel package. /usr/lib/jvm/zulu25 is the stable path Zulu provides.
ENV JAVA_HOME=/usr/lib/jvm/zulu25

COPY --chown=rundeck:root rundeck.rpm /rundeck/rundeck.rpm
COPY entry.sh /entry.sh
RUN chmod +x /entry.sh

VOLUME $HOME/rundeck
WORKDIR $HOME/rundeck


EXPOSE 4440
ENTRYPOINT ["/entry.sh"]
