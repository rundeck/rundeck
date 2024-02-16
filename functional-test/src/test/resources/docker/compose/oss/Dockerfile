ARG IMAGE
FROM ${IMAGE}

COPY --chown=rundeck:root git-remote-repo-mock.zip /tmp/git-remote-repo-mock.zip
RUN unzip -q /tmp/git-remote-repo-mock.zip -d /tmp/repoTemplate