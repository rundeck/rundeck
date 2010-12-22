SHELL=/bin/bash

TAG?=snapshot

RPMBUILD_DEFINES = --define "_topdir ${PWD}/dist" 

ifndef BUILD_NUMBER
ALPHATAG?=$(shell date "+%Y%m%d%H%M")$(TAG)
else
ALPHATAG?=$(TAG)
RPMBUILD_DEFINES+= --define "_buildnumber $(BUILD_NUMBER)"
endif

ifndef GA_RELEASE
RPMBUILD_DEFINES+= --define "_alphatag $(ALPHATAG)"
endif

.PHONY: rpm

rpm:
	@mkdir -p dist/RPMS/noarch
	@mkdir -p dist/SOURCES
	@mkdir -p dist/SOURCES/usr/bin
	@mkdir -p dist/SOURCES/var/lib/rundeck
	@mkdir -p dist/SOURCES/var/lib/rundeck/exp/webapp
	@mkdir -p dist/SOURCES/var/lib/rundeck/{data,work,logs,cli}
	@mkdir -p dist/SOURCES/var/log/rundeck
	@mkdir -p dist/SOURCES/var/lib/rundeck/var/tmp
	@mkdir -p dist/SOURCES/var/rundeck/projects
	@mkdir -p dist/SOURCES/usr/share/man/man{1,5}
	@mkdir -p dist/SOURCES/usr/share/man/en/man{1,5}
	@cp -R root/* dist/SOURCES
	@cp ../docs/en/manpages/man1/*.1.gz dist/SOURCES/usr/share/man/man1
	@cp ../docs/en/manpages/man5/*.5.gz dist/SOURCES/usr/share/man/man5
	@cp ../docs/en/manpages/man1/*.1.gz dist/SOURCES/usr/share/man/en/man1
	@cp ../docs/en/manpages/man5/*.5.gz dist/SOURCES/usr/share/man/en/man5
	@cp -R ../core/target/tools/lib/*.jar dist/SOURCES/var/lib/rundeck/cli
	@cp ../rundeckapp/target/rundeck-server-*.jar dist/SOURCES/var/lib/rundeck/bootstrap	
	@find ../core/target/bin ! -regex ".*.bat$$" -and -type f | sed -e /rd-check/d -e /rd-setup/d | xargs -I Z cp Z dist/SOURCES/usr/bin
	@unzip -oq ../rundeckapp/target/rundeck-1.0.0.war -d dist/SOURCES/var/lib/rundeck/exp/webapp/
	@ln -s /var/lib/rundeck/exp/webapp/WEB-INF/classes/log4j.properties; mv log4j.properties dist/SOURCES/var/lib/rundeck/exp/webapp/WEB-INF/classes
	rpmbuild -bb --target=noarch-linux $(RPMBUILD_DEFINES) --buildroot ${PWD}/dist/SOURCES rundeck.spec 
    
clean:
	rm -rf target dist/
