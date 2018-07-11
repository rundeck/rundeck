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

.PHONY: clean rundeck release core-snapshot test app notes

rundeck:  app
	@echo $(VERSION)-$(RELEASE)

#app build via gradle

app: rundeckapp/build/libs/rundeck-$(VERSION).war

rundeckapp/build/libs/rundeck-$(VERSION).war:
	./gradlew -g $$(pwd)/gradle-cache $(PROXY_DEFS) \
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

#clean various components

clean:
	./gradlew clean
	$(MAKE) -C packaging clean
	rm -rf ./gradle-cache


	@echo "Cleaning..."
	#remove rundeckapp lib dir which may contain previously built jars
	-rm rundeckapp/lib/rundeck*.jar

	@echo "Cleaned local build artifacts and targets."
