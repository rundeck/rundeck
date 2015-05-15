% CLI Tool Authentication
% Greg Schueler
% May 14, 2015

The CLI tools (except `rd-acl`) use the Rundeck API to communicate to the Rundeck server.

Authentication to the server can happen in several ways:

# Static configuration

The default mechanism is to use these properties stored in the `framework.properties` file:

* `framework.server.url` The URL to connect to Rundeck
* `framework.server.username` The username
* `framework.server.password` The password

This requires storing the username/password in plaintext on the filesystem.

# Environment Variables

These environment variable can be used to define the connection and authentication data:

* `RUNDECK_URL` The URL to connect to Rundeck
* `RUNDECK_USER` The username
* `RUNDECK_PASS` The password

Example:

```
RUNDECK_URL=http://madmartigan.local:4440 RUNDECK_USER=admin RUNDECK_PASS=admin rd-jobs list -p test
```