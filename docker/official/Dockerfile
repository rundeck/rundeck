FROM rundeck/ubuntu-base@sha256:7ebfe11f0b03f0919d64935dd66c3a630a1bc4a91104778be7066240ac25d2b9

COPY --chown=rundeck:rundeck .build .
RUN java -jar rundeck.war --installonly

COPY --chown=rundeck:rundeck remco /etc/remco
COPY --chown=rundeck:rundeck lib docker-lib
COPY --chown=rundeck:rundeck etc etc

VOLUME ["/home/rundeck/server/data"]

EXPOSE 4440
ENTRYPOINT [ "docker-lib/entry.sh" ]
