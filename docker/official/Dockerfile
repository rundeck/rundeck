FROM rundeck/ubuntu-base@sha256:7057294fad2e341ea2582acf0a4d26fc66d7e1cf8d875f4207d3377f4b9fbd9f

COPY --chown=rundeck:rundeck .build .
RUN java -jar rundeck.war --installonly

COPY --chown=rundeck:rundeck remco /etc/remco
COPY --chown=rundeck:rundeck lib docker-lib
COPY --chown=rundeck:rundeck etc etc

VOLUME ["/home/rundeck/server/data"]

EXPOSE 4440
ENTRYPOINT [ "docker-lib/entry.sh" ]
