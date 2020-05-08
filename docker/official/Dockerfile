FROM rundeck/ubuntu-base@sha256:f22116b6acdad58f2e24f9f6516c6ada0903f18839d285f12e01c5e905f6148d

COPY --chown=rundeck:root .build .
RUN java -jar rundeck.war --installonly \
    # Create plugin folder
    && mkdir libext \
    # Adjust permissions for OpenShift
    && chmod -R 0775 libext server tools user-assets var

COPY --chown=rundeck:root remco /etc/remco
COPY --chown=rundeck:root lib docker-lib
COPY --chown=rundeck:root etc etc

RUN chmod -R 0775 etc

VOLUME ["/home/rundeck/server/data"]

EXPOSE 4440
ENTRYPOINT [ "/tini", "--", "docker-lib/entry.sh" ]
