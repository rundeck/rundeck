FROM rundeck/ubuntu-base@sha256:b985e4561ce61dc0865750394885f9afd9a1dffb56d4f72a8b6b575f2a342509

COPY --chown=rundeck:root scripts/rd-util.sh /rd-util.sh
ADD --chown=rundeck:root scripts/deb-tests.sh /init-tests.sh
