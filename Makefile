SHELL=/bin/bash

VNUMBER=$(shell grep version.number= ${PWD}/version.properties | cut -d= -f 2)
VTAG=$(shell grep version.tag= ${PWD}/version.properties | cut -d= -f 2)
VERSION=${VNUMBER}-${VTAG}
ifeq ($(strip $(VTAG)),GA)
VERSION=${VNUMBER}
endif
RELEASE=$(shell grep version.release.number= ${PWD}/version.properties | cut -d= -f 2)

GRAILSVERS=1.3.7
JETTYVERS=6.1.21

GRAILS_HOME=${PWD}/build/local/grails-${GRAILSVERS}
#PATH=$PATH:$GRAILS_HOME/bin

PROXY_DEFS=
ifdef http_proxy
	# assume that http_proxy is of format http://<host>:<port> or <host>:<port>
gradle_proxy_host=$(shell echo ${http_proxy}|sed 's/http:\/\///'|awk -F ':' '{ print $1 }')
gradle_proxy_port=$(shell echo ${http_proxy}|awk -F ':' '{ print $NF }')
PROXY_DEFS="-Dhttp.proxyHost=$gradle_proxy_host -Dhttp.proxyPort=$gradle_proxy_port"
endif

GARGS += -Dgrails.project.work.dir=${PWD}/rundeckapp/work $(PROXY_DEFS)

GRAILS=$(GRAILS_HOME)/bin/grails $(GARGS)
HASWGET=$(shell which wget)
HASCURL=$(shell which curl)
GET=
ifneq ($(strip $(HASWGET)),)
GET=$(HASWGET) -N -nd
endif
ifneq ($(strip $(HASCURL)),)
GET=$(HASCURL) -O
endif

RUNDECK_FILES=$(shell find rundeckapp/{src,test,grails-app,scripts} -name "*.java" -o -name "*.groovy" -o -name "*.gsp")
CORE_FILES=$(shell find core/src -name "*.java" -o -name "*.templates" -o -path "*/src/sh/*")
PLUGIN_FILES=$(shell find plugins/*-plugin -name "*.java" )
plugs= $(shell for i in plugins/*-plugin ; do v=` basename $$i`; echo $$i/build/libs/rundeck-$${v}-$(VERSION).jar ; done )

core = core/build/libs/rundeck-core-$(VERSION).jar
war = rundeckapp/target/rundeck-$(VERSION).war
launcher = rundeck-launcher/launcher/build/libs/rundeck-launcher-$(VERSION).jar



.PHONY: clean rundeck docs makedocs plugins war launcher

rundeck:  $(launcher)
	@echo $(VERSION)-$(RELEASE)

rpm: docs $(launcher) $(plugs)
	cd packaging; $(MAKE) VERSION=$(VERSION) RELEASE=$(RELEASE) rpmclean rpm

deb: docs $(launcher) $(plugs)
	cd packaging; $(MAKE) VERSION=$(VERSION) RELEASE=$(RELEASE) debclean deb

makedocs:
	$(MAKE) -C docs

$(core): $(CORE_FILES)
	cd core; ./gradlew $(PROXY_DEFS) -PbuildNum=$(RELEASE) clean check assemble javadoc

war: $(war)

grails: $(GRAILS_HOME)

$(GRAILS_HOME):
	mkdir -p ${PWD}/build/local
ifndef GET
	echo "Couldn't find wget or curl, need one or the other!" 1>&2
	exit 1
endif

	@echo "Get/expand grails distribution..."
	@if [ ! -f ${PWD}/build/local/grails-$(GRAILSVERS)/bin/grails ] ; then \
		if [ ! -z "${PKGREPO}" -a -f ${PKGREPO}/grails/zips/grails-$(GRAILSVERS).zip ] ; then \
			cd ${PWD}/build/local ; \
			unzip ${PKGREPO}/grails/zips/grails-$(GRAILSVERS).zip ; \
		else \
			cd ${PWD}/build/local ; \
			$(GET) http://dist.springframework.org.s3.amazonaws.com/release/GRAILS/grails-$(GRAILSVERS).zip ; \
			unzip ${PWD}/build/local/grails-$(GRAILSVERS).zip ; \
		fi \
	fi

$(war): $(core) $(RUNDECK_FILES) $(GRAILS_HOME)
	echo make war
	cp $(core) rundeckapp/lib/
	#echo 'y' to the command to quell y/n prompt on second time running it:
	cd rundeckapp; yes | $(GRAILS)  install-plugin ${PWD}/dependencies/grails-jetty/zips/grails-jetty-1.2-SNAPSHOT.zip
	cd rundeckapp; $(GRAILS)  clean
	cd rundeckapp; $(GRAILS)   test-app
	cd rundeckapp; yes | $(GRAILS) prod war

$(plugs): $(core) $(PLUGIN_FILES)
	cd plugins && ./gradlew	

plugins: $(plugs)

docs: makedocs
	mkdir -p ./rundeckapp/web-app/docs
	cp -r docs/en/dist/html/* ./rundeckapp/web-app/docs

launcher: $(launcher)

$(launcher): plugins $(war)
	cd rundeck-launcher; ./gradlew -g $$(pwd)/gradle-cache $(PROXY_DEFS) -PbuildNum=$(RELEASE) clean assemble

.PHONY: test
test: $(war)
	@echo Running TestSuite
	pushd rundeckapp; $(GRAILS) test-app; popd
	
clean:
	-rm $(core) $(war) $(launcher) $(plugs)
	$(MAKE) -C docs clean
	cd rundeck-launcher; ./gradlew clean
	rm -rf rundeck-launcher/gradle-cache

	pushd rundeckapp; $(GRAILS) clean; popd

	@echo "Cleaning..."
	#clean localrepo of build artifacts
	-rm -r build/localrepo/rundeck*

	#remove rundeckapp lib dir which may contain previously built jars
	-rm rundeckapp/lib/rundeck*.jar

	#clean build target dirs
	-rm -rf core/target
	-rm -rf core/build
	-rm -r rundeckapp/target

	@echo "Cleaned local build artifacts and targets."
