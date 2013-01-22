SHELL=/bin/bash

VNUMBER=$(shell grep version.number= ${PWD}/version.properties | cut -d= -f 2)
ifndef TAG
TAG=$(shell grep version.tag= ${PWD}/version.properties | cut -d= -f 2)
endif
VERSION=${VNUMBER}-${TAG}
ifeq ($(strip $(TAG)),GA)
VERSION=${VNUMBER}
endif
RELEASE=$(shell grep version.release.number= ${PWD}/version.properties | cut -d= -f 2)

PROXY_DEFS=
ifdef http_proxy
	# assume that http_proxy is of format http://<host>:<port> or <host>:<port>
gradle_proxy_host=$(shell echo ${http_proxy}|sed 's/http:\/\///'|awk -F ':' '{ print $1 }')
gradle_proxy_port=$(shell echo ${http_proxy}|awk -F ':' '{ print $NF }')
PROXY_DEFS="-Dhttp.proxyHost=$gradle_proxy_host -Dhttp.proxyPort=$gradle_proxy_port"
endif


.PHONY: clean rundeck docs makedocs release core-snapshot test app notes

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
	cd core; ./gradlew $(PROXY_DEFS) -Penvironment=release -PbuildNum=$(RELEASE) uploadArchives

#test via gradle

test:
	@echo Running TestSuite
	./gradlew test

#rpm and deb packaging

rpm: docs app
	cd packaging; $(MAKE) VERSION=$(VNUMBER) VNAME=$(VERSION) RELEASE=$(RELEASE) rpmclean rpm

deb: docs app
	cd packaging; $(MAKE) VERSION=$(VNUMBER) VNAME=$(VERSION) RELEASE=$(RELEASE) debclean deb

#doc build

notes: docs/en/history/toc.conf docs/en/RELEASE.md

docs/en/RELEASE.md: RELEASE.md
	cp $< $@

docs/en/history/version-$(VNUMBER).md: RELEASE.md
	( echo "% Version $(VNUMBER)" ; \
		echo "%" $(shell whoami) ; \
		echo "%" $(shell date "+%m/%d/%Y") ; \
		echo ; ) >$@
	cat RELEASE.md >>$@

docs/en/history/toc.conf: docs/en/history/version-$(VNUMBER).md
	echo "1:version-$(VNUMBER).md:Version $(VNUMBER)" > $@.new
	test -f $@ && ( grep -v -q "$(VNUMBER)" $@ && \
		cat $@ >> $@.new && \
		mv $@.new $@ ) || (  mv $@.new $@ )
	

makedocs: 
	$(MAKE) VERSION=$(VERSION) -C docs

docs: makedocs
	mkdir -p ./rundeckapp/web-app/docs
	cp -r docs/en/dist/html/* ./rundeckapp/web-app/docs

#clean various components

clean:
	./gradlew clean
	$(MAKE) -C docs clean
	$(MAKE) -C packaging clean
	rm -rf ./gradle-cache


	@echo "Cleaning..."
	#remove rundeckapp lib dir which may contain previously built jars
	-rm rundeckapp/lib/rundeck*.jar

	@echo "Cleaned local build artifacts and targets."
