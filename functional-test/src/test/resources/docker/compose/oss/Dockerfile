ARG IMAGE
FROM ${IMAGE}
VOLUME /home/rundeck/privatekeys
USER root

RUN apt-get update && \
    apt-get install -y netcat && \
    apt-get -y install sshpass

RUN apt-get install -y netcat

# install ansible
RUN apt-get update && \
    apt-get -y install python3-pip && \
    apt-get -y install iputils-ping && \
    pip3 install --upgrade pip

RUN pip3 install ansible==9.6.1

RUN mkdir -p /home/rundeck/privatekeys
RUN chown -R rundeck /home/rundeck/privatekeys
RUN chmod 770 /home/rundeck/privatekeys

COPY --chown=rundeck:root entrypoint.sh /entrypoint.sh
RUN chmod +x /entrypoint.sh

USER rundeck

ENTRYPOINT [ "/tini", "--", "/entrypoint.sh" ]