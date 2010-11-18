SHELL=/bin/bash

BUILD_NUMBER?=snapshot
RPMBUILD_DEFINES = --define "_topdir ${PWD}/dist" 
RPMBUILD_DEFINES+= --define "_buildnumber $(BUILD_NUMBER)"

.PHONY: rundeck.rpm

rpm:
	@mkdir -p dist/RPMS/noarch
	@mkdir -p target/usr/bin
	@mkdir -p target/var/lib/rundeck
	@mkdir -p target/var/run/rundeck/exp/webapp
	@mkdir -p target/var/run/rundeck/{data,work,logs}
	@mkdir -p target/var/log/rundeck
	@mkdir -p target/var/rundeck/projects
	@cp -R root/* target
	@cp -R ../core/target/tools/lib/*.jar target/var/lib/rundeck
	@find ../core/target/bin ! -regex ".*.bat$$" -and -type f | xargs -I Z cp Z target/usr/bin
	@unzip -oq ../rundeckapp/target/rundeck-1.0.0.war -d target/var/run/rundeck/exp/webapp/
	rpmbuild -bb --target=noarch $(RPMBUILD_DEFINES) --buildroot="${PWD}/target" rundeck.spec 
    
clean:
	rm -rf target dist/RPMS/noarch
