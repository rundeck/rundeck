SHELL=/bin/bash

VERSION=1.0.0
RELEASE=0

GRAILSVERS=1.2.0
JETTYVERS=6.1.21

GRAILS_HOME=${BUILD_ROOT}/local/grails-$GRAILSVERS
#PATH=$PATH:$GRAILS_HOME/bin

GARGS += -Dgrails.project.work.dir=${PWD}/rundeckapp/work

GRAILS=grails $(GARGS)

RUNDECK_FILES=$(shell find rundeckapp/{src,test,grails-app,scripts} -name "*.java" -o -name "*.groovy" -o -name "*.gsp")
CORE_FILES=$(shell find core/src -name "*.java" -o -path "*/src/sh/*")

core = core/target/rundeck-core-$(VERSION).jar
war = rundeckapp/target/rundeck-$(VERSION).war
launcher = rundeckapp/target/rundeck-launcher-$(VERSION).jar

.PHONY: clean, rundeck

rundeck: $(war) $(launcher)
	@echo $(VERSION)-$(RELEASE)

rpm: $(war)
	cd packaging; $(MAKE) clean rpm

$(core): $(CORE_FILES)
	./build.sh rundeck_core

$(war): $(core) $(RUNDECK_FILES)
	./build.sh rundeckapp

$(launcher): $(core) $(RUNDECK_FILES)
	./build.sh rundeckapp

.PHONY: test
test: $(war)
	@echo Running TestSuite
	pushd rundeckapp; $(GRAILS) test-app; popd
	
clean:
	rm $(core) $(war) $(launcher)

	pushd rundeckapp; $(GRAILS) clean; popd

	@echo "Cleaning..."
	#clean localrepo of build artifacts
	#rm -r ${BUILD_ROOT}/localrepo/rundeck*

	#remove rundeckapp lib dir which may contain previously built jars
	rm rundeckapp/lib/rundeck*.jar

	#clean build target dirs
	rm -rf core/target
	rm -r rundeckapp/target

	#clean intermediate maven repo dirs of build artifacts
	rm -r maven/repository/rundeck*

	@echo "Cleaned local build artifacts and targets."
