FROM rdtest:latest

RUN sudo apt-get update
RUN sudo apt-get --no-install-recommends -y install software-properties-common
RUN sudo apt-add-repository ppa:ansible/ansible
RUN sudo apt-get update
RUN sudo apt-get --no-install-recommends -y install ansible

RUN mkdir -p $HOME/atest
COPY config $HOME/atest
VOLUME $HOME/atest

RUN mkdir -p $HOME/ansible-tests
COPY tests $HOME/ansible-tests
VOLUME $HOME/ansible-tests

EXPOSE 4440