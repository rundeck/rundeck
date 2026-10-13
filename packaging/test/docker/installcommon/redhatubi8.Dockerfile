FROM redhat/ubi8:8.10@sha256:8827ae684e58fbdb93c8893e48731bae36d5cdd302a6efebee8e01707145c85e

RUN yum -y update
# Grails 7: Java 17 required
RUN yum -y install java-17-openjdk java-17-openjdk-devel initscripts openssh openssl

COPY scripts/rd-util.sh /rd-util.sh
COPY scripts/rpm-tests.sh /init-tests.sh