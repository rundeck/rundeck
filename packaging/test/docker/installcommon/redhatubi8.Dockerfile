FROM redhat/ubi8:8.10-1793308674@sha256:c322aa68f2d630b36b9faab64094357f62720be2f56f7099376ca2d6c4fe389b

RUN yum -y update
# The war is compiled to Java 25 bytecode (class file 69). RHEL 8's own repositories stop at
# java-21-openjdk and Adoptium's rhel/8 channel stops at 23, so take Java 25 from Azul -- which
# is also the route a customer on RHEL 8 has to take.
RUN yum -y install https://cdn.azul.com/zulu/bin/zulu-repo-1.0.0-1.noarch.rpm && \
    yum -y install zulu25-jdk initscripts openssh openssl

COPY scripts/rd-util.sh /rd-util.sh
COPY scripts/rpm-tests.sh /init-tests.sh