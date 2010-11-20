SHELL=/bin/bash

BUILD_NUMBER?=snapshot.$(shell date "+%Y%m%d%H%M")
RPMBUILD_DEFINES = --define "_topdir ${PWD}/dist" 
RPMBUILD_DEFINES+= --define "_buildnumber $(BUILD_NUMBER)"

.PHONY: rundeck.rpm

rpm:
	@mkdir -p dist/RPMS/noarch
	@mkdir -p target/usr/bin
	@mkdir -p target/var/lib/rundeck
	@mkdir -p target/var/lib/rundeck/exp/webapp
	@mkdir -p target/var/lib/rundeck/{data,work,logs,cli}
	@mkdir -p target/var/log/rundeck
	@mkdir -p target/var/rundeck/projects
	@cp -R root/* target
	@cp -R ../core/target/tools/lib/*.jar target/var/lib/rundeck/cli
	@find ../core/target/bin ! -regex ".*.bat$$" -and -type f | sed -e /rd-check/d -e /rd-setup/d | xargs -I Z cp Z target/usr/bin
	@unzip -oq ../rundeckapp/target/rundeck-1.0.0.war -d target/var/lib/rundeck/exp/webapp/
	ln -s /var/lib/rundeck/exp/webapp/WEB-INF/classes/log4j.properties; mv log4j.properties target/var/lib/rundeck/exp/webapp/WEB-INF/classes
	rpmbuild -bb --target=noarch-linux $(RPMBUILD_DEFINES) --buildroot="${PWD}/target" rundeck.spec 
    
clean:
	rm -rf target dist/RPMS/noarch
