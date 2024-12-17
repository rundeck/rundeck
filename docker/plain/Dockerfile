FROM rundeck/ubuntu-base-plain

# Update image apt packages
USER root
RUN apt-get update && \
    apt-get upgrade -y && \
    apt-get clean

USER rundeck

COPY --chown=rundeck:root .build .
RUN java -jar rundeck.war --installonly

# Directories and permissions
RUN mkdir -p libext etc var/logs \
        # Adjust permissions for OpenShift
        && chmod -R 0775 libext server user-assets etc var

COPY --chown=rundeck:root lib docker-lib
COPY --chown=rundeck:root etc etc
COPY --chown=rundeck:root config server/config

VOLUME ["/home/rundeck/server/data"]
VOLUME ["/home/rundeck/var/logs"]

EXPOSE 4440
ENTRYPOINT [ "/tini", "--", "docker-lib/entry.sh" ]
