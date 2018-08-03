FROM rundeck/ubuntu-base@sha256:59eea4c67be38950f2b7fab92accbcfb22ef7e447dc76971930f2171aa1bce8f

COPY --chown=rundeck:rundeck .build .
RUN java -jar rundeck.war --installonly

COPY --chown=rundeck:rundeck confd /etc/confd
COPY --chown=rundeck:rundeck lib docker-lib
COPY --chown=rundeck:rundeck etc etc

VOLUME ["/home/rundeck/server/data"]

EXPOSE 4440
ENTRYPOINT [ "docker-lib/entry.sh" ]
