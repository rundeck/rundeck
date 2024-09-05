ARG IMAGE
FROM ${IMAGE}

USER root

RUN apt-get update && \
    apt-get install -y netcat && \
    apt-get -y install sshpass

USER rundeck