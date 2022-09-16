FROM rdpro-ubuntu1604-util:latest

ENV USERNAME=rundeck \
    USER=rundeck \
    HOME=/home/rundeck \
    LOGNAME=$USERNAME \
    TERM=xterm-256color

COPY --chown=rundeck:root entry.sh /entry.sh
RUN chmod +x /entry.sh
VOLUME $HOME/rundeck
WORKDIR $HOME/rundeck

USER root

EXPOSE 4440
ENTRYPOINT ["/entry.sh"]
