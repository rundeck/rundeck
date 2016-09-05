# Docker tests for remote dispatch

Test plan:

1. start rundeck on node `rundeck1`
2. start sshd on `hostnode`
	1. generate ssh key, copy to shared `/home/rundeck/resources` volume
	2. generate rundeck resource.xml
3. in rundeck1, upload hostnode ssh key to key storage for key-storage tests.
4. run remote dispatch tests in rundeck1

# Run

	bash test.sh

This uses docker-compose to run tests.  It will by default download the rundeck-launcher jar for version
2.6.9. 

Override the version to use:

	RUNDECK_VERSION=2.6.10 bash test.sh

Override the download URL:
	
	LAUNCHER_URL=http://... bash test.sh

Or use a local launcher jar file

	cp /some/path/rundeck-launcher-x.y.z.jar ./rundeck-launcher.jar
	bash test.sh
