FROM rdtest:latest

RUN sudo apt-add-repository ppa:ansible/ansible && \
    sudo apt-get update

RUN sudo DEBIAN_FRONTEND=noninteractive apt-get install -y ansible

RUN mkdir -p $HOME/atest
COPY config $HOME/atest
VOLUME $HOME/atest

RUN mkdir -p $HOME/ansible-tests
COPY tests $HOME/ansible-tests
VOLUME $HOME/ansible-tests

EXPOSE 4440