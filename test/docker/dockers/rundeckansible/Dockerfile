FROM rdtest:latest

RUN sudo apt-get update && \
    sudo apt-get -y install python3-pip && \
    sudo pip3 install --break-system-packages ansible-core && \
    sudo rm -rf /var/lib/apt/lists/*

RUN mkdir -p $HOME/atest
COPY config $HOME/atest
VOLUME $HOME/atest

RUN mkdir -p $HOME/ansible-tests
COPY tests $HOME/ansible-tests
VOLUME $HOME/ansible-tests

EXPOSE 4440