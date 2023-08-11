# Rundeck API Test suite

This uses testcontainers to spin up a docker container running Rundeck, and
runs Spock based API tests.

## Running against a docker image

This runs the tests against the `rundeck/rundeck` docker image by default.
```
./gradlew :api-tests:apiTest
```

Env vars to control docker image usage:

- `TEST_RUNDECK_IMAGE`: The docker image to use (default: `rundeck/rundeck`)
- `TEST_RUNDECK_CONTAINER_PORT`: The port to expose on the host (e.g. 8080) (default: `4440`)
- `TEST_RUNDECK_CONTAINER_CONTEXT`: The context path to use for the container (e.g. `/rundeck`). Must start with `/`. (
  default: blank)
- `TEST_RUNDECK_CONTAINER_TOKEN`: The API token to use for authentication. (default: `admintoken`)

## Running against external URL

You can run the tests against an external Rundeck instance by setting these env vars:

* `TEST_RUNDECK_URL`: The URL of the Rundeck instance
  * This should be the base URL of the server, but not include the `/api` path
* `TEST_RUNDECK_TOKEN`: The API token to use for authentication


## Adding a test

Create a new Spock "*Spec" class in the `src/test/groovy` directory.

Extend the `Base` class to share the same Rundeck container instance with all the tests.
