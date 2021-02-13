FROM rundeck/ubuntu-base@sha256:e9f21a4252c80b145a18c2d0de55453073b551c5ca737d449df4d942f0d1a443

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
