FROM ubuntu:16.04

ENV DEBIAN_FRONTEND noninteractive
ENV LANG C.UTF-8
ENV LC_ALL C.UTF-8

## BASH
RUN echo "dash dash/sh boolean false" | debconf-set-selections \
    && dpkg-reconfigure dash

RUN set -euxo pipefail \
    && sed -i -e 's#http://\(archive\|security\)#mirror://mirrors#' -e 's#/ubuntu/#/mirrors.txt#' /etc/apt/sources.list \
    && apt-get -y update \
    && apt-get -y --no-install-recommends install \
        ca-certificates \
        git \
        wget \
        zip \
    && wget -q -O - https://dl-ssl.google.com/linux/linux_signing_key.pub | apt-key add - \
    && echo "deb http://dl.google.com/linux/chrome/deb/ stable main" >> /etc/apt/sources.list.d/google-chrome.list \
    && apt-get update \
    && apt-get -y --no-install-recommends install google-chrome-stable \
    && rm -rf /var/lib/apt/lists/*

RUN wget -qO- https://raw.githubusercontent.com/creationix/nvm/v0.33.11/install.sh | bash \
    && echo 'export NVM_DIR="$HOME/.nvm"' >> ~/.bash_profile \
    && echo '[ -s "$NVM_DIR/nvm.sh" ] && \. "$NVM_DIR/nvm.sh"' >> ~/.bash_profile \
    && bash -l -c 'nvm install 12'

COPY entry.sh .

RUN mkdir /app

VOLUME /app/node_modules

WORKDIR /app

ENTRYPOINT ["/bin/bash", "-l", "/entry.sh"]