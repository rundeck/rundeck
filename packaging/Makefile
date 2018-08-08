SHELL=/bin/bash

ENV=development

# Grab the version information from gradle
IGNORE := $(shell cd ../ && TERM=dumb $${PWD}/gradlew -q -Penvironment=$(ENV) bashVersionInfo | sed 's/=/:=/' | sed 's/^/export /' > ../makeenv)
include ../makeenv

# Assign to new variables that can be overriden in the make call
VERSION=${VERSION_FULL}
VTAG=${VERSION_TAG}
VNUM=${VERSION_NUMBER}
VDATE=${VERSION_DATE}
VREVISION=${VERSION_REVISION}

BUILD_TIMESTAMP=$(shell date "+%Y%m%d%H%M")

# Tilde separates version number from tag for sortability
# Yum and apt consider any version with ~ to be older than those without
ifeq ($(VTAG),GA)
  RPM_VERSION=$(VNUM).$(VDATE)
  DEB_VERSION=$(VNUM).$(VDATE)-$(VREVISION).$(BUILD_TIMESTAMP)
else
  RPM_VERSION=$(VNUM).$(VDATE)~$(VTAG)
  DEB_VERSION=$(VNUM).$(VDATE)~$(VTAG)-$(VREVISION).$(BUILD_TIMESTAMP)
endif

RPM_RELEASE=$(VREVISION).$(BUILD_TIMESTAMP)

RPMBUILD_DEFINES = --define "_topdir ${PWD}/rpmdist" 
RPMBUILD_DEFINES+= --define "_version ${RPM_VERSION}"
RPMBUILD_DEFINES+= --define "_release ${RPM_RELEASE}"

RPMBUILD_DEFINES+= --define "_vname ${VERSION}"

.PHONY: rpm deb

rpm:
	@mkdir -p rpmdist/RPMS/noarch
	@mkdir -p rpmdist/SOURCES
	@mkdir -p rpmdist/SOURCES/usr/bin
	@mkdir -p rpmdist/SOURCES/var/lib/rundeck/.ssh
	@mkdir -p rpmdist/SOURCES/var/lib/rundeck/exp/webapp
	@mkdir -p rpmdist/SOURCES/var/lib/rundeck/{data,work,logs,cli,libext,bootstrap}
	@mkdir -p rpmdist/SOURCES/var/lib/rundeck/libext/cache
	@mkdir -p rpmdist/SOURCES/var/log/rundeck
	@mkdir -p rpmdist/SOURCES/var/lib/rundeck/var/tmp/pluginJars
	@mkdir -p rpmdist/SOURCES/tmp/rundeck
	@mkdir -p rpmdist/SOURCES/var/rundeck/projects
	@mkdir -p rpmdist/SOURCES/var/rundeck/user-assets
	@cp -R root/* rpmdist/SOURCES
	@cp -R common/* rpmdist/SOURCES
	@cp -R ../core/build/tools/lib/*.jar rpmdist/SOURCES/var/lib/rundeck/cli
	@cp ../core/build/libs/rundeck-core-$(VERSION).jar rpmdist/SOURCES/var/lib/rundeck/cli
	@cp ../rundeckapp/build/libs/rundeck-$(VERSION).war rpmdist/SOURCES/var/lib/rundeck/bootstrap
	@cp ../plugins/*-plugin/build/libs/*-plugin-$(VERSION).jar rpmdist/SOURCES/var/lib/rundeck/libext
	@cp ../core/src/main/resources/com/dtolabs/launcher/setup/templates/admin.aclpolicy.template rpmdist/SOURCES/etc/rundeck/admin.aclpolicy
	@cp ../core/src/main/resources/com/dtolabs/launcher/setup/templates/apitoken.aclpolicy.template rpmdist/SOURCES/etc/rundeck/apitoken.aclpolicy
	@find ../core/src/main/resources/com/dtolabs/rundeck/core/cli/templates ! -regex ".*.bat$$" -and -type f | sed -e /rd-check/d -e /rd-setup/d | xargs -I Z cp Z rpmdist/SOURCES/usr/bin
	rpmbuild -bb --target=noarch-linux $(RPMBUILD_DEFINES) --buildroot ${PWD}/rpmdist/SOURCES rundeck.spec


deb:
	@mkdir -p debdist/
	@mkdir -p debdist/usr/bin
	@mkdir -p debdist/var/lib/rundeck/exp/webapp
	@mkdir -p debdist/var/lib/rundeck/{data,work,logs,cli,libext,bootstrap}
	@mkdir -p debdist/var/log/rundeck
	@mkdir -p debdist/var/lib/rundeck/var/tmp/pluginJars
	@mkdir -p debdist/tmp/rundeck
	@mkdir -p debdist/var/rundeck/projects
	@mkdir -p debdist/var/rundeck/user-assets
	@cp -R debroot/* debdist
	@cp -R common/* debdist
	@# set version
	@cat debroot/DEBIAN/control | sed -e "s/%VERSION%/$(DEB_VERSION)/" > debdist/DEBIAN/control
	@cp -R ../core/build/tools/lib/*.jar debdist/var/lib/rundeck/cli
	@cp ../core/build/libs/rundeck-core-$(VERSION).jar debdist/var/lib/rundeck/cli
	@cp ../rundeckapp/build/libs/rundeck-$(VERSION).war debdist/var/lib/rundeck/bootstrap
	@cp ../plugins/*-plugin/build/libs/*-plugin-$(VERSION).jar debdist/var/lib/rundeck/libext
	@cp ../core/src/main/resources/com/dtolabs/launcher/setup/templates/admin.aclpolicy.template debdist/etc/rundeck/admin.aclpolicy
	@cp ../core/src/main/resources/com/dtolabs/launcher/setup/templates/apitoken.aclpolicy.template debdist/etc/rundeck/apitoken.aclpolicy
	@find ../core/src/main/resources/com/dtolabs/rundeck/core/cli/templates ! -regex ".*.bat$$" -and -type f | sed -e /rd-check/d -e /rd-setup/d | xargs -I Z cp Z debdist/usr/bin
	fakeroot dpkg-deb --build -Zgzip debdist
	@mv debdist.deb debdist/rundeck_$(DEB_VERSION)_all.deb

clean: rpmclean debclean

rpmclean:
	rm -rf rpmdist

debclean:
	rm -rf debdist rundeck*.deb
