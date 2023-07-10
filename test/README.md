To launch the following integration test, make sure to have rundeck launched and listening on port 4440 (default).

export vars pointing at correct directories, and specify name of xmlstarlet executable:
```shell
    export TMP_DIR=/tmp \
    export RDECK_BASE=/var/lib/rundeck \
    export RDECK_ETC=/etc/rundeck \
    export RDECK_PROJECTS=/var/rundeck/projects \
    export XMLSTARLET=xmlstarlet \
    sh src/test.sh http://localhost:4440 admin admin
```

# Run Docker Tests
These tests run against a docker image built from a `<rundeck-oss-package-name>.war` file located in `rundeck/rundeckapp/build/libs/`, make sure it's present before running any of the tests below.   

Before running any test, export the following variable to prevent scripts from stopping containers after exiting
```shell
export DEBUG_RD_SERVER=true
```

## Docker Test
```shell
bash test/run-docker-tests.sh
```

## LDAP Bind Test
```shell
DOCKER_COMPOSE_SPEC=docker-compose-ldap-binding-test.yaml bash test/run-docker-ldap-tests.sh
```

## LDAP Test
```shell
bash test/run-docker-ldap-tests.sh
```

## PAM Test
```shell
bash test/run-docker-pam-tests.sh
```

## SSL Test
```shell
bash test/run-docker-ssl-tests.sh
```

## Tomcat 8 API Test
```shell
# Run tests test/api/test-*.sh in tomcat 8 environment
bash test/run-docker-tomcat-tests.sh 8-jdk11
```

## Tomcat 9 API Test
```shell
# Run tests test/api/test-*.sh in tomcat 9 environment
bash test/run-docker-tomcat-tests.sh 9-jdk11
```

## Blocklist Test
```shell
bash test/run-docker-plugin-blocklist-test.sh
```

## Ansible Test
```shell
bash test/run-docker-ansible-tests.sh
```

## API Test
```shell
# Run tests test/api/test-*.sh
DOCKER_COMPOSE_SPEC=docker-compose-api-mysql.yaml bash test/run-docker-api-tests.sh
```
