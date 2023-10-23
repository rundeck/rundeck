# Rundeck functional tests

This uses docker compose test containers to spin up a docker container running Rundeck, and
runs Spock based API tests.

## Running against a docker image

This runs the tests against the `rundeck/rundeck:SNAPSHOT` docker image by default.
```
./gradlew :functional-test:{AVAILABLE_TEST_TASK}
```
If additional information is needed when running the task, you can the gradle flag "-i"
Available tasks listed below

Env vars to control docker image usage:

- `TEST_IMAGE`: The docker image to use (default: `rundeck/rundeck:SNAPSHOT`)
- `TEST_RUNDECK_CONTAINER_SERVICE`: The docker service to expose (default: `rundeck`)
- `TEST_RUNDECK_CONTAINER_PORT`: The port to expose on the host (e.g. 8080) (default: `4440`)
- `TEST_RUNDECK_CONTAINER_CONTEXT`: The context path to use for the container (e.g. `/rundeck`). Must start with `/`. (
  default: blank)
- `TEST_RUNDECK_CONTAINER_TOKEN`: The API token to use for authentication. (default: `admintoken`)
- `TEST_RUNDECK_GRAILS_URL`: This value is used as `RUNDECK_GRAILS_URL` (default: `http://localhost:4440`)
- `WAR_FILE_LOCATION`: (optional for tomcatTest task) If set, it will use this path to get the war file and run the tomcatTest
  task, if not it will try to get the war from the build process

## Running against external URL

You can run the tests against an external Rundeck instance by setting these env vars:

* `TEST_RUNDECK_URL`: The URL of the Rundeck instance
    * This should be the base URL of the server, but not include the `/api` path
* `TEST_RUNDECK_TOKEN`: The API token to use for authentication
* `RUNDECK_TEST_USER`: The username to use for authentication (default: `admin`)
* `RUNDECK_TEST_PASS`: The password to use for authentication (default: `admin123`)

## Adding a new test task

Create a new gradle test task where you must specify the docker compose file to use and the spock configuration

* `COMPOSE_PATH`: Relative path to the docker compose file
* `TEST_IMAGE`: The docker image to use (default: `rundeck/rundeck:SNAPSHOT`)
* `spock.configuration`: Relative path to the spock configuration file (spock-configs path)

## Adding a test

Create a new Spock "*Spec" class in the `src/test/groovy` directory.

Extend the `BaseContainer` class to share the same Rundeck container instance with all the tests.

All tests must be annotated so the spock configuration used for the test task knows which tests to run

# Current Rundeck Test tasks

* `apiTest`: It runs all tests annotated with @APITest
* `seleniumCoreTest`: It runs all tests annotated with @SeleniumCoreTest
* `tomcatTest`: It runs all tests annotated with @APITest using a rundeck running on tomcat