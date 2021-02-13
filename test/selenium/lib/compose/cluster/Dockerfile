ARG RUNDECK_IMAGE

FROM ${RUNDECK_IMAGE:-rundeck/rundeck:SNAPSHOT}

RUN mkdir /home/rundeck/var/logs

COPY --chown=rundeck:root remco /etc/remco

VOLUME [ "/home/rundeck/var/logs" ]