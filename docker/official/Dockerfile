FROM rundeck/ubuntu-base

USER rundeck

COPY --chown=rundeck:root .build .
RUN java -jar rundeck.war --installonly

# Directories and permissions
RUN mkdir -p libext etc var/logs \
        # Adjust permissions for OpenShift
        && chmod -R 0775 libext server user-assets etc var

COPY --chown=rundeck:root remco /etc/remco
COPY --chown=rundeck:root lib docker-lib
COPY --chown=rundeck:root etc etc

VOLUME ["/home/rundeck/server/data"]
VOLUME ["/home/rundeck/var/logs"]

EXPOSE 4440
ENTRYPOINT [ "/tini", "--", "docker-lib/entry.sh" ]
