FROM rundeck/ubuntu-base@sha256:f26f1db704553f138c8ec70ea6a53d23329308589d2d2a3f781a410a9a154e5c

# Update image apt packages
USER root
RUN apt-get update && \
    apt-get upgrade -y && \
    apt-get clean

USER rundeck

# Update image apt packages
USER root
RUN apt-get update && \
    apt-get upgrade -y && \
    apt-get clean

USER rundeck

COPY --chown=rundeck:root .build .
RUN java -jar rundeck.war --installonly \
    # Create plugin folder
    && mkdir libext \
    # Adjust permissions for OpenShift
    && chmod -R 0775 libext server user-assets var

COPY --chown=rundeck:root remco /etc/remco
COPY --chown=rundeck:root lib docker-lib
COPY --chown=rundeck:root etc etc

RUN chmod -R 0775 etc

VOLUME ["/home/rundeck/server/data"]


EXPOSE 4440
ENTRYPOINT [ "/tini", "--", "docker-lib/entry.sh" ]
