FROM redhat/ubi8:8.10@sha256:8827ae684e58fbdb93c8893e48731bae36d5cdd302a6efebee8e01707145c85e

RUN yum -y update
# The war is compiled to Java 25 bytecode (class file 69). RHEL 8's own repositories stop at
# java-21-openjdk and Adoptium's rhel/8 channel stops at 23, so take Java 25 from Azul -- which
# is also the route a customer on RHEL 8 has to take.
RUN yum -y install https://cdn.azul.com/zulu/bin/zulu-repo-1.0.0-1.noarch.rpm && \
    yum -y install zulu25-jdk initscripts openssh openssl

COPY scripts/rd-util.sh /rd-util.sh
COPY scripts/rpm-tests.sh /init-tests.sh