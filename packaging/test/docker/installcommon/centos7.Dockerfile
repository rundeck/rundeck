FROM centos:7

RUN yum -y update
RUN yum -y install java-11-openjdk java-11-openjdk-devel initscripts openssh openssl

COPY scripts/rd-util.sh /rd-util.sh
COPY scripts/rpm-tests.sh /init-tests.sh