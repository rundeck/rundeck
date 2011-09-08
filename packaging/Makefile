SHELL=/bin/bash

ifndef VERSION
VERSION=$(shell grep version.number= ${PWD}/../version.properties | cut -d= -f 2)
endif
ifndef RELEASE
RELEASE=$(shell grep version.release.number= ${PWD}/../version.properties | cut -d= -f 2)
endif

TAG?=snapshot

RPMBUILD_DEFINES = --define "_topdir ${PWD}/rpmdist" 
RPMBUILD_DEFINES+= --define "_version ${VERSION}"
RPMBUILD_DEFINES+= --define "_release ${RELEASE}"

ifndef BUILD_NUMBER
ALPHATAG?=$(shell date "+%Y%m%d%H%M")$(TAG)
else
ALPHATAG?=$(TAG)
RPMBUILD_DEFINES+= --define "_buildnumber $(BUILD_NUMBER)"
endif

ifndef GA_RELEASE
RPMBUILD_DEFINES+= --define "_alphatag $(ALPHATAG)"
endif

.PHONY: rpm deb

rpm:
	@mkdir -p rpmdist/RPMS/noarch
	@mkdir -p rpmdist/SOURCES
	@mkdir -p rpmdist/SOURCES/usr/bin
	@mkdir -p rpmdist/SOURCES/var/lib/rundeck
	@mkdir -p rpmdist/SOURCES/var/lib/rundeck/exp/webapp
	@mkdir -p rpmdist/SOURCES/var/lib/rundeck/{data,work,logs,cli,libext}
	@mkdir -p rpmdist/SOURCES/var/log/rundeck
	@mkdir -p rpmdist/SOURCES/var/lib/rundeck/var/tmp
	@mkdir -p rpmdist/SOURCES/tmp/rundeck
	@mkdir -p rpmdist/SOURCES/var/rundeck/projects
	@mkdir -p rpmdist/SOURCES/usr/share/man/man{1,5}
	@mkdir -p rpmdist/SOURCES/usr/share/man/en/man{1,5}
	@cp -R root/* rpmdist/SOURCES
	@cp ../docs/en/manpages/man1/*.1.gz rpmdist/SOURCES/usr/share/man/man1
	@cp ../docs/en/manpages/man5/*.5.gz rpmdist/SOURCES/usr/share/man/man5
	@cp ../docs/en/manpages/man1/*.1.gz rpmdist/SOURCES/usr/share/man/en/man1
	@cp ../docs/en/manpages/man5/*.5.gz rpmdist/SOURCES/usr/share/man/en/man5
	@cp -R ../core/build/tools/lib/*.jar rpmdist/SOURCES/var/lib/rundeck/cli
	@cp ../core/build/libs/rundeck-core-*.jar rpmdist/SOURCES/var/lib/rundeck/cli
	@cp ../rundeckapp/target/rundeck-server-*.jar rpmdist/SOURCES/var/lib/rundeck/bootstrap	
	@cp ../plugins/*-plugin/build/libs/*-plugin*.jar rpmdist/SOURCES/var/lib/rundeck/libext
	@find ../core/src/resources/com/dtolabs/rundeck/core/cli/templates ! -regex ".*.bat$$" -and -type f | sed -e /rd-check/d -e /rd-setup/d | xargs -I Z cp Z rpmdist/SOURCES/usr/bin
	@unzip -oq ../rundeckapp/target/rundeck-$(VERSION).war -d rpmdist/SOURCES/var/lib/rundeck/exp/webapp/
	@ln -s /etc/rundeck/log4j.properties; mv log4j.properties rpmdist/SOURCES/var/lib/rundeck/exp/webapp/WEB-INF/classes
	rpmbuild -bb --target=noarch-linux $(RPMBUILD_DEFINES) --buildroot ${PWD}/rpmdist/SOURCES rundeck.spec 


deb:
	@mkdir -p debdist/
	@mkdir -p debdist/usr/bin
	@mkdir -p debdist/var/lib/rundeck/exp/webapp
	@mkdir -p debdist/var/lib/rundeck/{data,work,logs,cli,libext}
	@mkdir -p debdist/var/log/rundeck
	@mkdir -p debdist/var/lib/rundeck/var/tmp
	@mkdir -p debdist/tmp/rundeck
	@mkdir -p debdist/var/rundeck/projects
	@mkdir -p debdist/usr/share/man/man{1,5}
	@mkdir -p debdist/usr/share/man/en/man{1,5}
	@cp -R debroot/* debdist
	@# set version
	@cat debroot/DEBIAN/control | sed -e "s/%VERSION%/$(VERSION)/" > debdist/DEBIAN/control
	@cp ../docs/en/manpages/man1/*.1.gz debdist/usr/share/man/man1
	@cp ../docs/en/manpages/man5/*.5.gz debdist/usr/share/man/man5
	@cp ../docs/en/manpages/man1/*.1.gz debdist/usr/share/man/en/man1
	@cp ../docs/en/manpages/man5/*.5.gz debdist/usr/share/man/en/man5
	@cp -R ../core/build/tools/lib/*.jar debdist/var/lib/rundeck/cli
	@cp ../core/build/libs/rundeck-core-*.jar debdist/var/lib/rundeck/cli
	@cp ../rundeckapp/target/rundeck-server-*.jar debdist/var/lib/rundeck/bootstrap
	@cp ../plugins/*-plugin/build/libs/*-plugin*.jar debdist/var/lib/rundeck/libext
	@find ../core/src/resources/com/dtolabs/rundeck/core/cli/templates ! -regex ".*.bat$$" -and -type f | sed -e /rd-check/d -e /rd-setup/d | xargs -I Z cp Z debdist/usr/bin
	@unzip -oq ../rundeckapp/target/rundeck-$(VERSION).war -d debdist/var/lib/rundeck/exp/webapp/
	@ln -s /etc/rundeck/log4j.properties; mv log4j.properties debdist/var/lib/rundeck/exp/webapp/WEB-INF/classes
	fakeroot dpkg-deb --build debdist
	@mv debdist.deb rundeck-$(VERSION)-$(RELEASE).deb

clean:
	rm -rf target rpmdist debdist
