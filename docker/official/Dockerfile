FROM rundeck/ubuntu-base@sha256:18c1cc7f9a50e4cbd651268eca4de659d0c522bdf12a1b2b0f67aabf9da8400b

COPY --chown=rundeck:rundeck .build .
RUN java -jar rundeck.war --installonly

COPY --chown=rundeck:rundeck remco /etc/remco
COPY --chown=rundeck:rundeck lib docker-lib
COPY --chown=rundeck:rundeck etc etc

VOLUME ["/home/rundeck/server/data"]

EXPOSE 4440
ENTRYPOINT [ "docker-lib/entry.sh" ]
