SHELL=/bin/bash

.PHONY: rundeck.rpm

rundeck.rpm:
	mkdir -p dist/RPMS/noarch
	mkdir -p target/var/lib/rundeck
	mkdir -p target/var/run/rundeck/exp/webapp
	mkdir -p target/var/run/rundeck/{data,work}
	mkdir -p target/var/log/rundeck
	cp -R root/* target
	cp -R ../core/target/tools/lib/*.jar target/var/lib/rundeck
	unzip -o ../rundeckapp/target/rundeck-1.0.0.war -d target/var/run/rundeck/exp/webapp/
	rpmbuild -bb --target=noarch --define "_topdir ${PWD}/dist" --buildroot="${PWD}/target" rundeck.spec 
    
clean:
	rm -rf target dist/RPMS/noarch
