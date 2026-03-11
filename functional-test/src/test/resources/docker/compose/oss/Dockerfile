ARG IMAGE
FROM ${IMAGE}
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

COPY --chown=rundeck:root keys /home/rundeck/privatekeys
RUN chown -R rundeck /home/rundeck/privatekeys
RUN chmod 770 /home/rundeck/privatekeys

USER rundeck