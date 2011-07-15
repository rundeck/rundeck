SHELL=/bin/bash

VERSION=$(shell grep version.number= ${PWD}/version.properties | cut -d= -f 2)
RELEASE=$(shell grep version.release.number= ${PWD}/version.properties | cut -d= -f 2)

GRAILSVERS=1.2.0
JETTYVERS=6.1.21

GRAILS_HOME=${PWD}/build/local/grails-${GRAILSVERS}
#PATH=$PATH:$GRAILS_HOME/bin

GARGS += -Dgrails.project.work.dir=${PWD}/rundeckapp/work

GRAILS=$(GRAILS_HOME)/bin/grails $(GARGS)

RUNDECK_FILES=$(shell find rundeckapp/{src,test,grails-app,scripts} -name "*.java" -o -name "*.groovy" -o -name "*.gsp")
CORE_FILES=$(shell find core/src -name "*.java" -o -name "*.templates" -o -path "*/src/sh/*")
PLUGIN_FILES=$(shell find plugins/*-plugin -name "*.java" )
plugs= $(shell for i in plugins/*-plugin ; do v=` basename $$i`; echo $$i/build/libs/rundeck-$${v}-$(VERSION).jar ; done )

core = core/build/libs/rundeck-core-$(VERSION).jar
war = rundeckapp/target/rundeck-$(VERSION).war
launcher = rundeckapp/target/rundeck-launcher-$(VERSION).jar

.PHONY: clean rundeck docs makedocs plugins

rundeck:  $(launcher)
	@echo $(VERSION)-$(RELEASE)

rpm: docs $(war) $(plugs)
	cd packaging; $(MAKE) VERSION=$(VERSION) RELEASE=$(RELEASE) clean rpm

deb: docs $(war) $(plugs)
	cd packaging; $(MAKE) VERSION=$(VERSION) RELEASE=$(RELEASE) clean deb

makedocs:
	$(MAKE) -C docs

$(core): $(CORE_FILES)
	./build.sh rundeck_core

$(war): $(launcher)

$(plugs): $(core) $(PLUGIN_FILES)
	cd plugins && ./gradlew	

plugins: $(plugs)
	-rm -rf ./rundeckapp/target/launcher-contents/libext 
	mkdir -p ./rundeckapp/target/launcher-contents/libext
	for i in $(plugs) ; do cp $$i ./rundeckapp/target/launcher-contents/libext/ ; done

docs: makedocs
	-rm -rf ./rundeckapp/target/launcher-contents/docs ./rundeckapp/web-app/docs
	mkdir -p ./rundeckapp/target/launcher-contents/docs/man/man1
	mkdir -p ./rundeckapp/target/launcher-contents/docs/man/man5
	mkdir -p ./rundeckapp/web-app/docs
	cp -r docs/en/dist/html/* ./rundeckapp/target/launcher-contents/docs
	cp -r docs/en/dist/html/* ./rundeckapp/web-app/docs
	cp docs/en/dist/man/man1/*.1 ./rundeckapp/target/launcher-contents/docs/man/man1
	cp docs/en/dist/man/man5/*.5 ./rundeckapp/target/launcher-contents/docs/man/man5

$(launcher): $(core) plugins $(RUNDECK_FILES)
	./build.sh rundeckapp

.PHONY: test
test: $(war)
	@echo Running TestSuite
	pushd rundeckapp; $(GRAILS) test-app; popd
	
clean:
	-rm $(core) $(war) $(launcher) $(plugs)
	$(MAKE) -C docs clean

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
