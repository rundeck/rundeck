VERSION=1.0.0
RELEASE=0

GRAILSVERS=1.2.0
JETTYVERS=6.1.21

core = core/target/rundeck-core-$(VERSION).jar
war = rundeckapp/target/rundeck-$(VERSION).war
launcher = rundeckapp/target/rundeck-launcher-$(VERSION).jar

.PHONY: clean, rundeck

rundeck: $(core) $(war) $(launcher)
	@echo $(VERSION)-$(RELEASE)

rpm: $(war)
	@echo rpmbuild

$(core):
	./build.sh rundeck_core

$(war):
	./build.sh rundeckapp

$(launcher):
	./build.sh rundeckapp
	
clean:
	echo "Cleaning..."
	#clean localrepo of build artifacts
	rm -r ${BUILD_ROOT}/localrepo/rundeck*

	#remove rundeckapp lib dir which may contain previously built jars
	rm	${BUILD_ROOT}/rundeckapp/lib/rundeck*.jar

	#clean build target dirs
	rm -rf $BASEDIR/core/target
	rm -r $BASEDIR/rundeckapp/target

	#clean intermediate maven repo dirs of build artifacts
	rm -r $BASEDIR/maven/repository/rundeck*

	echo "Cleaned local build artifacts and targets."
