FROM circleci/openjdk:8

RUN wget -qO- https://raw.githubusercontent.com/creationix/nvm/v0.33.11/install.sh | bash \
    && echo 'export NVM_DIR="$HOME/.nvm"' >> ~/.bash_profile \
    && echo '[ -s "$NVM_DIR/nvm.sh" ] && \. "$NVM_DIR/nvm.sh"' >> ~/.bash_profile \
    && bash -l -c 'nvm install 12'

RUN mkdir ~/.gradle && echo "org.gradle.daemon=false" > ~/.gradle/gradle.properties

RUN curl -s "https://get.sdkman.io" | bash \
    && echo "source /home/circleci/.sdkman/bin/sdkman-init.sh" >> ~/.bash_profile \
    && bash -l -c 'yes | sdk install groovy' 

RUN sudo apt-get update \
    && sudo apt-get -y install libpython-dev python-pip \
    && pip install awscli --user \
    && sudo apt-get clean \
    && sudo rm -rf /var/lib/apt/lists/*

ENTRYPOINT ["/bin/bash", "-l", "-c"]