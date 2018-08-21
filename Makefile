SHELL=/bin/bash

ENV=development

# Grab the version information from gradle
IGNORE := $(shell TERM=dumb $${PWD}/gradlew -q -Penvironment=$(ENV) bashVersionInfo | sed 's/=/:=/' | sed 's/^/export /' > makeenv)
include makeenv

# Assign to new variables that can be overriden in the make call
VERSION=${VERSION_FULL}
VTAG=${VERSION_TAG}
VNUM=${VERSION_NUMBER}
VDATE=${VERSION_DATE}
VREVISION=${VERSION_REVISION}

PROXY_DEFS=
ifdef http_proxy
	# assume that http_proxy is of format http://<host>:<port> or <host>:<port>
gradle_proxy_host=$(shell echo ${http_proxy}|sed 's/http:\/\///'|awk -F ':' '{ print $1 }')
gradle_proxy_port=$(shell echo ${http_proxy}|awk -F ':' '{ print $NF }')
PROXY_DEFS="-Dhttp.proxyHost=$gradle_proxy_host -Dhttp.proxyPort=$gradle_proxy_port"
endif

.PHONY: clean rundeck release core-snapshot test app notes pkgbuild-docker-image rpm-docker deb-docker rdcentos6-util rdubuntu16.04-util rddebinstall rdrpminstall

rundeck:  app
	@echo $(VERSION)-$(RELEASE)

#app build via gradle

app: rundeckapp/build/libs/rundeck-$(VERSION).war

rundeckapp/build/libs/rundeck-$(VERSION).war:
	./gradlew $(PROXY_DEFS) \
		--build-cache \
		--scan \
		-Penvironment=$(ENV) \
		assemble

#snapshot and release

core-snapshot: 
	cd core; ./gradlew $(PROXY_DEFS) -Psnapshot -PbuildNum=$(RELEASE) uploadArchives

release: 
	GRADLE_OPTS="-Xmx1024m -Xms256m" ./gradlew $(PROXY_DEFS) --no-daemon -Penvironment=release -PbuildNum=$(RELEASE) uploadArchives
	./gradlew $(PROXY_DEFS) --no-daemon nexusStagingRelease

#test via gradle

test:
	@echo Running TestSuite
	./gradlew test

#rpm and deb packaging

rpm: app
	cd packaging; $(MAKE) VERSION=$(VERSION) VNUM=$(VNUM) ENV=$(ENV) VDATE=$(VDATE) rpmclean rpm

deb: app
	cd packaging; $(MAKE) VERSION=$(VERSION) VNUM=$(VNUM) ENV=$(ENV) VDATE=$(VDATE) debclean deb

#doc build

javadoc:
	./gradlew $(PROXY_DEFS) -Psnapshot -PbuildNum=$(RELEASE) alljavadoc

# changelog

CHANGELOG.md: RELEASE.md
	( cat $< ; echo ; echo "---" ; echo; cat $@ ) > $@.tmp
	mv $@.tmp $@

pkgbuild-docker-image: docker/package/Dockerfile
	docker build -t $@ docker/package

rpm-docker: pkgbuild-docker-image app
	docker run -it -v $(PWD):/home/rundeck/rundeck pkgbuild-docker-image make rpm

deb-docker: pkgbuild-docker-image app
	docker run -it -v $(PWD):/home/rundeck/rundeck pkgbuild-docker-image make deb

rdubuntu16.04-util: docker/installcommon/ubuntu.Dockerfile
	docker build -t "$@" -f docker/installcommon/ubuntu.Dockerfile docker/installcommon

rddebinstall: rdubuntu16.04-util docker/debinstall/Dockerfile docker/debinstall/entry.sh
	docker build -t $@ docker/debinstall

deb-test-install: rddebinstall
	docker run -it -v $(PWD):/home/rundeck/rundeck rddebinstall bash

rdcentos6-util: docker/installcommon/centos6.Dockerfile
	docker build -t "$@" -f docker/installcommon/centos6.Dockerfile docker/installcommon

rdrpminstall: rdcentos6-util docker/rpminstall/Dockerfile docker/rpminstall/entry.sh
	docker build -t $@ docker/rpminstall

rpm-test-install: rdrpminstall
	docker run -it -v $(PWD):/home/rundeck/rundeck rdrpminstall bash

#clean various components

clean:
	./gradlew clean
	$(MAKE) -C packaging clean
	rm -rf ./gradle-cache


	@echo "Cleaning..."
	#remove rundeckapp lib dir which may contain previously built jars
	-rm rundeckapp/lib/rundeck*.jar

	@echo "Cleaned local build artifacts and targets."
