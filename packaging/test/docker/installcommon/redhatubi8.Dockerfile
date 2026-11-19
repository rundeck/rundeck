FROM redhat/ubi8:8.10-1793308674@sha256:c322aa68f2d630b36b9faab64094357f62720be2f56f7099376ca2d6c4fe389b

RUN yum -y update
# Grails 7: Java 17 required
RUN yum -y install java-17-openjdk java-17-openjdk-devel initscripts openssh openssl

COPY scripts/rd-util.sh /rd-util.sh
COPY scripts/rpm-tests.sh /init-tests.sh