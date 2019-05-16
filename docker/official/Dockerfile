FROM rundeck/ubuntu-base@sha256:da17cd6654dc861c92c3a73dbb7fed90932ab3e6364a33b4eb92d72c40da24c2

COPY --chown=rundeck:root .build .
RUN java -jar rundeck.war --installonly \
    && chmod -R 0775 server tools user-assets var

COPY --chown=rundeck:root remco /etc/remco
COPY --chown=rundeck:root lib docker-lib
COPY --chown=rundeck:root etc etc

RUN chmod -R 0775 etc

VOLUME ["/home/rundeck/server/data"]

EXPOSE 4440
ENTRYPOINT [ "/tini", "--", "docker-lib/entry.sh" ]
