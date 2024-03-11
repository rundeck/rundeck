FROM rundeck/ubuntu-base

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

RUN chmod -R 0775 etc && \
    mkdir -p /home/rundeck/var/logs && \
    chmod -R 0775 /home/rundeck/var/logs

VOLUME ["/home/rundeck/server/data"]
VOLUME /home/rundeck/var/logs

EXPOSE 4440
ENTRYPOINT [ "/tini", "--", "docker-lib/entry.sh" ]
