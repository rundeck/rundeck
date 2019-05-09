FROM rundeck/ubuntu-base@sha256:591a682e8028226e2eabff328c516189b39af85e1a5cb457f23a672cbc47982e

COPY --chown=rundeck:rundeck .build .
RUN java -jar rundeck.war --installonly

COPY --chown=rundeck:rundeck remco /etc/remco
COPY --chown=rundeck:rundeck lib docker-lib
COPY --chown=rundeck:rundeck etc etc

VOLUME ["/home/rundeck/server/data"]

EXPOSE 4440
ENTRYPOINT [ "/tini", "--", "docker-lib/entry.sh" ]
