SHELL=/bin/bash

VNUMBER=$(shell grep version.number= $${PWD}/version.properties | cut -d= -f 2)
ifndef TAG
TAG=$(shell grep version.tag= $${PWD}/version.properties | cut -d= -f 2)
endif
VERSION=${VNUMBER}-${TAG}
ifeq ($(strip $(TAG)),GA)
VERSION=${VNUMBER}
endif
RELEASE=$(shell grep version.release.number= $${PWD}/version.properties | cut -d= -f 2)

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

app: rundeck-launcher/launcher/build/libs/rundeck-launcher-$(VERSION).jar 

rundeck-launcher/launcher/build/libs/rundeck-launcher-$(VERSION).jar:
	./gradlew -g $$(pwd)/gradle-cache $(PROXY_DEFS) -Penvironment=release -PreleaseTag=$(TAG) -PbuildNum=$(RELEASE) assemble


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
	cd packaging; $(MAKE) VERSION=$(VNUMBER) VNAME=$(VERSION) RELEASE=$(RELEASE) rpmclean rpm

deb: app
	cd packaging; $(MAKE) VERSION=$(VNUMBER) VNAME=$(VERSION) RELEASE=$(RELEASE) debclean deb

#doc build

javadoc:
	./gradlew $(PROXY_DEFS) -Psnapshot -PbuildNum=$(RELEASE) alljavadoc
	mkdir -p docs/en/dist/html
	cp -r build/docs/javadoc docs/en/dist/html/

#clean various components

clean:
	./gradlew clean
	$(MAKE) -C packaging clean
	rm -rf ./gradle-cache


	@echo "Cleaning..."
	#remove rundeckapp lib dir which may contain previously built jars
	-rm rundeckapp/lib/rundeck*.jar

	@echo "Cleaned local build artifacts and targets."
