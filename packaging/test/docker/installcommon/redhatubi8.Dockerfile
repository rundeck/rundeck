FROM redhat/ubi8:8.10-1793308674@sha256:c322aa68f2d630b36b9faab64094357f62720be2f56f7099376ca2d6c4fe389b

RUN yum -y update
# The war is compiled to Java 21 bytecode (class file 65). Take Java 21 from Azul, the same
# route used for the RPM install test image.
RUN yum -y install https://cdn.azul.com/zulu/bin/zulu-repo-1.0.0-1.noarch.rpm && \
    yum -y install zulu21-jdk initscripts openssh openssl

COPY scripts/rd-util.sh /rd-util.sh
COPY scripts/rpm-tests.sh /init-tests.sh