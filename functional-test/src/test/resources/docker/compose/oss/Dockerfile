ARG IMAGE
FROM ${IMAGE}

USER root

RUN apt-get update && \
    apt-get install -y netcat-traditional && \
    apt-get -y install sshpass

# install ansible
RUN apt-get update && \
    apt-get -y install python3-pip && \
    apt-get -y install iputils-ping && \
    pip3 install --upgrade pip

RUN pip3 install ansible==9.6.1

USER rundeck