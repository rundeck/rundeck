FROM rundeck/ubuntu-base

COPY --chown=rundeck:root scripts/rd-util.sh /rd-util.sh
COPY --chown=rundeck:root scripts/deb-tests.sh /init-tests.sh
