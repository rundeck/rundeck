VERSION=1.0.0
RELEASE=0

GRAILSVERS=1.2.0
JETTYVERS=6.1.21

GRAILS_HOME=${BUILD_ROOT}/local/grails-$GRAILSVERS
#PATH=$PATH:$GRAILS_HOME/bin

GARGS += -Dgrails.project.work.dir=rundeckapp/work
grails=grails $(GARGS)

BUILD_ON_COMMIT=.git/index

core = core/target/rundeck-core-$(VERSION).jar
war = rundeckapp/target/rundeck-$(VERSION).war
launcher = rundeckapp/target/rundeck-launcher-$(VERSION).jar

.PHONY: clean, rundeck

rundeck: $(war) $(launcher)
	@echo $(VERSION)-$(RELEASE)

rpm: $(war)
	@echo rpmbuild

$(core): $(BUILD_ON_COMMIT)
	./build.sh rundeck_core

$(war): $(core) $(BUILD_ON_COMMIT)
	./build.sh rundeckapp

$(launcher): $(core) $(BUILD_ON_COMMIT)
	./build.sh rundeckapp

.PHONY: test
test: $(war)
	@echo Running TestSuite
	pushd rundeckapp; $(grails) test-app; popd
	
clean:
	rm $(core) $(war) $(launcher)

	pushd rundeckapp; $(grails) clean; popd

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
