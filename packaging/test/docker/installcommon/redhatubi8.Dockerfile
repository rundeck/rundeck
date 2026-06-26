FROM redhat/ubi8

RUN yum -y update
# Grails 7: Java 17 required
RUN yum -y install java-17-openjdk java-17-openjdk-devel initscripts openssh openssl

COPY scripts/rd-util.sh /rd-util.sh
COPY scripts/rpm-tests.sh /init-tests.sh