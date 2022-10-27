FROM rundeck/ubuntu-base@sha256:b985e4561ce61dc0865750394885f9afd9a1dffb56d4f72a8b6b575f2a342509

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
