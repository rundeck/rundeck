# Rundeck API Test suite

This uses testcontainers to spin up a docker container running Rundeck, and
runs Spock based API tests.

## Running against a docker image

This runs the tests against the `rundeck/rundeck:SNAPSHOT` docker image by default.
```
./gradlew :api-tests:apiTest
```

You can set the env var `TEST_RUNDECK_IMAGE` to change the image used.

## Running against external URL

You can run the tests against an external Rundeck instance by setting these env vars:

* `TEST_RUNDECK_URL`: The URL of the Rundeck instance
  * This should be the base URL of the server, but not include the `/api` path
* `TEST_RUNDECK_TOKEN`: The API token to use for authentication


## Adding a test

Create a new Spock "*Spec" class in the `src/test/groovy` directory.

Extend the `Base` class to share the same Rundeck container instance with all the tests.
