% API Reference | Version ${APIVERS}
% Alex Honor; Greg Schueler
% November 20, 2010

Rundeck provides a Web API for use with your application.  

## API Version Number

The current API version is `${APIVERS}`.

For API endpoints described in this document, the *minimum* API version required for their
use is indicated by the URL used, e.g.:

    /api/2/system/info
    /api/1/projects

This means you must use at least the API version indicated to access the
endpoint, unless otherwise noted. Some features or functionality for the
endpoint may only be supported in later versions.

The API Version Number is required to be included in all API calls within the URL.

If the version number is not included or if the requested version number is unsupported, then the API call will fail.  The error response will include the code "api-version-unsupported" and have HTTP status code of `400 Bad Request`:


`Content-Type: application/xml`:

~~~~~~~~~~~~~~~~~~~ {.xml}
<result error="true" apiversion="2">
    <error code="api-version-unsupported">
        <message>
        Unsupported API Version "1". API Request: /rundeck/api/1/project/test/jobs. Reason: Minimum supported version: 2
        </message>
    </error>
</result>
~~~~~~~~~~~~~~~~~~~


`Content-Type: application/json`:

~~~~~~~~~~~~~~~~~~~ {.json}
{
  "error": true,
  "apiversion": 14,
  "errorCode": "api.error.api-version.unsupported",
  "message": "Unsupported API Version \"1\". API Request: /api/1/project/test/resources. Reason: Minimum supported version: 2"
}
~~~~~~~~~~~~~~~~~~~

## Index Links

View the [Index](#index) listing API paths.

## Changes

Changes introduced by API Version number:

**Version 17**:

* New Endpoints.
    - [`/api/17/scheduler/server/[UUID]/jobs`][/api/V/scheduler/server/[UUID]/jobs] - List scheduled jobs owned by the server with given UUID.
    - [`/api/17/scheduler/jobs`][/api/V/scheduler/jobs] - List scheduled jobs owned by the target server.
    - [`/api/17/system/logstorage`][/api/V/system/logstorage] - Get stats about the Log File storage system.
    - [`/api/17/system/logstorage/incomplete`][/api/V/system/logstorage/incomplete] - List all executions with incomplete logstorage.
    - [`/api/17/system/logstorage/incomplete/resume`][/api/V/system/logstorage/incomplete/resume] - Resume incomplete log storage processing.
    
* Updated Endpoints.
    - [`/api/17/project/[PROJECT]/jobs`][/api/V/project/[PROJECT]/jobs] 
        - Response now includes whether a job is enabled, scheduled, schedule is enabled, and in Cluster mode includes the cluster mode server UUID of the schedule owner, and whether that is the current server or not.
        - add `?scheduledFilter=true/false` returns scheduled/unscheduled jobs only
        - and `?serverNodeUUIDFilter=[uuid]` returns scheduled jobs owned by the given cluster member
    - [`/api/17/scheduler/takeover`][/api/V/scheduler/takeover]
        - Response now includes previous scheduler owner UUID for jobs.
    - [`/api/17/scheduler/takeover`][/api/V/scheduler/takeover] - Can specify a single job ID to takeover.

**Version 16**:

* New Endpoints.
    - [`/api/16/jobs/execution/enable`][/api/V/jobs/execution/enable] - Enable execution for multiple jobs
    - [`/api/16/jobs/execution/disable`][/api/V/jobs/execution/disable] - Disable execution for multiple jobs
    - [`/api/16/jobs/schedule/enable`][/api/V/jobs/schedule/enable] - Enable schedule for multiple jobs
    - [`/api/16/jobs/schedule/disable`][/api/V/jobs/schedule/disable] - Disable schedule for multiple jobs


**Version 15**:

* New Endpoints.
    - [`/api/15/project/[PROJECT]/scm/[INTEGRATION]/plugins`][/api/V/project/[PROJECT]/scm/[INTEGRATION]/plugins] - List SCM plugins for a project.
    - [`/api/15/project/[PROJECT]/scm/[INTEGRATION]/plugin/[TYPE]/input`][/api/V/project/[PROJECT]/scm/[INTEGRATION]/plugin/[TYPE]/input] - Get SCM plugin setup input fields.
    - [`/api/15/project/[PROJECT]/scm/[INTEGRATION]/plugin/[TYPE]/setup`][/api/V/project/[PROJECT]/scm/[INTEGRATION]/plugin/[TYPE]/setup] - Setup SCM for a project.
    - [`/api/15/project/[PROJECT]/scm/[INTEGRATION]/plugin/[TYPE]/enable`][/api/V/project/[PROJECT]/scm/[INTEGRATION]/plugin/[TYPE]/enable] - Enable SCM for a project.
    - [`/api/15/project/[PROJECT]/scm/[INTEGRATION]/plugin/[TYPE]/disable`][/api/V/project/[PROJECT]/scm/[INTEGRATION]/plugin/[TYPE]/disable] - Disable SCM for a project.
    - [`/api/15/project/[PROJECT]/scm/[INTEGRATION]/status`][/api/V/project/[PROJECT]/scm/[INTEGRATION]/status] - Get SCM status for a project.
    - [`/api/15/project/[PROJECT]/scm/[INTEGRATION]/config`][/api/V/project/[PROJECT]/scm/[INTEGRATION]/config] - Get SCM config for a project.
    - [`/api/15/project/[PROJECT]/scm/[INTEGRATION]/action/[ACTION_ID]/input`][/api/V/project/[PROJECT]/scm/[INTEGRATION]/action/[ACTION_ID]/input] - Get Project SCM Action Input Fields.
    - [`/api/15/project/[PROJECT]/scm/[INTEGRATION]/action/[ACTION_ID]`][/api/V/project/[PROJECT]/scm/[INTEGRATION]/action/[ACTION_ID]] - Perform SCM action for a project.
    
    - [`/api/15/job/[ID]/scm/[INTEGRATION]/status`][/api/V/job/[ID]/scm/[INTEGRATION]/status] - Get SCM status for a Job.
    - [`/api/15/job/[ID]/scm/[INTEGRATION]/action/[ACTION_ID]`][/api/V/job/[ID]/scm/[INTEGRATION]/action/[ACTION_ID]] - Perform SCM action for a Job.
    - [`/api/15/job/[ID]/scm/[INTEGRATION]/action/[ACTION_ID]/input`][/api/V/job/[ID]/scm/[INTEGRATION]/action/[ACTION_ID]/input] - Get Job SCM Action Input Fields
    

**Version 14**:

**Note**: this document now has an [Index](#index) listing API paths.

* Deprecated Endpoints.  These endpoints are deprecated, and new versions are added which include the Project name in the URL path
    - `/api/14/executions/running` replacement: [`/api/14/project/[PROJECT*]/executions/running`][/api/V/project/[PROJECT*]/executions/running]
    - `/api/14/executions` replacement: [`/api/14/project/[PROJECT]/executions`][/api/V/project/[PROJECT]/executions]
    - `/api/14/history` replacement: [`/api/14/project/[PROJECT]/history`][/api/V/project/[PROJECT]/history]
    - `/api/14/jobs/export` replacement: [`/api/14/project/[PROJECT]/jobs/export`][/api/V/project/[PROJECT]/jobs/export]
    - `/api/14/jobs/import` replacement: [`/api/14/project/[PROJECT]/jobs/import`][/api/V/project/[PROJECT]/jobs/import]
    - `/api/14/jobs` replacement: [`/api/14/project/[PROJECT]/jobs`][/api/V/project/[PROJECT]/jobs]
    - `/api/14/resource/[NAME]` replacement: [`/api/14/project/[PROJECT]/resource/[NAME]`][/api/V/project/[PROJECT]/resource/[NAME]]
    - `/api/14/resources(/*)` replacement: [`/api/14/project/[PROJECT]/resources`][/api/V/project/[PROJECT]/resources]
    - `/api/14/run/command` replacement: [`/api/14/project/[PROJECT]/run/command`][/api/V/project/[PROJECT]/run/command]
    - `/api/14/run/script` replacement: [`/api/14/project/[PROJECT]/run/script`][/api/V/project/[PROJECT]/run/script]
    - `/api/14/run/url` replacement: [`/api/14/project/[PROJECT]/run/url`][/api/V/project/[PROJECT]/run/url]
* Deprecated Endpoints with no replacement
    - `/api/2/project/[PROJECT]/resources/refresh`
* New Endpoints
    - [`/api/14/system/executions/enable`][/api/V/system/executions/enable] - Enable executions (ACTIVE mode)
    - [`/api/14/system/executions/disable`][/api/V/system/executions/disable] - Disable executions (PASSIVE mode)
    - [`/api/14/system/acl/*`][/api/V/system/acl/*] - Manage system ACLs
    - [`/api/14/project/[PROJECT]/acl/*`][/api/V/project/[PROJECT]/acl/*] - Manage project ACLs
    - [`/api/14/job/[ID]/execution/enable`][/api/V/job/[ID]/execution/enable] - Enable executions for a job
    - [`/api/14/job/[ID]/execution/disable`][/api/V/job/[ID]/execution/disable] - Disable executions for a job
    - [`/api/14/job/[ID]/schedule/enable`][/api/V/job/[ID]/schedule/enable] - Enable scheduling for a job
    - [`/api/14/job/[ID]/schedule/disable`][/api/V/job/[ID]/schedule/disable] - Disable scheduling for a job
* New Endpoints, replacing deprecated versions:
    - [`/api/14/project/[PROJECT*]/executions/running`][/api/V/project/[PROJECT*]/executions/running]
    - [`/api/14/project/[PROJECT]/executions`][/api/V/project/[PROJECT]/executions]
    - [`/api/14/project/[PROJECT]/history`][/api/V/project/[PROJECT]/history]
    - [`/api/14/project/[PROJECT]/jobs/export`][/api/V/project/[PROJECT]/jobs/export]
    - [`/api/14/project/[PROJECT]/jobs/import`][/api/V/project/[PROJECT]/jobs/import]
    - [`/api/14/project/[PROJECT]/resource/[NAME]`][/api/V/project/[PROJECT]/resource/[NAME]]
    - `/api/14/project/[PROJECT]/resources(/*)`
    - [`/api/14/project/[PROJECT]/run/command`][/api/V/project/[PROJECT]/run/command]
    - [`/api/14/project/[PROJECT]/run/script`][/api/V/project/[PROJECT]/run/script]
    - [`/api/14/project/[PROJECT]/run/url`][/api/V/project/[PROJECT]/run/url]
* Added JSON support for endpoints, when using API v14:
    - [`/api/14/execution/[ID]/abort`][/api/V/execution/[ID]/abort]
    - [`/api/14/execution/[ID]`][/api/V/execution/[ID]]
    - [`/api/14/job/[ID]/executions`][/api/V/job/[ID]/executions]
    - [`/api/14/job/[ID]/run`][/api/V/job/[ID]/run] and [`POST /api/14/job/[ID]/executions`][POST /api/V/job/[ID]/executions]
    - [`/api/14/jobs/delete`][/api/V/jobs/delete]
    - [`/api/14/project/[PROJECT*]/executions/running`][/api/V/project/[PROJECT*]/executions/running]
    - [`/api/14/project/[PROJECT]/executions`][/api/V/project/[PROJECT]/executions]
    - [`/api/14/project/[PROJECT]/history`][/api/V/project/[PROJECT]/history]
    - [`/api/14/project/[PROJECT]/jobs/import`][/api/V/project/[PROJECT]/jobs/import]
    - [`/api/14/project/[PROJECT]/jobs`][/api/V/project/[PROJECT]/jobs]
    - [`/api/14/project/[PROJECT]/resource/[NAME]`][/api/V/project/[PROJECT]/resource/[NAME]]
    - [`/api/14/project/[PROJECT]/resources`][/api/V/project/[PROJECT]/resources]
    - [`/api/14/project/[PROJECT]/run/command`][/api/V/project/[PROJECT]/run/command]
    - [`/api/14/project/[PROJECT]/run/script`][/api/V/project/[PROJECT]/run/script]
    - [`/api/14/project/[PROJECT]/run/url`][/api/V/project/[PROJECT]/run/url]
    - [`/api/14/system/info`][/api/V/system/info]
* TODO json support:
    - [`/api/14/project/[PROJECT]/jobs/export`][/api/V/project/[PROJECT]/jobs/export]
* Updated endpoints:
    - [`/api/14/job/[ID]/run`][/api/V/job/[ID]/run] action `GET` is no longer allowed (v14+), `POST` is required. For POST, this endpoint is now equivalent to `/api/14/job/[ID]/executions`. JSON request content is now supported.
    - [`/api/14/project/[PROJECT]/jobs/import`][/api/V/project/[PROJECT]/jobs/import]
        * Both XML and YAML job definitions can now be posted directly using the appropriate MIME type
        * Add API `href` and GUI `permalink` values into XML response
        * JSON response support
    - [`/api/14/project/[PROJECT]/jobs`][/api/V/project/[PROJECT]/jobs] - added API/GUI href/permalink to XML responses.
    - [`/api/14/execution/[ID]/abort`][/api/V/execution/[ID]/abort] - added API/GUI href/permalink to XML responses.
    - [`/api/14/project/[PROJECT]/history`][/api/V/project/[PROJECT]/history] - added API/GUI href/permalink to XML responses.
    - `/api/14/project/[PROJECT]/run/*` - added API/GUI href/permalink to XML responses for adhoc command/script/url.
    - [`/api/14/system/info`][/api/V/system/info] - added information about Rundeck Execution Mode
    - [`/api/14/project/[PROJECT]/import`][/api/V/project/[PROJECT]/import] - Added parameters for importing Configuration and ACL Policies from the archive.
* Endpoints promoted out of "incubator" status:
    - [`/api/14/scheduler/takeover`][/api/V/scheduler/takeover] - Can specify `all` servers, or jobs within a specific `project`. Added API/GUI href/permalink to XML responses for adhoc command/script/url. Note: `href` was modified as mentioned below.
* Modified `href` meaning for XML responses:
    * Some endpoints that included a `href` value in XML responses used the link that was appropriate
    for an end user to use in a web browser,
    essentially the permalink to the GUI view for the linked object.
    When using API v14, these URLs now point to the API,
    and a new attribute `permalink` will be included to link to the GUI view for the object.
    * Using an API version 13 or earlier will retain the old behavior of `href` in XML responses.

Corrections:

* The response for [DELETE /api/V/job/[ID]][] incorrectly stated it would return XML response, when the actual response is `204 No Content`.

**Version 13**:

* New endpoints
    - `/api/13/project/[PROJECT]/readme.md` and `/api/13/project/[PROJECT]/motd.md`
        - [Project Readme File](#project-readme-file) (`GET`, `PUT`, `DELETE`)

**Version 12**:

* New endpoints
    - `POST /api/12/executions/delete`
        - [Bulk delete executions](#bulk-delete-executions)
* Updated endpoints
    - `DELETE /api/12/execution/[ID]`
        - [Delete an execution](#delete-an-execution)
    - `DELETE /api/12/job/[ID]/executions`
        - [Delete all executions for a job](#delete-all-executions-for-a-job)
    - `POST /api/12/job/[ID]/executions`
        - [Run a Job](#running-a-job)


**Version 11**:

**Update**: The URL path for Token access was corrected.

In this version, all new and updated endpoints support XML or JSON request and response content where appropriate.

**Modified XML Response format**:

- For endpoints requiring API version 11 *only*, the default for XML responses is to *no longer* include a `<result>` element around the data.
- For API clients that expect to see the `<result>` element, a request header of `X-Rundeck-API-XML-Response-Wrapper: true` will restore it.
- For endpoint requests for API version 10 and earlier, the `<result>` element will be sent as it has been (described in [Response Format][])

[Response Format]: #response-format

**Version 11**:

* New endpoints
    - `/api/11/project/[PROJECT]/config`
        - PUT and GET for [Project Configuration](#project-configuration)
    - `/api/11/project/[PROJECT]/config/[KEY]`
        + PUT, GET, DELETE for [Project Configuration Keys](#project-configuration-keys)
    - `/api/11/project/[PROJECT]/export`
        + GET to retrieve archive of a project - [Project Archive Export](#project-archive-export)
    - `/api/11/project/[PROJECT]/import`
        + PUT to import an archive to a project - [Project Archive Import](#project-archive-import)
    - `/api/11/storage/keys/[PATH]`
        + GET, POST, PUT, DELETE: manage stored keys - [Key Storage](#key-storage)
    - `/api/11/tokens`
        + GET: List all auth tokens - [List Tokens](#list-tokens)
        + POST: Generate a token for a user - [Create a Token](#create-a-token)
    - `/api/11/tokens/[user]`
        + GET: List auth tokens defined for a user - [List Tokens](#list-tokens)
        + POST: Generate a token for a user - [Create a Token](#create-a-token)
    - `/api/11/token/[tokenID]`
        + GET: get a token - [Get a token](#get-a-tokens)
        + DELETE: delete a token - [Delete a Token](#delete-a-token)
* Updated endpoints
    - `/api/11/project/[PROJECT]`
        + DELETE method can delete a project - [Project Deletion](#project-deletion)
        + GET method response updated - [Getting Project Info](#getting-project-info)
    - `/api/11/projects`
        + POST method can be used to create a new project - [Project creation](#project-creation)

**Version 10**:

* New endpoints
    - `/api/10/execution/[ID]/state` - [Execution State](#execution-state)
        + Retrieve workflow step and node state information
    - `/api/10/execution/[ID]/output/state` - [Execution Output with State](#execution-output-with-state)
        + Retrieve log output with state change information
    - `/api/10/execution/[ID]/output/node/[NODENAME]` and `/api/10/execution/[ID]/output/step/[STEPCTX]` - [Execution Output](#execution-output)
        + Retrieve log output for a particular node or step
        + Can combine both node and step context
* Updated endpoints
    - `/api/10/execution/[ID]` - [Execution Info](#execution-info)
        + added `successfulNodes` and `failedNodes` detail.
        + added `job/options` data

**Version 9**:

* Updated endpoints
    * `/api/9/executions/running` - [Listing Running Executions](#listing-running-executions)
        * Allow `project=*` to list running executions across all projects
        * Result data now includes `project` attribute for each `<execution>`.
    * `/api/9/jobs/import` - [Importing Jobs](#importing-jobs)
        * Add `uuidOption` parameter to allow removing imported UUIDs to avoid creation conflicts.

**Version 8**:

* Updated endpoints
    * `/api/8/run/script` and `/api/8/run/url` -  [Running Adhoc Scripts](#running-adhoc-scripts) and [Running Adhoc Script URLs](#running-adhoc-script-urls)
        * Added two optional parameters for `scriptInterpreter` and `interpreterArgsQuoted`
    * `/api/8/jobs/import` -  [Importing Jobs](#importing-jobs)
        * Added an optional parameter `project` which will override any project defined in the Job definition contexts.  If used, the job definitions do not need a `project` value in them.
* Removed endpoints
    * `/api/1/report/create`
      * Removed due to History no longer supporting arbitrary event reports.

**Version 7**:

* Add **Incubator** endpoint
    * PUT `/api/7/incubator/jobs/takeoverSchedule` - [Takeover Schedule in Cluster Mode](#takeover-schedule-in-cluster-mode)
        * incubating feature for cluster mode schedule takeover

**Version 6**:

* Updated endpoints
    * `/api/6/execution/[ID]/output` - [Execution Output](#execution-output)
        * XML format has changed for API v6: entry log content is now specified as a `log` attribute value
        * The old XML format will still be used for queries using `/api/5`
        * Fixed invalid XML when no format was specified and XML was used by default
        * **documentation typo fixed**: the JSON format incorrectly specified the log text key as 'mesg', corrected to 'log'

**Version 5**:

Added in Rundeck 1.4.6, 1.5.1:

* New feature for some endpoints:
    * new `asUser` parameter can record an action (run or abort) as having been performed by another user
    * Affected endpoints
        * [Running a Job](#running-a-job)
        * [Running Adhoc Commands](#running-adhoc-commands)
        * [Running Adhoc Scripts](#running-adhoc-scripts)
        * [Running Adhoc Script URLs](#running-adhoc-script-urls)
        * [Aborting Executions](#aborting-executions)

* New endpoint
    * `/api/5/jobs/delete` - [Bulk Job Delete](#bulk-job-delete)
* New endpoint
    * `/api/5/execution/[ID]/output` - [Execution Output](#execution-output)
* New endpoint
    * `/api/5/executions` - [Execution Query](#execution-query)
* Updated endpoints
    * `/api/1/history` - [Listing History](#listing-history)
        * new filter parameters added for including or excluding reports for exact job group/name values: `jobListFilter` and `excludeJobListFilter`

**Version 4**:

* New endpoint
    * `/api/4/run/url` - [Running Adhoc Script URLs](#running-adhoc-script-urls)

**Version 3**:

* Updated endpoints
    * `/api/1/resources` - [Listing Resources](#listing-resources)
        * `format` parameter can now use any supported Resource Format Parser format name.
    * `/api/2/project/[PROJECT]/resources` - [Updating and Listing Resources for a Project](#updating-and-listing-resources-for-a-project)
        * `POST` request Content-Type can be any MIME type supported by a Resource Format Parser plugin.

**Version 2**:

* New endpoints
    * `/api/2/project/[PROJECT]/jobs` - [Listing Jobs for a Project](#listing-jobs-for-a-project)
    * `/api/2/project/[PROJECT]/resources` - [Updating and Listing Resources for a Project](#updating-and-listing-resources-for-a-project)
    * `/api/2/project/[PROJECT]/resources/refresh` - [Refreshing Resources for a Project](#refreshing-resources-for-a-project)
* Updated endpoints
    * `/api/1/jobs` - [Listing Jobs](#listing-jobs)
        * Additional parameters added

## URLs

The Rundeck server has a "Base URL", where you access the server. Your Rundeck Server URL may look like: `http://myserver:4440`.

The root URL path for all calls to the API in this version is:

    $RUNDECK_SERVER_URL/api/2

## XML and JSON

The API supports both XML and JSON.  Some import/export features support YAML or `text/plain` formatted documents, but XML and JSON are used for all API-level information.

As of API version 14, all endpoints support JSON format, with content type `application/json`, with one exception ([/api/V/project/[PROJECT]/jobs/export][]).

JSON results can be retrieved by sending the HTTP "Accept" header with a `application/json` value.  JSON request content is supported when the HTTP "Content-Type" header specifies `application/json`.

If an "Accept" header is not specified, then the response will be either the same format as the request content (for POST, or PUT requests), or XML by default.

Some endpoints also support using a `format` query parameter to specify the expected output format.

## Authentication

Authentication can be done in two different ways, either with Token based authentication,
or with a username and password.

Note that in either case, **it is recommended that you enable SSL Support for the Rundeck server** so that communication is encrypted at all times. For more information about using SSL, see [Administration - Configuring Rundeck for SSL](../administration/configuring-ssl.html).

### Token Authentication

Token Authentication consists of including a string known as an "API Token" with every
request to the Rundeck API.

To obtain an API Token, you must first log in to the Rundeck GUI using a user account
that has "admin" credentials. Click on your username in the header of the page, and you will be shown your User Profile page.  From this page you can manage your API Tokens.  Click "Generate API Token" to create a new one.  The unique string that is shown is the API Token.

Alternately you can define tokens in static file, by setting the `rundeck.tokens.file` in [framework.properties](../administration/configuration-file-reference.html#framework.properties).

You must include one of the following with every HTTP request to the API:

* HTTP Header `X-Rundeck-Auth-Token` set to the API Token string

OR

* HTTP URL Parameter `authtoken` set to the API Token string

With that in place, you can make calls to the API as described in the rest of this
document, and you don't need to maintain any cookies between requests.

Examples:

Using the URL parameter to request the project list:

    GET /api/1/projects?authtoken=E4rNvVRV378knO9dp3d73O0cs1kd0kCd HTTP/1.1
    ...

Using the HTTP Header:

    GET /api/1/projects HTTP/1.1
    X-Rundeck-Auth-Token: E4rNvVRV378knO9dp3d73O0cs1kd0kCd
    ...

### Password Authentication

If using Password Authentication, you must perform the authentication steps prior to accessing the API.

This means that you must submit authentication parameters (username, password) to the "Authentication URL" and retain a Session Cookie.

The Session Cookie must be sent with all calls to the API to maintain the authenticated connection.

To submit authentication, submit a `POST` request to the URL:

    $RUNDECK_SERVER_URL/j_security_check

With these parameters:

* `j_username`: rundeck username
* `j_password`: password

If the response includes a redirect chain which includes or results in `$RUNDECK_SERVER_URL/user/login` or `$RUNDECK_SERVER_URL/user/error`, then the authentication request failed.

Otherwise, if the response is a redirect chain which results in `200 successful` response,  then the authentication was successful.  

The response should set a cookie named `JSESSIONID`.

## XML Response Format

For version 11 and later API requests, XML responses will have only the content indicated in the appropriate endpoint documentation.

For version 10 and earlier API requests, XML responses will have this document structure:

~~~~~~~~~~~~ {.xml}
<result success/error="true" apiversion="X">
    <!-- error included if error="true" -->
    <error>
        <message><!-- error message text --></message>
        <!-- ... multiple message elements -->
    </error>

    <!-- optional success element if declared for the endpoint -->
    <success>
        <message><!-- success message --></message>
    </success>

    <!--
        Specific API results..
    -->
</result>
~~~~~~~~~~~~

If an error occurred, then the `error` attribute of the `<result>` element will be "true". Otherwise a `success` attribute will have the value "true".

Some `<error>` responses will include a `code` attribute giving a specific type
of error code, in addition to the message.

The `apiversion` attribute will be set to the latest version of the API
supported by the server.

### Error codes ###

Defined error codes that may be returned as `<error code="...">`

`api-version-unsupported`

:    The specified API version is not supported for the requested resource

`unauthorized`

:    The requested action is not authorized and/or the connection is not authenticated.

### Item Lists ###

Many API requests will return an item list as a result.  These are typically in the form:

    <[items] count="x">
        <[item] ...>
        <[item] ...>
    </[items]>

Where the list of specific items are wrapped in a pluralized element name which contains a `count` attribute.

When an API path declares its results as an "Item List" this is the format that will be returned.


# API Contents

## Authentication Tokens ###

Authentication tokens can be managed via the API itself.

### List Tokens ####

List all tokens or all tokens for a specific user.

**Request:**

    GET /api/11/tokens
    GET /api/11/tokens/[USER]

**Response:**

`application/xml`:

All users:

~~~~ {.xml}
<tokens count='3' allusers='true'>
  <token id='DRUVEuCdENoPkUpDkcDcdd6PeKkPdurc' user='alice' />
  <token id='VprOpDeDP3KcK2dp37p5DoD6o53cc82D' user='bob' />
  <token id='EveKe1KSRORnUN28D09eERDN3OvO4S6N' user='frank' />
</tokens>
~~~~

For a specific user:

~~~~ {.xml}
<tokens count='1' user='alice'>
  <token id='DRUVEuCdENoPkUpDkcDcdd6PeKkPdurc' user='alice' />
</tokens>
~~~~

`application/json`:

~~~~ {.json}
[
  {
    "user": "alice",
    "id": "DRUVEuCdENoPkUpDkcDcdd6PeKkPdurc"
  },
  {
    "user": "bob",
    "id": "VprOpDeDP3KcK2dp37p5DoD6o53cc82D"
  },
  {
    "user": "frank",
    "id": "EveKe1KSRORnUN28D09eERDN3OvO4S6N"
  }
]
~~~~

### Get a token ####

Get a specified auth token.

**Request:**

    GET /api/11/token/[ID]

Response:

`application/xml`

~~~~ {.xml}
<token id='DuV0UoDUDkoR38Evd786cdRsed6uSNdP' user='alice' />
~~~~

`application/json`

~~~~ {.json}
{
  "user": "alice",
  "id": "DuV0UoDUDkoR38Evd786cdRsed6uSNdP"
}
~~~~

### Create a Token ####

Create a new token for a specific user.

**Request:**

    POST /api/11/tokens
    POST /api/11/tokens/[USER]

The user specified must either be part of the URL, or be part of the request content. If used in the URL, then the request content is ignored and can be empty.

`Content-type: application/xml`

~~~~ {.xml}
<user user="alice"/>
~~~~

`Content-type: application/json`

~~~~ {.json}
{ "user" : "alice" }
~~~~

Response:


`application/xml`

~~~~ {.xml}
<token id='DuV0UoDUDkoR38Evd786cdRsed6uSNdP' user='alice' />
~~~~

`application/json`

~~~~ {.json}
{
  "user": "alice",
  "id": "DuV0UoDUDkoR38Evd786cdRsed6uSNdP"
}
~~~~

### Delete a token ####

Delete a specified auth token.

**Request:**

    DELETE /api/11/token/[ID]

Response:

    204 No Content

## System Info ###

Get Rundeck server information and stats.

**Request:**

    GET /api/14/system/info

Parameters: none

**Response:**

Success response, with included system info and stats in this format:

`Content-Type: application/xml`:

~~~~~~~~~~~~~ {.xml}
<system>
    <timestamp epoch="1305909785806" unit="ms">
        <datetime>2011-05-20T16:43:05Z</datetime>
    </timestamp>
    <rundeck>
        <version>1.2.1</version>
        <apiversion>2</apiversion>
        <build>1.2.1-0-beta</build>
        <node>Venkman.local</node>
        <base>/Users/greg/rundeck121</base>
        <serverUUID>3E43E30D-F3D7-45AA-942A-04D5BAFED8CA</serverUUID>
    </rundeck>
    <executions active="true" executionMode="active" />
    <os>
        <arch>x86_64</arch>
        <name>Mac OS X</name>
        <version>10.6.7</version>
    </os>
    <jvm>
        <name>Java HotSpot(TM) 64-Bit Server VM</name>
        <vendor>Apple Inc.</vendor>
        <version>19.1-b02-334</version>
    </jvm>
    <stats>
        <uptime duration="300584" unit="ms">
            <since epoch="1305909485222" unit="ms">
                <datetime>2011-05-20T16:38:05Z</datetime>
            </since>
        </uptime>
        <cpu>
            <loadAverage unit="percent">0.40234375</loadAverage>
            <processors>4</processors>
        </cpu>
        <memory unit="byte">
            <max>477233152</max>
            <free>76626216</free>
            <total>257163264</total>
        </memory>
        <scheduler>
            <running>0</running>
            <threadPoolSize>10</threadPoolSize>
        </scheduler>
        <threads>
            <active>24</active>
        </threads>
    </stats>
    <metrics href='http://dignan:4440/metrics/metrics?pretty=true' contentType='text/json' />
    <threadDump href='http://dignan:4440/metrics/threads' contentType='text/plain' />
</system>
~~~~~~~~~~~~~~~


`Content-Type: application/json`:

~~~~~~~~~~~~~ {.json}
{
  "system": {
    "timestamp": {
      "epoch": 1431975278220,
      "unit": "ms",
      "datetime": "2015-05-18T18:54:38Z"
    },
    "rundeck": {
      "version": "2.5.2-SNAPSHOT",
      "build": "2.5.2-0-SNAPSHOT",
      "node": "madmartigan.local",
      "base": "/Users/greg/rundeck25",
      "apiversion": 14,
      "serverUUID": null
    },
    "executions":{
      "active":true,
      "executionMode":"active"
    },
    "os": {
      "arch": "x86_64",
      "name": "Mac OS X",
      "version": "10.10.3"
    },
    "jvm": {
      "name": "Java HotSpot(TM) 64-Bit Server VM",
      "vendor": "Oracle Corporation",
      "version": "1.7.0_71",
      "implementationVersion": "24.71-b01"
    },
    "stats": {
      "uptime": {
        "duration": 546776,
        "unit": "ms",
        "since": {
          "epoch": 1431974731444,
          "unit": "ms",
          "datetime": "2015-05-18T18:45:31Z"
        }
      },
      "cpu": {
        "loadAverage": {
          "unit": "percent",
          "average": 2.689453125
        },
        "processors": 8
      },
      "memory": {
        "unit": "byte",
        "max": 716177408,
        "free": 138606040,
        "total": 527958016
      },
      "scheduler": {
        "running": 0,
        "threadPoolSize": 10
      },
      "threads": {
        "active": 35
      }
    },
    "metrics": {
      "href": "http://madmartigan.local:4440/metrics/metrics?pretty=true",
      "contentType": "text/json"
    },
    "threadDump": {
      "href": "http://madmartigan.local:4440/metrics/threads",
      "contentType": "text/plain"
    }
  }
}
~~~~~~~~~~~~~


Description of included elements:

`timestamp` describes the current system time known to the server. The `@epoch`
attribute includes the milliseconds since the unix epoch.

`datetime`

:   The W3C date and time

`rundeck` includes information about the Rundeck application.

`rundeck/version`

:   Rundeck version

`rundeck/apiversion`

:   Rundeck API version

`rundeck/build`

:   Rundeck build stamp

`rundeck/node`

:   Server node name

`rundeck/base`

:   Server base directory

`rundeck/serverUUID`

:   Server UUID (present if cluster mode is enabled)

`os/arch`

:   Operating System architecture

`os/name`

:   Operating System Name

`os/version`

:   Operating System Version

`jvm/name`

:   JVM name

`jvm/vendor`

:   JVM vendor

`jvm/version`

:   JVM version

`stats` section includes some system statistics:

`uptime` describes the JVM uptime as duration in ms, and includes absolute
startup time:

`uptime/since`

:   JVM startup time as time since the unix epoch

`uptime/since/datetime`

:   JVM startup time as W3C date time.

`cpu/loadAverage`

:   JVM load average percentage for the system for the previous minute (see [getSystemLoadAverage](http://download.oracle.com/javase/6/docs/api/java/lang/management/OperatingSystemMXBean.html#getSystemLoadAverage()))

`cpu/processors`

:   Number of available system processors. note that loadAverage might be
    calculated based on the total number of available processors

The `memory` section describes memory usage in bytes:

`max`

:   Maximum JVM memory that can be allocated

`free`

:   Free memory of the allocated memory

`total`

:   Total allocated memory for the JVM

`scheduler/running`

:   Number of running jobs in the scheduler

`scheduler/threadPoolSize`

:   Size of the scheduler threadPool: maximum number of concurrent Rundeck executions

`threads/active`

:   Number of active Threads in the JVM

## Log Storage

### Log Storage Info

Get Log Storage information and stats.

**Request:**

    GET /api/17/system/logstorage

**Response:**

Success response, with log storage info and stats in this format:

`Content-Type: application/xml`:

~~~ {.xml}
<logStorage enabled="true" pluginName="NAME">
  <succeededCount>349</succeededCount>
  <failedCount>0</failedCount>
  <queuedCount>0</queuedCount>
  <totalCount>349</totalCount>
  <incompleteCount>0</incompleteCount>
  <missingCount>0</missingCount>
</logStorage>
~~~

`Content-Type: application/json`:

~~~ {.json}
{
  "enabled": true,
  "pluginName": "NAME",
  "succeededCount": 369,
  "failedCount": 0,
  "queuedCount": 0,
  "totalCount": 369,
  "incompleteCount": 0,
  "missingCount": 0
}
~~~

`enabled`

:   True if a plugin is configured

`pluginName`

:   Name of the configured plugin

`succeededCount`

:   Number of successful storage requests

`failedCount`

:   Number of failed storage requests

`queuedCount`

:   Number of queued storage requests

`totalCount`

:   Total number of storage requests (currently queued plus previously processed)

`incompleteCount`

:   Number of storage requests which have not completed successfully

`missingCount`

:   Number of executions for this cluster node which have no associated storage requests

### List Executions with Incomplete Log Storage

List all executions with incomplete log storage.

**Request:**

    GET /api/17/system/logstorage/incomplete

**Response:**

`Content-Type: application/xml`:

```xml
<logstorage>
  <incompleteExecutions total="[#]" max="20" offset="0">
    <execution id="[EXECID]" project="[PROJECT]" href="[API HREF]" permalink="[GUI HREF]">
      <storage incompleteFiletypes="[TYPES]" queued="true/false" failed="true/false" date="[DATE]" localFilesPresent="true/false">
        <errors>
          <message>[error message]</message>
          <message>[error message...]</message>
        </errors>
    </storage>
    </execution>
    ...
</logstorage>
```

`Content-Type: application/json`:

```json
{
  "total": #,
  "max": 20,
  "offset": 0,
  "executions": [
    {
      "id": [EXECID],
      "project": "[PROJECT]",
      "href": "[API HREF]",
      "permalink": "[GUI HREF]",
      "storage": {
        "localFilesPresent": true/false,
        "incompleteFiletypes": "[TYPES]",
        "queued": true/false,
        "failed": true/false,
        "date": "[DATE]"
      },
      "errors": ["message","message..."]
    },
    ...
    ]
}
```

`total`, `max`, `offset` (paging information)

:   Total number of executions with incomplete log data storage, maximum returned in the response, offset of first result.

`id`

:   Execution ID

`project`

:   Project Name

`href`

:   API URL for Execution

`permalink`

:   GUI URL for Execution

`incompleteFiletypes`

:   Comma-separated list of filetypes which have not be uploaded, e.g. `rdlog,state.json`. Types are `rdlog` (log output), `state.json` (workflow state data), `execution.xml` (execution definition)

`queued`

:   True if the log data storage is queued to be processed.

`failed`

:   True if the log data storage was processed but failed without completion.

`date`

:   Date when log data storage was first processed. (W3C date format.)

`localFilesPresent`

:   True if all local files (`rdlog` and `state.json`) are available for upload.  False if one of them is not proesent on disk.

### Resume Incomplete Log Storage

Resume processing incomplete Log Storage uploads.

**Request:**

    POST /api/17/system/logstorage/incomplete/resume

**Response:**

`Content-Type: application/xml`:

```xml
<logStorage resumed='true' />
```

`Content-Type: application/json`:

~~~ {.json}
{
  "resumed": true
}
~~~

## Execution Mode ##

Change the server execution mode to ACTIVE or PASSIVE.  The state of the current
execution mode can be viewed via the [`/api/14/system/info`][/api/V/system/info]
endpoint.

### Set Active Mode ###

Enables executions, allowing adhoc and manual and scheduled jobs to be run.

**Request:**

    POST /api/14/system/executions/enable

**Response**

`Content-Type: application/xml`:

~~~ {.xml}
<executions executionMode="active"/>
~~~

`Content-Type: application/json`:

~~~ {.json}
{
  "executionMode":"active"
}
~~~

### Set Passive Mode ###

Disables executions, preventing adhoc and manual and scheduled jobs from running.

**Request:**

POST /api/14/system/executions/disable

**Response**

`Content-Type: application/xml`:

~~~ {.xml}
<executions executionMode="passive"/>
~~~

`Content-Type: application/json`:

~~~ {.json}
{
  "executionMode":"passive"
}
~~~

## Cluster Mode


### Takeover Schedule in Cluster Mode

Tell a Rundeck server in cluster mode to claim all scheduled jobs from another
cluster server.

This endpoint can take over the schedule of certain jobs based on the input:

* specify a server `uuid`: take over all jobs from that server
* specify server `all` value of `true`: take over all jobs regardless of server UUID

Additionally, you can specify a `project` name to take over only jobs matching
the given project name, in combination with the server options.

Alternately, specify a job ID to takeover only a single Job's schedule.

**Request**

    PUT /api/14/scheduler/takeover

Either XML or JSON request.

`Content-Type: application/xml`:

XML Document containing:

* `<takeoverSchedule>` top level element
  * optional `<server>` element, with one of required attributes:
    * `uuid` server UUID to take over from
    * `all` value of `true` to take over from all servers
  * optional `<project>` element, required attribute: `name`
  * optional `<job`> element, with attribute:
    * `id` Job UUID to take over.

Example for a single server UUID:

~~~ {.xml}
<takeoverSchedule>
    <server uuid="[UUID]" />
</takeoverSchedule>
~~~

Example for all servers:

~~~ {.xml}
<takeoverSchedule>
    <server all="true"/>
</takeoverSchedule>
~~~

Example for all servers and a specific project:

~~~ {.xml}
<takeoverSchedule>
    <server all="true"/>
    <project name="[PROJECT]"/>
</takeoverSchedule>
~~~

Example for a single Job:

~~~ {.xml}
<takeoverSchedule>
    <job id="[UUID]"/>
</takeoverSchedule>
~~~

**Note**: The `<server>` element can be the root of the document request for backwards compatibility.

`Content-Type: application/json`:

A JSON object.

* optional `server` entry, with one of these required entries:
    * `uuid` server UUID to take over from
    * `all` value of `true` to take over from all servers
* optional `project` entry, specifying a project name
* optional `job` entry, with required entry:
    * `id` Job UUID

~~~ {.json}
{
  "server": {
    "uuid": "[UUID]",
    "all": true
  },
  "project": "[PROJECT]"
}
~~~

Specify a job id:

~~~ {.json}
{
  "job": {
    "id": "[UUID]"
  }
}
~~~

**Response:**

If request was XML, then Standard API response containing the following additional elements:

*  `takeoverSchedule`
    * `self`
        * `server`
            *  `@uuid` - this cluster server's UUID
    *  `server`
        *  `@uuid` - requested server UUID to take over, if specifed in the request
        *  `@all` - `true` if requested
    *  `project` - name of project, if specified in request
    *  `job`
        *  `@id` - requested job UUID to take over, if specifed in the request
    *  `jobs` - set of successful and failed jobs taken over
        *  `successful`/`failed` - job set
            *  `@count` number of jobs in the set
            *  `job` - one element for each job
                *  `@id` Job ID
                *  `@href` Job API HREF
                *  `@permalink` Job GUI HREF
                *  `@previous-owner` UUID of the cluster server that was the previous schedule owner

Example XML Response, when `uuid` was specified:

~~~~~~~~~~ {.xml}
<takeoverSchedule>
    <self>
      <server uuid='C677C663-F902-4B97-B8AC-4AA57B58DDD6' />
    </self>
    <server uuid='8F3D5976-2232-4529-847B-8E45764608E3' />
    <jobs total='2'>
      <successful count='2'>
        <job id='a1aa53ac-73a6-4ead-bbe4-34afbff8e057'
        href='http://localhost:9090/api/14/job/a1aa53ac-73a6-4ead-bbe4-34afbff8e057'
        permalink='http://localhost:9090/rundeck/job/show/a1aa53ac-73a6-4ead-bbe4-34afbff8e057'
        previous-owner="8F3D5976-2232-4529-847B-8E45764608E3" />
        <job id='116e2025-7895-444a-88f7-d96b4f19fdb3'
        href='http://localhost:9090/api/14/job/116e2025-7895-444a-88f7-d96b4f19fdb3'
        permalink='http://localhost:9090/rundeck/job/show/116e2025-7895-444a-88f7-d96b4f19fdb3'
        previous-owner="8F3D5976-2232-4529-847B-8E45764608E3" />
      </successful>
      <failed count='0'></failed>
    </jobs>
</takeoverSchedule>
~~~~~~~~~~

Example XML Response, when `all` was specified:

~~~~~~~~~~ {.xml}
<takeoverSchedule>
    <self>
      <server uuid='C677C663-F902-4B97-B8AC-4AA57B58DDD6' />
    </self>
    <server all='true' />
    <jobs total='2'>
      ...
    </jobs>
</takeoverSchedule>
~~~~~~~~~~

Example XML Response, when `project` was specified:

~~~~~~~~~~ {.xml}
<takeoverSchedule>
    <self>
      <server uuid='C677C663-F902-4B97-B8AC-4AA57B58DDD6' />
    </self>
    <project name='My Project' />
    <jobs total='2'>
      ...
    </jobs>
</takeoverSchedule>
~~~~~~~~~~

JSON response for `uuid` specified:

~~~~~~~~~~ {.json}
{
  "takeoverSchedule": {
    "jobs": {
      "failed": [],
      "successful": [
        {
          "href": "http://dignan:4440/api/14/job/a1aa53ac-73a6-4ead-bbe4-34afbff8e057",
          "permalink": "http://dignan:4440/job/show/a1aa53ac-73a6-4ead-bbe4-34afbff8e057",
          "id": "a1aa53ac-73a6-4ead-bbe4-34afbff8e057",
          "previous-owner": "8F3D5976-2232-4529-847B-8E45764608E3"
        },
        {
          "href": "http://dignan:4440/api/14/job/116e2025-7895-444a-88f7-d96b4f19fdb3",
          "permalink": "http://dignan:4440/job/show/116e2025-7895-444a-88f7-d96b4f19fdb3",
          "id": "116e2025-7895-444a-88f7-d96b4f19fdb3",
          "previous-owner": "8F3D5976-2232-4529-847B-8E45764608E3"
        }
      ],
      "total": 2
    },
    "server": {
      "uuid": "8F3D5976-2232-4529-847B-8E45764608E3"
    }
  },
  "self": {
    "server": {
      "uuid": "C677C663-F902-4B97-B8AC-4AA57B58DDD6"
    }
  },
  "message": "Schedule Takeover successful for 2/2 Jobs.",
  "apiversion": 14,
  "success": true
}
~~~~~~~~~~

JSON response for `all` specified:

~~~~~~~~~~ {.json}
{
  "takeoverSchedule": {
    "jobs": {
      ...
      "total": 2
    },
    "server": {
      "all": true
    }
  },
  "self": {
    "server": {
      "uuid": "C677C663-F902-4B97-B8AC-4AA57B58DDD6"
    }
  },
  "message": "Schedule Takeover successful for 2/2 Jobs.",
  "apiversion": 14,
  "success": true
}
~~~~~~~~~~

JSON response for `project` specified:

~~~~~~~~~~ {.json}
{
  "takeoverSchedule": {
    "jobs": {
      ...
      "total": 2
    },
    "project": "My Project"
  },
  "self": {
    "server": {
      "uuid": "C677C663-F902-4B97-B8AC-4AA57B58DDD6"
    }
  },
  "message": "Schedule Takeover successful for 2/2 Jobs.",
  "apiversion": 14,
  "success": true
}
~~~~~~~~~~

### List Scheduled Jobs For a Cluster Server

List the scheduled Jobs with their schedule owned by the cluster server with the specified UUID.

**Request**

    GET /api/17/scheduler/server/[UUID]/jobs

**Response**

The same format as [Listing Jobs](#listing-jobs).


### List Scheduled Jobs For this Cluster Server

List the scheduled Jobs with their schedule owned by the target cluster server.

**Request**

    GET /api/17/scheduler/jobs

**Response**

The same format as [Listing Jobs](#listing-jobs).


## ACLs

Manage the system system ACL policy files stored in the database.  

The files managed via the API **do not** include the files located on disk, however these policy files will be merged with
any policy files in the normal filesystem locations (e.g. `$RDECK_BASE/etc`).

**Note:** For Project-specific ACLs see [Project ACLs](#project-acls).

For more information about ACL Policies see:

* [ACLPOLICY format][ACLPOLICY]
* [Access Control Policy](../administration/access-control-policy.html)

### List System ACL Policies

**Request:**

    GET /api/14/system/acl/

**Response:**

`Content-Type: application/xml`:  A `<resource>` containing more resources within a `<contents>` element:

~~~~~~~~~~ {.xml}
<resource path="" type="directory" href="http://server/api/14/system/acl/">
  <contents>
    <resource path="name.aclpolicy" type="file" href="http://server/api/14/system/acl/name.aclpolicy" name="name.aclpolicy"/>
  </contents>
</resource>
~~~~~~~~~~~~

`Content-Type: application/json`

`resources` contains a list of entries for each policy

~~~~~~~~~~ {.json}
{
  "path": "",
  "type": "directory",
  "href": "http://server/api/14/system/acl/",
  "resources": [
    {
      "path": "name.aclpolicy",
      "type": "file",
      "name": "name.aclpolicy",
      "href": "http://server/api/14/system/acl/name.aclpolicy"
    },
    ...
  ]
}
~~~~~~~~~~

### Get an ACL Policy

Retrieve the YAML text of the ACL Policy file.  If YAML or text content is requested, the contents will be returned directly.
Otherwise if XML or JSON is requested, the YAML text will be wrapped within that format.

**Request:**

    GET /api/14/system/acl/name.aclpolicy

**Response:**

`Content-Type: application/yaml` or `Content-Type: text/plain`:  

~~~~~ {.yaml}
description: "my policy"
context:
  application: rundeck
for:
  project:
    - allow: read
by:
  group: build
~~~~~

`Content-Type: application/json`:  

~~~~ {.json}
{
  "contents": "description: \"my policy\"\ncontext:\n  application: rundeck\nfor:\n  project:\n    - allow: read\nby:\n  group: build"
}
~~~~

`Content-Type: application/xml`:  The content is wrapped in a `CDATA` section to preserve whitespace formatting.

~~~~ {.xml}
<contents><![CDATA[description: "my policy"
context:
  application: rundeck
for:
  project:
    - allow: read
by:
  group: build]]></contents>
~~~~


### Create an ACL Policy

Use `POST` to create a policy.

**Request:**

    POST /api/14/system/acl/name.aclpolicy
    
If the `Content-Type` is `application/yaml` or `text/plain`, then the request body is the ACL policy contents directly.

Otherwise, you can use XML or JSON in the same format as returned by [Get an ACL Policy](#get-an-acl-policy):

`Content-Type: application/json`

~~~~ {.json}
{
  "contents": "description: \"my policy\"\ncontext:\n  application: rundeck\nfor:\n  project:\n    - allow: read\nby:\n  group: build"
}

~~~~

`Content-Type: application/xml`

~~~~ {.xml}
<contents><![CDATA[description: "my policy"
context:
  application: rundeck
for:
  project:
    - allow: read
by:
  group: build]]></contents>
~~~~

**Response:**

*Successful*

    201 Created

The format the response is based on the `Accept:` header, the same format as returned by [Get an ACL Policy](#get-an-acl-policy).

*Already Exists*

    409 Conflict

*Validation Failure*

    400 Bad Request

If Validation fails, the response will be `400 Bad Request`, and the body will contain a list of validation errors.
Because each [ACLPOLICY][] document can contain multiple Yaml documents, each will be listed as a separate policy.

`Content-Type: application/json`

~~~~ {.json}
{
  "valid": false,
  "policies": [
    {
      "policy": "file1.aclpolicy[1]",
      "errors": [
        "reason...",
        "reason2..."
      ]
    },

    {
      "policy": "file1.aclpolicy[2]",
      "errors": [
        "reason...",
        "reason2..."
      ]
    }
  ]
}

~~~~


`Content-Type: application/xml`

~~~~ {.xml}
<validation valid="false">
  <policy id="file1.aclpolicy[1]">
    <error>reason text...</error>
    <error>reason2 text...</error>
  </policy>
  <policy id="file1.aclpolicy[2]">
    <error>reason text...</error>
  </policy>
</validation>
~~~~

### Update an ACL Policy

Use `PUT` to update a policy.

**Request:**

    PUT /api/14/system/acl/name.aclpolicy

You can use Yaml, XML or JSON in the same request format as used by [Create an ACL Policy](#create-an-acl-policy).

**Response:**

*Successful*

    200 OK

The same response format as used by [Create an ACL Policy](#create-an-acl-policy).

*Not Found*

    404 Not Found

If the policy does not exist, then a `404 Not Found` response is returned.

### Delete an ACL Policy

Delete an ACL policy file.

**Request:**

    DELETE /api/14/system/acl/name.aclpolicy

**Response:**

*Successful*

    204 No Content

*Not Found*

    404 Not Found

## Jobs

### Listing Jobs ###

List the jobs that exist for a project.

**Request:**

    GET  /api/14/project/[PROJECT]/jobs

(**Deprecated URL**: `/api/14/jobs` with required parameter: `project`.)

The following parameters can also be used to narrow down the result set.

* `idlist`: specify a comma-separated list of Job IDs to include
* `groupPath`: specify a group or partial group path to include all jobs within that group path. (Default value: "*", all groups). Set to the special value "-" to match the top level jobs only
* `jobFilter`: specify a filter for the job Name. Matches any job name that contains this value.
* `jobExactFilter`: specify an exact job name to match.
* `groupPathExact`: specify an exact group path to match.  Set to the special value "-" to match the top level jobs only
* `scheduledFilter`: `true/false` specify whether to return only scheduled or only not scheduled jobs.
* `serverNodeUUIDFilter`: Value: a UUID. In cluster mode, use to select scheduled jobs assigned to the server with given UUID.

Note: If neither `groupPath` nor `groupPathExact` are specified, then the default `groupPath` value of "*" will be used (matching jobs in all groups).  `groupPathExact` cannot be combined with `groupPath`.  You can set either one to "-" to match only the top-level jobs which are not within a group.

**Response**

`Content-Type: application/xml`:  An Item List of `jobs`. Each `job` is of the form:

~~~~~~~~~~ {.xml}
<job id="ID" href="[API url]" permalink="[GUI URL]" scheduled="true/false" scheduleEnabled="true/false"
   enabled="true/false"
   >
    <name>Job Name</name>
    <group>Job Name</group>
    <project>Project Name</project>
    <description>...</description>
</job>
~~~~~~~~~~~~

`Content-Type: application/json`

~~~~~~~~~~ {.json}
[
  {
    "id": "[UUID]",
    "name": "[name]",
    "group": "[group]",
    "project": "[project]",
    "description": "...",
    "href": "[API url]",
    "permalink": "[GUI url]",
    "scheduled": true/false,
    "scheduleEnabled": true/false,
    "enabled": true/false
  }
]
~~~~~~~~~~~~

**Since v17**:

* `scheduled` indicates whether the job has a schedule
* `scheduleEnabled` indicates whether the job's schedule is enabled or not
* `enabled` indicates whether executions are enabled or not

In Cluster mode, additional information about what server UUID is the schedule owner will be included:

* `serverNodeUUID` UUID of the schedule owner server for this job
* `serverOwner` boolean value whether the target server is the owner, `true/false`.

`Content-Type: application/xml`: 

~~~~~~~~~~ {.xml}
<job id="ID" href="[API url]" permalink="[GUI URL]" scheduled="true/false" scheduleEnabled="true/false" 
  enabled="true/false"
  serverNodeUUID="[UUID]" 
  serverOwner="true/false"
  >
    <name>Job Name</name>
    <group>Job Name</group>
    <project>Project Name</project>
    <description>...</description>
</job>
~~~~~~~~~~~~

`Content-Type: application/json`

~~~~~~~~~~ {.json}
[
  {
    "id": "[UUID]",
    "name": "[name]",
    "group": "[group]",
    "project": "[project]",
    "description": "...",
    "href": "[API url]",
    "permalink": "[GUI url]",
    "scheduled": true/false,
    "scheduleEnabled": true/false,
    "enabled": true/false,
    "serverNodeUUID": "[UUID]",
    "serverOwner": true/false
  }
]
~~~~~~~~~~~~


### Running a Job

Run a job specified by ID.

**Request:**

    POST /api/1/job/[ID]/run
    POST /api/12/job/[ID]/executions

Optional parameters:

* `argString`: argument string to pass to the job, of the form: `-opt value -opt2 value ...`.
* `loglevel`: argument specifying the loglevel to use, one of: 'DEBUG','VERBOSE','INFO','WARN','ERROR'
* `asUser` : specifies a username identifying the user who ran the job. Requires `runAs` permission.
* Node filter parameters as described under [Using Node Filters](#using-node-filters)
* `filter` can be a node filter string.

(**API v14**) If the request has `Content-Type: application/json`, then the parameters will be ignored,
and this format is expected in the content:

~~~~~ {.json}
{
    "argString":"...",
    "loglevel":"...",
    "asUser":"...",
    "filter":"..."
}
~~~~~


**Response**:

See [Listing Running Executions](#listing-running-executions).

### Exporting Jobs

Export the job definitions for in XML or YAML formats.

**Request:**

    GET /api/14/project/[PROJECT]/jobs/export

(**Deprecated URL**: `/api/14/jobs/export` with required parameter: `project`.)

Optional parameters:

* `format` : can be "xml" or "yaml" to specify the output format. Default is "xml"

The following parameters can also be used to narrow down the result set.

* `idlist`: specify a comma-separated list of Job IDs to export
* `groupPath`: specify a group or partial group path to include all jobs within that group path.
* `jobFilter`: specify a filter for the job Name

**Response:**

If you specify `format=xml`, then the output will be in [job-xml](../man5/job-xml.html) format.

If you specify `format=yaml`, then the output will be in [job-yaml](../man5/job-yaml.html) format.

If an error occurs, then the output will be in XML format, using the common `result` element described in the [Response Format](#response-format) section.

### Importing Jobs ###

Import job definitions in XML or YAML formats.

**Request:**

    POST /api/1/project/[PROJECT]/jobs/import

(**Deprecated URL**: `/api/14/jobs/import` with optional parameter: `project`.)

Request Content:

One of the following:


* `Content-Type: x-www-form-urlencoded`, with a `xmlBatch` request parameter containing the input content
* `Content-Type: multipart/form-data` multipart MIME request part named `xmlBatch` containing the content.
* `Content-Type: application/xml`, request body is the Jobs XML formatted job definition (**since API v14**)
* `Content-Type: application/yaml`, request body is the Jobs YAML formatted job definition (**since API v14**)

Optional parameters:

* `format` : can be "xml" or "yaml" to specify the input format, if multipart of form input is sent. Default is "xml"
* `dupeOption`: A value to indicate the behavior when importing jobs which already exist.  Value can be "skip", "create", or "update". Default is "create".
* `uuidOption`: Whether to preserve or remove UUIDs from the imported jobs. Allowed values (**since V9**):
    *  `preserve`: Preserve the UUIDs in imported jobs.  This may cause the import to fail if the UUID is already used. (Default value).
    *  `remove`: Remove the UUIDs from imported jobs. Allows update/create to succeed without conflict on UUID.

**Response:**

A set of status results.  Each imported job definition will be either "succeeded", "failed" or "skipped".  These status sections contain a `count` attribute declaring how many jobs they contain.  Within each one there will be 0 or more `job` elements.

`Content-Type: application/xml`:

~~~~~~~~~~ {.xml}
<succeeded count="x">
    <!-- job elements -->
</succeeded>
<failed count="x">
    <!-- job elements -->
</failed>
<skipped count="x">
    <!-- job elements -->
</skipped>
~~~~~~~~~~

Each Job element will be of the form:

~~~~~~~~~~ {.xml}
<job index="x" href="[API url]">
    <!-- ID, href, and permalink may not be present if the job was not created yet -->
    <id>ID</id>
    <permalink>[GUI url]</permalink>
    <name>job name</name>
    <group>job group</group>
    <project>job project</project>
    <!--if within the failed section, then an error section will be included -->
    <error>Error message</error>
</job>
~~~~~~~~~~~~~~

`Content-Type: application/json`:

~~~~~~ {.json}
{
  "succeeded": [...],
  "failed": [...],
  "skipped": [...]
}
~~~~~~

Each array may contain a job data object:

~~~~~~ {.json}
{
  "index": 1,
  "href": "http://madmartigan.local:4440/api/14/job/3b6c19f6-41ee-475f-8fd0-8f1a26f27a9a",
  "id": "3b6c19f6-41ee-475f-8fd0-8f1a26f27a9a",
  "name": "restart",
  "group": "app2/dev",
  "project": "test",
  "permalink": "http://madmartigan.local:4440/job/show/3b6c19f6-41ee-475f-8fd0-8f1a26f27a9a"
}
~~~~~~

* `index`: index in the input content of the job definition.
* `id`: If the job exists, or was successfully created, its UUID
* `href`: If the job exists, or was successfully created, its API href
* `permalink`: If the job exists, or was successfully created, its GUI URL.

### Getting a Job Definition ###

Export a single job definition in XML or YAML formats.

**Request:**

    GET /api/1/job/[ID]

Optional parameters:

* `format` : can be "xml" or "yaml" to specify the output format. Default is "xml"

**Response:**

If you specify `format=xml`, then the output will be in [job-xml](../man5/job-xml.html) format.

If you specify `format=yaml`, then the output will be in [job-yaml](../man5/job-yaml.html) format.

If an error occurs, then the output will be in XML format, using the common `result` element described in the [Response Format](#response-format) section.

### Deleting a Job Definition ###

Delete a single job definition.

**Request:**

    DELETE /api/1/job/[ID]

**Response:**

    204 No Content

### Bulk Job Delete ###

Delete multiple job definitions at once.

**Request:**

    DELETE /api/5/jobs/delete
    POST /api/5/jobs/delete

Either Query parameters:

* `ids`: The Job IDs to delete, can be specified multiple times
* `idlist`: The Job IDs to delete as a single comma-separated string.

Or JSON/XML content:

`Content-Type: application/json`

~~~~~ {.json}
{
  "ids": [
    "fefa50e1-2265-47af-b101-d4bbaa3ba21c",
    "f07e2311-4dae-40ca-bdfa-412bd223f863"
  ],
  "idlist":"49336998-21a3-42c7-8da3-a855587982e0,a387f77f-a623-45dc-967f-746a2e3f6686"
}
~~~~~

Note: you can combine `ids` with `idlist`

`application/xml` response:

The common `result` element described in the [Response Format](#response-format) section, indicating success or failure and any messages.

If successful, then the `result` will contain a `deleteJobs` element with two sections of results, `succeeded` and `failed`:

~~~~~~~~~~ {.xml}
<deleteJobs requestCount="#" allsuccessful="true/false">
    <succeeded count="1">
        <deleteJobRequest id="[job ID]">
            <message>[message]</message>
        </deleteJobRequest>
    </succeeded>
    <failed count="1">
        <deleteJobRequest id="[job ID]" errorCode="[code]">
            <error>[message]</error>
        </deleteJobRequest>
    </failed>
</deleteJobs>
~~~~~~~~~~


`deleteJobs` will have two attributes:

* `requestCount`: the number of job IDs that were in the delete request
* `allsuccessful`: true/false: true if all job deletes were successful, false otherwise.

The response may contain only one of `succeeded` or `failed`, depending on the result.

The `succeeded` and `failed` sections contain multiple `deleteJobRequest` elements.  

Each `deleteJobRequest` under the `succeeded` section will contain:

* `id` attribute - the Job ID
* `message` sub-element - result message for the delete request


Each `deleteJobRequest` under the `failed` section will contain:

* `id` attribute - the Job ID
* `error` sub-element - result error message for the delete request
* `errorCode` attribute - a code indicating the type of failure, currently one of `failed`, `unauthorized` or `notfound`.

`application/json` response:


~~~~~~ {.json}
{
  "requestCount": #integer#,
  "allsuccessful": true/false,
  "succeeded": [...],
  "failed":[...]
}
~~~~~~

The list of succeeded/failed will contain objects of this form:

~~~~~~ {.json}
{
  "id": "[UUID]",
  "errorCode": "(error code, see above)",
  "message": "(success or failure message)"
}
~~~~~~

### Enable Executions for a Job

Enable executions for a job. (ACL requires `toggle_execution` action for a job.)

**Request:**

    POST /api/14/job/[ID]/execution/enable

**Response:**

`application/xml`

~~~{.xml}
<success>true</success>
~~~

`application/json`

~~~{.json}
{"success":true}
~~~

### Disable Executions for a Job

Disable all executions for a job (scheduled or manual). (ACL requires `toggle_execution` action for a job.)

**Request:**

    POST /api/14/job/[ID]/execution/disable

**Response:**

(See [Enable Executions for a Job](#enable-executions-for-a-job).)

### Enable Scheduling for a Job

Enable the schedule for a job. (ACL requires `toggle_schedule` action for a job.)

**Request:**

    POST /api/14/job/[ID]/schedule/enable

**Response:**

(See [Enable Executions for a Job](#enable-executions-for-a-job).)

### Disable Scheduling for a Job

Disable the schedule for a job. (ACL requires `toggle_schedule` action for a job.)

**Request:**

    POST /api/14/job/[ID]/schedule/disable

**Response:**

(See [Enable Executions for a Job](#enable-executions-for-a-job).)

### Bulk Toggle Job Execution

Toggle whether executions are enabled for a set of jobs. (ACL requires `toggle_execution` action for each job.)

Executions will be enabled or disabled, depending on the URL used:

**Request:**

    POST /api/14/jobs/execution/enable
    POST /api/14/jobs/execution/disable

Query parameters:

* `ids`: The Job IDs to delete, can be specified multiple times
* `idlist`: The Job IDs to delete as a single comma-separated string.

Or JSON/XML content:

`Content-Type: application/json`

~~~~~ {.json}
{
  "ids": [
    "fefa50e1-2265-47af-b101-d4bbaa3ba21c",
    "f07e2311-4dae-40ca-bdfa-412bd223f863"
  ],
  "idlist":"49336998-21a3-42c7-8da3-a855587982e0,a387f77f-a623-45dc-967f-746a2e3f6686"
}
~~~~~

Note: you can combine `ids` with `idlist`.

**Response:**

If successful, then the `result` will contain a `toggleExecution` element with two sections of results, `succeeded` and `failed`:

~~~~~~~~~~ {.xml}
<toggleExecution enabled="true" requestCount="#" allsuccessful="true/false">
    <succeeded count="1">
        <toggleExecutionResult id="[job ID]">
            <message>[message]</message>
        </toggleExecutionResult>
    </succeeded>
    <failed count="1">
        <toggleExecutionResult id="[job ID]" errorCode="[code]">
            <error>[message]</error>
        </toggleExecutionResult>
    </failed>
</toggleExecution>
~~~~~~~~~~


`toggleExecution` has these attributes:

* `enabled`: `true` or `false`, depending on whether `enable` or `disable` was requested.
* `requestCount`: the number of job IDs that were in the request
* `allsuccessful`: true/false: true if all modifications were successful, false otherwise.

The response may contain only one of `succeeded` or `failed`, depending on the result.

The `succeeded` and `failed` sections contain multiple `toggleExecutionResult` elements.  

Each `toggleExecutionResult` under the `succeeded` section will contain:

* `id` attribute - the Job ID
* `message` sub-element - result message for the request


Each `toggleExecutionResult` under the `failed` section will contain:

* `id` attribute - the Job ID
* `error` sub-element - result error message for the request
* `errorCode` attribute - a code indicating the type of failure, currently one of `failed`, `unauthorized` or `notfound`.

`application/json` response:


~~~~~~ {.json}
{
  "requestCount": #integer#,
  "enabled": true/false,
  "allsuccessful": true/false,
  "succeeded": [...],
  "failed":[...]
}
~~~~~~

The list of succeeded/failed will contain objects of this form:

~~~~~~ {.json}
{
  "id": "[UUID]",
  "errorCode": "(error code, see above)",
  "message": "(success or failure message)"
}
~~~~~~

### Bulk Toggle Job Schedules

Toggle whether schedules are enabled for a set of jobs. (ACL requires `toggle_schedule` action for each job.)

Schedules will be enabled or disabled, depending on the URL used:

**Request:**

    POST /api/14/jobs/schedule/enable
    POST /api/14/jobs/schedule/disable

Query parameters:

* `ids`: The Job IDs to delete, can be specified multiple times
* `idlist`: The Job IDs to delete as a single comma-separated string.

Or JSON/XML content:

`Content-Type: application/json`

~~~~~ {.json}
{
  "ids": [
    "fefa50e1-2265-47af-b101-d4bbaa3ba21c",
    "f07e2311-4dae-40ca-bdfa-412bd223f863"
  ],
  "idlist":"49336998-21a3-42c7-8da3-a855587982e0,a387f77f-a623-45dc-967f-746a2e3f6686"
}
~~~~~

Note: you can combine `ids` with `idlist`.

**Response:**

If successful, then the `result` will contain a `toggleSchedule` element with two sections of results, `succeeded` and `failed`:

~~~~~~~~~~ {.xml}
<toggleSchedule enabled="true" requestCount="#" allsuccessful="true/false">
    <succeeded count="1">
        <toggleScheduleResult id="[job ID]">
            <message>[message]</message>
        </toggleScheduleResult>
    </succeeded>
    <failed count="1">
        <toggleScheduleResult id="[job ID]" errorCode="[code]">
            <error>[message]</error>
        </toggleScheduleResult>
    </failed>
</toggleSchedule>
~~~~~~~~~~


`toggleSchedule` has these attributes:

* `enabled`: `true` or `false`, depending on whether `enable` or `disable` was requested.
* `requestCount`: the number of job IDs that were in the request
* `allsuccessful`: true/false: true if all modifications were successful, false otherwise.

The response may contain only one of `succeeded` or `failed`, depending on the result.

The `succeeded` and `failed` sections contain multiple `toggleScheduleResult` elements.  

Each `toggleScheduleResult` under the `succeeded` section will contain:

* `id` attribute - the Job ID
* `message` sub-element - result message for the request


Each `toggleScheduleResult` under the `failed` section will contain:

* `id` attribute - the Job ID
* `error` sub-element - result error message for the request
* `errorCode` attribute - a code indicating the type of failure, currently one of `failed`, `unauthorized` or `notfound`.

`application/json` response:


~~~~~~ {.json}
{
  "requestCount": #integer#,
  "enabled": true/false,
  "allsuccessful": true/false,
  "succeeded": [...],
  "failed":[...]
}
~~~~~~

The list of succeeded/failed will contain objects of this form:

~~~~~~ {.json}
{
  "id": "[UUID]",
  "errorCode": "(error code, see above)",
  "message": "(success or failure message)"
}
~~~~~~

## Executions

### Getting Executions for a Job

Get the list of executions for a Job.

**Request:**

    GET /api/1/job/[ID]/executions

Optional Query Parameters:

* `status`: the status of executions you want to be returned.  Must be one of "succeeded", "failed", "aborted", or "running".  If this parameter is blank or unset, include all executions.
* Paging parameters:
    * `max`: indicate the maximum number of results to return. If unspecified, all results will be returned.
    * `offset`: indicate the 0-indexed offset for the first result to return.

**Response:**

An Item List of `executions`.  See [Listing Running Executions](#listing-running-executions).

### Delete all Executions for a Job

Delete all executions for a Job.

**Request:**

    DELETE /api/12/job/[ID]/executions

**Response:**

The same format as [Bulk Delete Executions](#bulk-delete-executions).

### Listing Running Executions

List the currently running executions for a project

**Request:**

    GET /api/14/project/[PROJECT]/executions/running

(**Deprecated URL**: `/api/14/executions/running`, required URL parameter `project`.)

Note: `PROJECT` is the project name, or '*' for all projects.

Response with `Content-Type: application/xml`: An `<executions>` element containing multiple `<execution>` elements.

~~~~ {.xml}
<executions count="[count]" offset="[offset]" max="[max]" total="[total]">
    <execution...>...</execution>
    <execution...>...</execution>
</executions>
~~~~

The `executions` element will have paging attributes:

* `max`: maximum number of results per page
* `offset`: offset from first of all results
* `total`: total number of results
* `count`: number of results in the response

Each `execution` of the form:

~~~~~~~~~~ {.xml}
<execution id="[ID]" href="[url]" permalink="[url]" status="[status]" project="[project]">
    <user>[user]</user>
    <date-started unixtime="[unixtime]">[datetime]</date-started>

    <!-- optional job context if the execution is associated with a job -->
    <job id="jobID" averageDuration="[milliseconds]" href="[API url]" permalink="[GUI url]">
        <name>..</name>
        <group>..</group>
        <description>..</description>
        <!-- optional if arguments are passed to the job since v10 -->
        <options>
            <option name="optname" value="optvalue"/>...
        </options>
    </job>

    <!-- description of the execution -->
    <description>...</description>

    <!-- argString (arguments) of the execution -->
    <argstring>...</argstring>

    <!-- if Rundeck is in cluster mode -->
    <serverUUID>...</serverUUID>

    <!-- The following elements included only if the execution has ended -->

    <!-- the completion time of the execution -->
    <date-ended unixtime="[unixtime]">[datetime]</date-ended>

    <!-- if the execution was aborted, the username who aborted it: -->
    <abortedby>[username]</abortedby>

    <!-- if the execution was is finished, a list of node names that succeeded -->
    <successfulNodes>
        <node name="node1"/>
        <node name="node2"/>
    </successfulNodes>

    <!-- if the execution was is finished, a list of node names that failed -->
    <failedNodes>
        <node name="node3"/>
        <node name="node4"/>
    </failedNodes>

</execution>
~~~~~~~~~~

**Since API v14, JSON format is available**

Response with `Content-Type: application/json`:

It contains a `paging` entry with paging information, and a `executions` array:

~~~~~~~~~~ {.json}
{
  "paging": {
    "count": 2,
    "total": 2,
    "offset": 0,
    "max": 20
  },
  "executions": [
    {
      "id": 387,
      "href": "[API url]",
      "permalink": "[GUI url]",
      "status": "[status]",
      "project": "test",
      "user": "[user]",
      "serverUUID":"[UUID]",
      "date-started": {
        "unixtime": 1431536339809,
        "date": "2015-05-13T16:58:59Z"
      },
      "date-ended": {
        "unixtime": 1431536346423,
        "date": "2015-05-13T16:59:06Z"
      },
      "job": {
        "id": "7400ff98-31c4-4834-ba3d-aee9646e867f",
        "averageDuration": 6094,
        "name": "test job",
        "group": "api-test/job-run-steps",
        "project": "test",
        "description": "",
        "href": "[API url]",
        "permalink": "[GUI url]",
        "options": {
          "opt2": "a",
          "opt1": "testvalue"
        }
      },
      "description": "echo hello there [... 5 steps]",
      "argstring": "-opt1 testvalue -opt2 a",
      "successfulNodes": [
        "madmartigan.local"
      ]
    },
    ...
  ]
}

~~~~~~~~~~

The `[status]` value indicates the execution status.  It is one of:

* `running`: execution is running
* `succeeded`: execution completed successfully
* `failed`: execution completed with failure
* `aborted`: execution was aborted

The `[url]` value for the `href` is a URL the Rundeck API for the execution.
The `[url]` value for the `permalink` is a URL to the Rundeck server page to view the execution output.

`[user]` is the username of the user who started the execution.

`[unixtime]` is the millisecond unix timestamp, and `[datetime]` is a W3C dateTime string in the format "yyyy-MM-ddTHH:mm:ssZ".

If known, the average duration of the associated Job will be indicated (in milliseconds) as `averageDuration`. (Since API v5)

**API v9 and above**: `project="[project]"` is the project name of the execution.

`successfulNodes` and `failedNodes` list the names of nodes which succeeded or failed. **API v10 and above**.

The `job` section contains `options` if an `argstring` value is set (**API v10 and above**).  Inside `options` is a sequence of `<option>` elements with two attributes:

* `name` the parsed option name
* `value` the parsed option value

**Since API v13**: The `serverUUID` will indicate the server UUID
if executed in cluster mode.

### Execution Info

Get the status for an execution by ID.

**Request:**

    GET /api/1/execution/[ID]

**Response:**

An Item List of `executions` with a single item. See [Listing Running Executions](#listing-running-executions).

With `Content-Type: application/json`, a single object:

~~~~~ {.json}
{
  "id": X,
  "href": "[url]",
  "permalink": "[url]",
  "status": "succeeded/failed/aborted/timedout/retried/other",
  "project": "[project]",
  "user": "[user]",
  "date-started": {
    "unixtime": 1431536339809,
    "date": "2015-05-13T16:58:59Z"
  },
  "date-ended": {
    "unixtime": 1431536346423,
    "date": "2015-05-13T16:59:06Z"
  },
  "job": {
    "id": "[uuid]",
    "href": "[url]",
    "permalink": "[url]",
    "averageDuration": 6094,
    "name": "[name]",
    "group": "[group]",
    "project": "[project]",
    "description": "",
    "options": {
      "opt2": "a",
      "opt1": "testvalue"
    }
  },
  "description": "echo hello there [... 5 steps]",
  "argstring": "-opt1 testvalue -opt2 a",
  "successfulNodes": [
    "nodea","nodeb"
  ],
  "failedNodes": [
    "nodec","noded"
  ]
}
~~~~~

### Delete an Execution

Delete an execution by ID.

**Request:**

    DELETE /api/12/execution/[ID]

**Response:**

`204 No Content`

*Authorization requirement*:

* Requires the `delete_execution` action allowed for a `project` in the `application` context. See: [Administration - Access Control Policy - Application Scope Resources and Actions](../administration/access-control-policy.html#application-scope-resources-and-actions)

### Bulk Delete Executions

Delete a set of Executions by their IDs.

**Request:**

    POST /api/12/executions/delete

The IDs can be specified in two ways:

1. Using a URL parameter `ids`, as a comma separated list, with no body content

        POST /api/12/executions/delete?ids=1,2,17
        Content-Length: 0

2. Using a request body of either XML or JSON data.

If using a request body, the formats are specified below:

`Content-Type: application/json`

~~~~ {.json}
{"ids": [ 1, 2, 17 ] }
~~~~

*OR* more simply:

~~~~ {.json}
[ 1, 2, 17 ]
~~~~

`Content-Type: application/xml`

~~~~ {.xml}
<executions>
    <execution id="1"/>
    <execution id="2"/>
    <execution id="17"/>
</executions>
~~~~

Response:

The response format will be either `xml` or `json`, depending on the `Accept` header.

`Content-Type: application/json`

~~~~ {.json}
{
  "failures": [
    {
      "id": "82",
      "message": "Not found: 82"
    },
    {
      "id": "83",
      "message": "Not found: 83"
    },
    {
      "id": "84",
      "message": "Not found: 84"
    }
  ],
  "failedCount": 3,
  "successCount": 2,
  "allsuccessful": false,
  "requestCount": 5
}
~~~~

The JSON fields will be:

* `failures`: a list of objects indicating the `id` and `message` for the failed deletion attempt
* `failedCount`: number of deletion attempts that failed
* `successCount`: number of deletion attempts that succeeded
* `allsuccessful`: true if all deletions were successful
* `requestCount`: number of requested execution deletions

`Content-Type: application/xml`

~~~~ {.xml}
<deleteExecutions requestCount='4' allsuccessful='false'>
  <successful count='0' />
  <failed count='4'>
    <execution id='131' message='Unauthorized: Delete execution 131' />
    <execution id='109' message='Not found: 109' />
    <execution id='81' message='Not found: 81' />
    <execution id='74' message='Not found: 74' />
  </failed>
</deleteExecutions>
~~~~

### Execution Query

Query for Executions based on Job or Execution details.

**Request:**

    GET /api/14/project/[PROJECT]/executions

(**Deprecated URL**: `/api/14/executions`, required parameter `project`.)

The following parameters can also be used to narrow down the result set.

* `statusFilter`: execution status, one of "running", succeeded", "failed" or "aborted"
* `abortedbyFilter`: Username who aborted an execution
* `userFilter`: Username who started the execution
* Date query parameters:
    * `recentFilter`: Use a simple text format to filter executions that completed within a period of time. The format is "XY" where X is an integer, and "Y" is one of:
        * `h`: hour
        * `d`: day
        * `w`: week
        * `m`: month
        * `y`: year

        So a value of `2w` would return executions that completed within the last two weeks.
    * `olderFilter`: (same format as `recentFilter`) return executions that completed before the specified relative period of time.  E.g. a value of `30d` returns executions older than 30 days.
    * `begin`: Specify exact date for earliest execution completion time
    * `end`: Specify exact date for latest execution completion time
* `adhoc`: "true/false", if true, include only Adhoc executions, if false return only Job executions. By default any matching executions are returned, however if you use any of the Job filters below, then only Job executions will be returned.

The format for the `end`, and `begin` filters is either:  a unix millisecond timestamp, or a W3C dateTime string in the format "yyyy-MM-ddTHH:mm:ssZ".

Parameters for querying for Executions for particular jobs:

* `jobIdListFilter`: specify a Job ID to include, can be specified multiple times
* `excludeJobIdListFilter`: specify a Job ID to exclude, can be specified multiple times
* `jobListFilter`: specify a full Job group/name to include, can be specified multiple times
* `excludeJobListFilter`: specify a full Job group/name to exclude, can be specified multiple times
* `groupPath`: specify a group or partial group path to include all jobs within that group path. Set to the special value "-" to match the top level jobs only.
* `groupPathExact`: specify an exact group path to match.  Set to the special value "-" to match the top level jobs only.
* `excludeGroupPath`: specify a group or partial group path to exclude all jobs within that group path. Set to the special value "-" to match the top level jobs only.
* `excludeGroupPathExact`: specify an exact group path to exclude.  Set to the special value "-" to match the top level jobs only.
* `jobFilter`: specify a filter for the job Name. Include any job name that matches this value.
* `excludeJobFilter`: specify a filter for the job Name. Exclude any job name that matches this value.
* `jobExactFilter`: specify an exact job name to match.
* `excludeJobExactFilter`: specify an exact job name to exclude.

The format for the `jobListFilter` and `excludeJobListFilter` is the job's group and name separated by a '/' character, such as: "group1/job name", or "my job" if there is no group.

Paging parameters:

* `max`: maximum number of results to include in response. (default: 20)
* `offset`: offset for first result to include. (default: 0)

**Response**

See [Listing Running Executions](#listing-running-executions).

### Execution State

Get detail about the node and step state of an execution by ID. The execution can be currently running or completed.

**Request:**

    GET /api/10/execution/[ID]/state

Specify expected output format with the `Accept: ` HTTP header. Supported formats:

* `text/xml`
* `application/json`

The content of the response contains state information for different parts of the workflow:

* overall state
* per-node overall state
* per-step node state

A workflow can have a step which consists of a sub-workflow, so each particular step has a "Step Context Identifier" which defines its location in the workflow(s), and looks something like "1/5/2". Each number identifies the step number (starting at 1) at a workflow level. If there is a "/" in the context identifier, it means there are sub-workflow step numbers, and each preceding number corresponds to a step which has a sub-workflow.

To identify the state of a particular node at a particular step, both a Node name, and a Step Context Identifier are necessary.

In the result set returned by this API call, state information is organized primarily by Step and is structured in the same way as the workflow.  This means that sub-workflows will have nested state structures for their steps.

The state information for a Node will not contain the full set of details for the Step and Node, since this information is present in the workflow structure which contains the step state.

#### State Result Content

The result set contains this top-level structure:

* general overal state information
    - `startTime` execution start time (see *Timestamp format* below)
    - `endTime` execution end time if complete
    - `updateTime` last update time
    - `executionState` overall execution state
* `allNodes` contains a *Node Name List* (see below) of nodes known to be targetted in some workflow
* `nodes` contains an *Overall Node State List* of per-node step states
* `serverNode` name of the server node
* `executionId` current execution ID
* `completed` true/false whether the execution is completed
* A *Workflow Section* (see below)

**Workflow Section**

Each Workflow Section within the result set will contain these structures

* `stepCount` Number of steps in the workflow
* `targetNodes` contains a Node Name List identifying the target nodes of the current workflow
* `steps` contains a *Step State List* (see below) of information and state for each step

**Node Name List**

Consists of a sequence of node name entries, identifying each entry by a name.

In XML, a sequence of `node` elements:

      <node name="abc" />
      <node name="xyz" />
      <!-- ... more node elements -->

In JSON, an array of node names.

**Overall Node State List**

Consists of a sequence of entries for each Node. Each entry contains

* `name` node name
* `steps` list of simple state indicator for steps executed by this node

State Indicators:

* `stepctx` Step Context Identifier
* `executionState` execution state for this step and node

In XML:

~~~~~~~~~~ {.xml}
<node name="abc">
  <steps>
    <step>
      <stepctx>1</stepctx>
      <executionState>SUCCEEDED</executionState>
    </step>
    <step>
      <stepctx>2/1</stepctx>
      <executionState>SUCCEEDED</executionState>
    </step>
  </steps>
</node>
<!-- more node elements -->
~~~~~~~~~~

In JSON: an object where each key is a node name, and the value is an array of State indicators.  A state indicator is an object with two keys, `stepctx` and `executionState`

~~~~~~~~~~ {.json}
{
    "abc": [
      {
        "executionState": "SUCCEEDED",
        "stepctx": "1"
      },
      {
        "executionState": "SUCCEEDED",
        "stepctx": "2/1"
      }
    ]
}
~~~~~~~~~~

**Step State List**

A list of Step State information.  Each step is identified by its number in the workflow (starting at 1) and its step context

* `num` the step number (XML)
* `id` the step number (JSON)
* `stepctx` the step context identifier in the workflow
* general overall state information for the step
    - `startTime` execution start time
    - `endTime` execution end time if complete
    - `updateTime` last update time
    - `executionState` overall execution state
* `nodeStep` true/false. true if this step directly targets each node from the targetNodes list.  If true, this means the step will contain a `nodeStates` section
* `nodeStates` a *Node Step State Detail List* (see below) for the target nodes if this is a node step.
* `hasSubworkflow` true/false. true if this step has a sub-workflow and a `workflow` entry
* `workflow` this section contains a Workflow Section

**Node Step State Detail List**

A sequence of state details for a set of Nodes for the containing step. Each entry will contain:

* `name` the node name
* state information for the Node
    - `startTime` execution start time
    - `endTime` execution end time if complete
    - `updateTime` last update time
    - `executionState` overall execution state

In XML:

~~~~~~~~~~ {.xml}
<nodeState name="abc">
  <startTime>2014-01-13T20:58:59Z</startTime>
  <updateTime>2014-01-13T20:59:04Z</updateTime>
  <endTime>2014-01-13T20:59:04Z</endTime>
  <executionState>SUCCEEDED</executionState>
</nodeState>
<!-- more nodeState elements -->
~~~~~~~~~~

In JSON: an object with node names as keys.  Values are objects containing the state information entries.

~~~~~~~~~~ {.json}
{
    "abc": {
      "executionState": "SUCCEEDED",
      "endTime": "2014-01-13T20:38:31Z",
      "updateTime": "2014-01-13T20:38:31Z",
      "startTime": "2014-01-13T20:38:25Z"
    }
}
~~~~~~~~~~

**Full XML Example**

Within the `<result>` element:

~~~~~~~~~~ {.xml}
<executionState id="135">
  <startTime>2014-01-13T20:58:59Z</startTime>
  <updateTime>2014-01-13T20:59:10Z</updateTime>
  <stepCount>2</stepCount>
  <allNodes>
    <nodes>
      <node name="dignan" />
    </nodes>
  </allNodes>
  <targetNodes>
    <nodes>
      <node name="dignan" />
    </nodes>
  </targetNodes>
  <executionId>135</executionId>
  <serverNode>dignan</serverNode>
  <endTime>2014-01-13T20:59:10Z</endTime>
  <executionState>SUCCEEDED</executionState>
  <completed>true</completed>
  <steps>
    <step stepctx="1" id="1">
      <startTime>2014-01-13T20:58:59Z</startTime>
      <nodeStep>true</nodeStep>
      <updateTime>2014-01-13T20:58:59Z</updateTime>
      <endTime>2014-01-13T20:59:04Z</endTime>
      <executionState>SUCCEEDED</executionState>
      <nodeStates>
        <nodeState name="dignan">
          <startTime>2014-01-13T20:58:59Z</startTime>
          <updateTime>2014-01-13T20:59:04Z</updateTime>
          <endTime>2014-01-13T20:59:04Z</endTime>
          <executionState>SUCCEEDED</executionState>
        </nodeState>
      </nodeStates>
    </step>
    <step stepctx="2" id="2">
      <startTime>2014-01-13T20:59:04Z</startTime>
      <nodeStep>false</nodeStep>
      <updateTime>2014-01-13T20:59:10Z</updateTime>
      <hasSubworkflow>true</hasSubworkflow>
      <endTime>2014-01-13T20:59:10Z</endTime>
      <executionState>SUCCEEDED</executionState>
      <workflow>
        <startTime>2014-01-13T20:59:04Z</startTime>
        <updateTime>2014-01-13T20:59:10Z</updateTime>
        <stepCount>1</stepCount>
        <allNodes>
          <nodes>
            <node name="dignan" />
          </nodes>
        </allNodes>
        <targetNodes>
          <nodes>
            <node name="dignan" />
          </nodes>
        </targetNodes>
        <endTime>2014-01-13T20:59:10Z</endTime>
        <executionState>SUCCEEDED</executionState>
        <completed>true</completed>
        <steps>
          <step stepctx="2/1" id="1">
            <startTime>2014-01-13T20:59:04Z</startTime>
            <nodeStep>true</nodeStep>
            <updateTime>2014-01-13T20:59:04Z</updateTime>
            <endTime>2014-01-13T20:59:10Z</endTime>
            <executionState>SUCCEEDED</executionState>
            <nodeStates>
              <nodeState name="dignan">
                <startTime>2014-01-13T20:59:04Z</startTime>
                <updateTime>2014-01-13T20:59:10Z</updateTime>
                <endTime>2014-01-13T20:59:10Z</endTime>
                <executionState>SUCCEEDED</executionState>
              </nodeState>
            </nodeStates>
          </step>
        </steps>
      </workflow>
    </step>
  </steps>
  <nodes>
    <node name="dignan">
      <steps>
        <step>
          <stepctx>1</stepctx>
          <executionState>SUCCEEDED</executionState>
        </step>
        <step>
          <stepctx>2/1</stepctx>
          <executionState>SUCCEEDED</executionState>
        </step>
      </steps>
    </node>
  </nodes>
</executionState>
~~~~~~~~~~

**Full JSON example**

~~~~~~~~~~ {.json}
{
  "completed": true,
  "executionState": "SUCCEEDED",
  "endTime": "2014-01-13T20:38:36Z",
  "serverNode": "dignan",
  "startTime": "2014-01-13T20:38:25Z",
  "updateTime": "2014-01-13T20:38:36Z",
  "stepCount": 2,
  "allNodes": [
    "dignan"
  ],
  "targetNodes": [
    "dignan"
  ],
  "nodes": {
    "dignan": [
      {
        "executionState": "SUCCEEDED",
        "stepctx": "1"
      },
      {
        "executionState": "SUCCEEDED",
        "stepctx": "2/1"
      }
    ]
  },
  "executionId": 134,
  "steps": [
    {
      "executionState": "SUCCEEDED",
      "endTime": "2014-01-13T20:38:31Z",
      "nodeStates": {
        "dignan": {
          "executionState": "SUCCEEDED",
          "endTime": "2014-01-13T20:38:31Z",
          "updateTime": "2014-01-13T20:38:31Z",
          "startTime": "2014-01-13T20:38:25Z"
        }
      },
      "updateTime": "2014-01-13T20:38:25Z",
      "nodeStep": true,
      "id": "1",
      "startTime": "2014-01-13T20:38:25Z"
    },
    {
      "workflow": {
        "completed": true,
        "startTime": "2014-01-13T20:38:31Z",
        "updateTime": "2014-01-13T20:38:36Z",
        "stepCount": 1,
        "allNodes": [
          "dignan"
        ],
        "targetNodes": [
          "dignan"
        ],
        "steps": [
          {
            "executionState": "SUCCEEDED",
            "endTime": "2014-01-13T20:38:36Z",
            "nodeStates": {
              "dignan": {
                "executionState": "SUCCEEDED",
                "endTime": "2014-01-13T20:38:36Z",
                "updateTime": "2014-01-13T20:38:36Z",
                "startTime": "2014-01-13T20:38:31Z"
              }
            },
            "updateTime": "2014-01-13T20:38:31Z",
            "nodeStep": true,
            "id": "1",
            "startTime": "2014-01-13T20:38:31Z"
          }
        ],
        "endTime": "2014-01-13T20:38:36Z",
        "executionState": "SUCCEEDED"
      },
      "executionState": "SUCCEEDED",
      "endTime": "2014-01-13T20:38:36Z",
      "hasSubworkflow": true,
      "updateTime": "2014-01-13T20:38:36Z",
      "nodeStep": false,
      "id": "2",
      "startTime": "2014-01-13T20:38:31Z"
    }
  ]
}
~~~~~~~~~~

**Timestamp format:**

The timestamp format is ISO8601: `yyyy-MM-dd'T'HH:mm:ss'Z'`

**Execution states:**

* `WAITING` - Waiting to start running
* `RUNNING` - Currently running
* `RUNNING_HANDLER` - Running error handler\*
* `SUCCEEDED` - Finished running successfully
* `FAILED` - Finished with a failure
* `ABORTED` - Execution was aborted
* `NODE_PARTIAL_SUCCEEDED` - Partial success for some nodes\*
* `NODE_MIXED` - Mixed states among nodes\*
* `NOT_STARTED` - After waiting the execution did not start\*

\* these states only apply to steps/nodes and do not apply to the overall execution or workflow.

### Execution Output

Get the output for an execution by ID.  The execution can be currently running or may have already completed. Output can be filtered down to a specific node or workflow step.

**Request:**

    GET /api/5/execution/[ID]/output
    GET /api/10/execution/[ID]/output/node/[NODE]
    GET /api/10/execution/[ID]/output/node/[NODE]/step/[STEPCTX]
    GET /api/10/execution/[ID]/output/step/[STEPCTX]

The log output for each execution is stored in a file on the Rundeck server, and this API endpoint allows you to retrieve some or all of the output, in several possible formats: json, XML, and plain text.  When retrieving the plain text output, some metadata about the log is included in HTTP Headers.  JSON and XML output formats include metadata about each output log line, as well as metadata about the state of the execution and log file, and your current index location in the file.

Output can be selected by Node or Step Context or both as of API v10.

Several parameters can be used to retrieve only part of the output log data.  You can use these parameters to more efficiently retrieve the log content over time while an execution is running.

The log file used to store the execution output is a formatted text file which also contains metadata about each line of log output emitted during an execution.  Several data values in this API endpoint refer to "bytes", but these do not reflect the size of the final log data; they are only relative to the formatted log file itself.  You can treat these byte values as opaque locations in the log file, but you should not try to correlate them to the actual textual log lines.

Optional Parameters:

* `offset`: byte offset to read from in the file. 0 indicates the beginning.
* `lastlines`: number of lines to retrieve from the end of the available output. If specified it will override the `offset` value and return only the specified number of lines at the end of the log.
* `lastmod`: epoch datestamp in milliseconds, return results only if modification changed since the specified date OR if more data is available at the given `offset`
* `maxlines`: maximum number of lines to retrieve forward from the specified offset.

**Response:**

The output content in the requested format, see [Output Content](#output-content).

#### Tailing Output

To "tail" the output from a running execution, you will need to make a series of requests to this API endpoint, and update the `offset` value that you send to reflect the returned `dataoffset` value that you receive.  This gives you a consistent pointer into the output log file.

When starting these requests, there are two mechanisms you can use:

1. Start at the beginning, specifying either a `lastmod` or a `offset` of 0
2. Start at the end, by using `lastlines` to receive the last available set of log lines.

After your first request you will have the `dataoffset` and `lastmod` response values you can use to continue making requests for subsequent log output. You can choose several ways to do this:

1. Use the `offset` and `lastmod` parameters to indicate modification time and receive as much output as is available
2. Use the `offset` and `maxlines` parameter to specify a maximum number of log entries
3. Use only the `offset` parameter and receive as much output as is available.

After each request, you will update your `offset` value to reflect the `dataoffset` in the response.

All log output has been read when the `iscompleted` value is "true".

Below is some example pseudo-code for using this API endpoint to follow the output of a running execution "live":

* set offset to 0
* set lastmod to 0
* Repeat until `iscompleted` response value is "true":
    * perform request sending `offset` and `lastmod` parameters
    * print any log entries, update progress bar, etc.
    * Record the resulting `dataoffset` and `lastmod` response values for the next request
    * if `unmodified` is "true", sleep for 5 seconds
    * otherwise sleep for 2 seconds

**Authorization:**

This endpoint requires that the user have 'read' access to the Job or to Adhoc executions to retrieve the output content.

#### Output Format Using the URL

Specifying an output format can occur in several ways.  The simplest ways are to include the format in the URL, either by including a `format` URL parameter, or an extension on the request URL.

When using a URL format, use one of these values for the format:

* `json`
* `xml`
* `text`

To use a URL parameter, add a `?format=` parameter to your request.

E.g.:

    GET /api/5/execution/3/output?format=json

To use a URL extension, add a ".[format]" to the end of the URL, but prior to any URL parameters.

E.g.:

    GET /api/5/execution/3/output.xml?offset=120

#### Output Format using Accept Header

You can also specify the format using Content Negotiation techniques by including an `Accept` header in your request, and specifying a valid MIME-type to represent one of the formats:

* For XML, `text/xml` or `application/xml`
* For JSON, `application/json` or `text/json`
* For plain text, `text/plain`

E.g.:

    GET /api/5/execution/3/output
    Accept: */xml

#### Output Content

The result will contain a set of data values reflecting the execution's status, as well as the status and read location in the output file.

* In JSON, there will be an object containing these entries.
* In XML, within the standard [Response Format](#response-format) `result` there will be an `output` element, containing these sub-elements, each with a text value.

Entries:

* `id`: ID of the execution
* `message`: optional text message indicating why no entries were returned
* `error`: optional text message indicating an error case
* `unmodified`: true/false, (optional) "true" will be returned if the `lastmod` parameter was used and the file had not changed
* `empty`: true/false, (optional) "true" will be returned if the log file does not exist or is empty, which may occur if the log data is requested before any output has been stored.
* `offset`: Byte offset to read for the next set of data
* `completed`: true/false, "true" if the current log entries or request parameters include all of the available data
* `execCompleted`: true/false, "true" if the execution has completed.
* `hasFailedNodes`: true/false, "true" if the execution has recorded a list of failed nodes
* `execState`: execution state, one of "running","succeeded","failed","aborted"
* `lastModified`: (long integer), millisecond timestamp of the last modification of the log file
* `execDuration`: (long integer), millisecond duration of the execution
* `percentLoaded`: (float), percentage of the output which has been loaded by the parameters to this request
* `totalSize`: (integer), total bytes available in the output file
* `filter` - if a `node` or `step` filter was used
    - `nodename` - value of the node name filter
    - `stepctx` - value of the step context filter

Each log entry will be included in a section called `entries`.

* In JSON, `entries` will contain an array of Objects, each containing the following format
* In XML, the `entries` element will contain a sequence of `entry` elements

Content of each Log Entry:

* `time`: Timestamp in format: "HH:MM:SS"
* `level`: Log level, one of: SEVERE,WARNING,INFO,CONFIG,FINEST
* `log`: The log message
* `user`: User name
* `command`: Workflow command context string
* `node`: Node name

**Note for API version 5:**

For API requests using version `5` only, the XML `entry` will have the log message as the text value. Otherwise the log entry
value will be within the `log` attribute.

#### Text Format Content

For the plain text format, the content of the response will simply be the log output lines at the chosen offset location.

Included in the response will be some HTTP headers that provide the metadata about the output location. Some headers may not be present, depending on the state of the response. See the [Output Content](#output-content) section for descriptions of the content and availability of the values:

* `X-Rundeck-ExecOutput-Error`: The `error` field
* `X-Rundeck-ExecOutput-Message`: The `message` field
* `X-Rundeck-ExecOutput-Empty`: The `empty` field
* `X-Rundeck-ExecOutput-Unmodified`: The `unmodified` field
* `X-Rundeck-ExecOutput-Offset`: The `offset` field
* `X-Rundeck-ExecOutput-Completed`: The `completed` field
* `X-Rundeck-Exec-Completed`: The `execCompleted` field
* `X-Rundeck-Exec-State`: The `execState` field
* `X-Rundeck-Exec-Duration`: the `execDuration` field
* `X-Rundeck-ExecOutput-LastModifed`: The `lastModified` field
* `X-Rundeck-ExecOutput-TotalSize`: The `totalSize` field

### Execution Output with State

Get the metadata associated with workflow step state changes along with the log output, optionally excluding log output.

**Request:**

    GET /api/10/execution/[ID]/output/state
    GET /api/10/execution/[ID]/output/state?stateOnly=true

This API endpoint provides the sequential log of state changes for steps and nodes, optionally interleaved with the actual log output.

**Response:**

The output format is the same as [Execution Output](#execution-output), with this change:

* in the `entries` section, each entry will have a `type` value indicating the entry type
    - `log` a normal log entry
    - `stepbegin` beginning of the step indicated by the `stepctx`
    - `stepend` finishing of the step
    - `nodebegin` beginning of execution of a node for the given step
    - `nodeend` finishing of execution of a node for the given step
* metadata about the entry may be included in the entry

### Aborting Executions

Abort a running execution by ID.

**Request:**

    GET /api/1/execution/[ID]/abort

Optional Parameters:

* `asUser` : specifies a username identifying the user who aborted the execution. Requires `runAs` permission.

**Response:**

`Content-Type: application/xml`: The result will contain a `success/message` element will contain a descriptive message.  The status of the abort action will be included as an element:

~~~~~~~~~~ {.xml}
<abort status="[abort-state]">
    <execution id="[id]" status="[status]"/>
</abort>
~~~~~~~~~~

`Content-Type: application/json`:

~~~~~~ {.json}
{
  "abort": {
    "status": "[abort-state]",
    "reason": "[reason]"
  },
  "execution": {
    "id": "[id]",
    "status": "[execution status]",
    "href": "[API href]",
  }
}
~~~~~~

The `[abort-state]` will be one of: "pending", "failed", or "aborted".

If the `[abort-state]` is "failed", then `[reason]` will be a textual description of the reason.

## Adhoc

### Running Adhoc Commands

Run a command string.

**Request:**

    GET /api/14/project/[PROJECT]/run/command
    POST /api/14/project/[PROJECT]run/command

(**Deprecated URLs**: `/api/14/run/command`, with required parameter `project`).

The necessary content can be supplied as request Parameters:

* `exec`: the shell command string to run, e.g. "echo hello". (required)
* `nodeThreadcount`: threadcount to use (optional)
* `nodeKeepgoing`: if "true", continue executing on other nodes even if some fail. (optional)
* `asUser` : specifies a username identifying the user who ran the command. Requires `runAs` permission. (optional)

Node filter parameters as described under [Using Node Filters](#using-node-filters)

Or the request can be `Content-type: application/json`:

~~~~~~ {.json}
{
    "project":"[project]",
    "exec":"[exec]",
    "nodeThreadcount": #threadcount#,
    "nodeKeepgoing": true/false,
    "asUser": "[asUser]",
    "filter": "[node filter string]"
}
~~~~~~

**Response:**

`Content-Type: application/xml`: A success message, and a single `<execution>` item identifying the
new execution by ID:

~~~~~~~~~~ {.xml}
<execution id="X" href="[API Href]" permalink="[GUI href]"/>
~~~~~~~~~~

`Content-Type: application/json`:

~~~~~~~~~~ {.json}
{
  "message": "Immediate execution scheduled (X)",
  "execution": {
    "id": X,
    "href": "[API Href]",
    "permalink": "[GUI Href]"
  }
}
~~~~~~~~~~

### Running Adhoc Scripts

Run a script.

**Request:**

    POST /api/14/project/[PROJECT]/run/script

(**Deprecated URL**: `/api/14/run/script`, with required parameter `project`).

Request Content:

The script file content can be submitted either as a form request or multipart attachment with request parameters, or can be a json document.

For Content-Type: `application/x-www-form-urlencoded`

* `scriptFile`: A `x-www-form-urlencoded` request parameter containing the script file content.

For Content-Type: `multipart/form-data`

* `scriptFile`: the script file contents (`scriptFile` being the `name` attribute of the `Content-Disposition` header)

Parameters:

* `argString`: Arguments to pass to the script when executed.
* `nodeThreadcount`: threadcount to use
* `nodeKeepgoing`: if "true", continue executing on other nodes even if some fail.
* `asUser` : specifies a username identifying the user who ran the script. Requires `runAs` permission.
* `scriptInterpreter`: a command to use to run the script (*since version 8*)
* `interpreterArgsQuoted`: `true`/`false`: if true, the script file and arguments will be quoted as the last argument to the `scriptInterpreter` (*since version 8*)
* `fileExtension`: extension of of the script file on the remote node (*since version 14*)

Node filter parameters as described under [Using Node Filters](#using-node-filters)

If using a json document with Content-type: `application/json`:

~~~~~~ {.json}
{
    "project":"[project]",
    "script":"[script]",
    "nodeThreadcount": #threadcount#,
    "nodeKeepgoing": true/false,
    "asUser": "[asUser]",
    "argString": "[argString]",
    "scriptInterpreter": "[scriptInterpreter]",
    "interpreterArgsQuoted": true/false,
    "fileExtension": "[fileExtension]",
    "filter": "[node filter string]"
}
~~~~~~

#### Response

`Content-Type: application/xml`: A success message, and a single `<execution>` item identifying the
new execution by ID:

~~~~~~~~~~ {.xml}
<execution id="X" href="[API Href]" permalink="[GUI href]"/>
~~~~~~~~~~

`Content-Type: application/json`:

~~~~~~~~~~ {.json}
{
  "message": "Immediate execution scheduled (X)",
  "execution": {
    "id": X,
    "href": "[API Href]",
    "permalink": "[GUI Href]"
  }
}
~~~~~~~~~~

### Running Adhoc Script URLs

Run a script downloaded from a URL.  (**API version 4** required.)

**Request:**

    POST /api/14/project/[PROJECT]/run/url
    GET /api/14/project/[PROJECT]/run/url

(**Deprecated URL**: `/api/14/run/url`, with required parameter `project`).

The request can be form content, or a JSON document.

With Content-Type: `application/x-www-form-urlencoded` form or query parameters are used.

* `scriptURL`: A URL pointing to a script file (required)
* `argString`: Arguments to pass to the script when executed.
* `nodeThreadcount`: threadcount to use
* `nodeKeepgoing`: if "true", continue executing on other nodes even if some fail.
* `asUser` : specifies a username identifying the user who ran the script. Requires `runAs` permission.
* `scriptInterpreter`: a command to use to run the script (*since version 8*)
* `interpreterArgsQuoted`: `true`/`false`: if true, the script file and arguments will be quoted as the last argument to the `scriptInterpreter` (*since version 8*)
* `fileExtension`: extension of of the script file on the remote node (*since version 14*)

Node filter parameters as described under [Using Node Filters](#using-node-filters)


If using a json document with Content-type: `application/json`:

~~~~~~ {.json}
{
    "project":"[project]",
    "url":"[scriptURL]",
    "nodeThreadcount": #threadcount#,
    "nodeKeepgoing": true/false,
    "asUser": "[asUser]",
    "argString": "[argString]",
    "scriptInterpreter": "[scriptInterpreter]",
    "interpreterArgsQuoted": true/false,
    "fileExtension": "[fileExtension]",
    "filter": "[node filter string]"
}
~~~~~~

**Response:**

A success message, and a single `<execution>` item identifying the
new execution by ID:

~~~~~~~~~~ {.xml}
<execution id="X" href="[API Href]" permalink="[GUI href]"/>
~~~~~~~~~~

**Since API version 8**: The script interpreter and whether the arguments to the interpreter are quoted can be specified.


`Content-Type: application/json`:

~~~~~~~~~~ {.json}
{
  "message": "Immediate execution scheduled (X)",
  "execution": {
    "id": X,
    "href": "[API Href]",
    "permalink": "[GUI Href]"
  }
}
~~~~~~~~~~

## Key Storage ###

Upload and manage public and private key files and passwords.
For more information see the [Administration - Key Storage](../administration/key-storage.html) document.

Keys are stored via Rundeck's *Storage* facility.  This is a path-based interface to manage files.  The underlying storage may be on disk or in a database.

The Storage facility manages "resources", which may be files or directories.  File resources can have metadata associated with them (such as MIME content type).

Note: Private Keys and Passwords can be uploaded but not retrieved directly with this API.  They can only be used internally by Rundeck.

URL:

    /api/11/storage/keys/[PATH]/[FILE]

### Upload Keys ####

Specify the type of key via the `Content-type` header:

* `application/octet-stream` specifies a **private key**
* `application/pgp-keys` specifies a **public key**
* `application/x-rundeck-data-password` specifies a **password**

Use `POST` to create a new file, or `PUT` to modify an existing file.

~~~
POST /api/11/storage/keys/[PATH]/[FILE]
Content-Type: [...]
~~~

~~~
PUT /api/11/storage/keys/[PATH]/[FILE]
Content-Type: [...]
~~~

### List keys ####

Lists resources at the specified PATH, provides a JSON or XML response based on the `Accept` request header.

Each resource has a type of `file` or `directory`.

    GET /api/11/storage/keys/[PATH]/

Response:

`application/xml`

~~~~ {.xml}
<resource path='keys' type='directory'
url='http://dignan.local:4440/api/11/storage/keys'>
  <contents count='3'>
    <resource path='keys/test1.pem' type='file'
    url='http://dignan.local:4440/api/11/storage/keys/test1.pem'
    name='test1.pem'>
      <resource-meta>
        <Rundeck-content-type>
        application/octet-stream</Rundeck-content-type>
        <Rundeck-content-size>1679</Rundeck-content-size>
        <Rundeck-content-mask>content</Rundeck-content-mask>
        <Rundeck-key-type>private</Rundeck-key-type>
      </resource-meta>
    </resource>
    <resource path='keys/test1.pub' type='file'
    url='http://dignan.local:4440/api/11/storage/keys/test1.pub'
    name='test1.pub'>
      <resource-meta>
        <Rundeck-content-type>
        application/pgp-keys</Rundeck-content-type>
        <Rundeck-content-size>393</Rundeck-content-size>
        <Rundeck-key-type>public</Rundeck-key-type>
      </resource-meta>
    </resource>
    <resource path='keys/monkey1.pub' type='file'
    url='http://dignan.local:4440/api/11/storage/keys/monkey1.pub'
    name='monkey1.pub'>
      <resource-meta>
        <Rundeck-content-type>
        application/pgp-keys</Rundeck-content-type>
        <Rundeck-content-size>640198</Rundeck-content-size>
        <Rundeck-key-type>public</Rundeck-key-type>
      </resource-meta>
    </resource>
    <resource path='keys/subdir' type='directory'
    url='http://dignan.local:4440/api/11/storage/keys/subdir'>
    </resource>
  </contents>
</resource>
~~~~

`application/json`

~~~~ {.json}
{
  "resources": [
    {
      "meta": {
        "Rundeck-key-type": "private",
        "Rundeck-content-mask": "content",
        "Rundeck-content-size": "1679",
        "Rundeck-content-type": "application/octet-stream"
      },
      "url": "http://dignan.local:4440/api/11/storage/keys/test1.pem",
      "name": "test1.pem",
      "type": "file",
      "path": "keys/test1.pem"
    },
    {
      "url": "http://dignan.local:4440/api/11/storage/keys/subdir",
      "type": "directory",
      "path": "keys/subdir"
    },
    {
      "meta": {
        "Rundeck-key-type": "public",
        "Rundeck-content-size": "640198",
        "Rundeck-content-type": "application/pgp-keys"
      },
      "url": "http://dignan.local:4440/api/11/storage/keys/monkey1.pub",
      "name": "monkey1.pub",
      "type": "file",
      "path": "keys/monkey1.pub"
    },
    {
      "meta": {
        "Rundeck-key-type": "public",
        "Rundeck-content-size": "393",
        "Rundeck-content-type": "application/pgp-keys"
      },
      "url": "http://dignan.local:4440/api/11/storage/keys/test1.pub",
      "name": "test1.pub",
      "type": "file",
      "path": "keys/test1.pub"
    }
  ],
  "url": "http://dignan.local:4440/api/11/storage/keys",
  "type": "directory",
  "path": "keys"
}

~~~~


### Get Key Metadata ####

Returns the metadata about the stored key file.

Provides a JSON or XML response based on the `Accept` request header:

    GET /api/11/storage/keys/[PATH]/[FILE]

Response:

`application/xml`

~~~~ {.xml}
<resource path='keys/test1.pub' type='file'
url='http://dignan.local:4440/api/11/storage/keys/test1.pub'
name='test1.pub'>
  <resource-meta>
    <Rundeck-content-type>
    application/pgp-keys</Rundeck-content-type>
    <Rundeck-content-size>393</Rundeck-content-size>
    <Rundeck-key-type>public</Rundeck-key-type>
  </resource-meta>
</resource>
~~~~

`application/json`

~~~~ {.json}
{
  "meta": {
    "Rundeck-key-type": "public",
    "Rundeck-content-size": "393",
    "Rundeck-content-type": "application/pgp-keys"
  },
  "url": "http://dignan.local:4440/api/11/storage/keys/test1.pub",
  "name": "test1.pub",
  "type": "file",
  "path": "keys/test1.pub"
}
~~~~

### Get Key Contents ####

Provides the **public key** content if the `Accept` request header matches `*/*` or `application/pgp-keys`:

    GET /api/11/storage/keys/[PATH]/[FILE]

**Retrieving private key or password file contents is not allowed.**

A GET request for a private key file if the `Accept` request header matches `*/*` or `application/octet-stream`,
or a password if the request header matches `*/*` or `application/x-rundeck-data-password`
will result in a `403 Unauthorized` response.

    GET /api/11/storage/keys/[PATH]/[FILE]
    Accept: application/octet-stream
    ...

Response:

    403 Unauthorized
    ...

### Delete Keys ####

Deletes the file if it exists and returns `204` response.

    DELETE /api/11/storage/keys/[PATH]/[FILE]

## Projects

### Listing Projects ###

List the existing projects on the server.

**Request:**

    GET /api/1/projects

**Response:**

An Item List of `projects`, each `project` of the form specified in the [Getting Project Info](#getting-project-info) section.

*Since API version 11*: JSON content can be retrieved with `Accept: application/json`

~~~~~ {.json}
[
    {
        "name":"...",
        "description":"...",
        "url":"...",
    }
]
~~~~~

### Project Creation ###

Create a new project.

    POST /api/11/projects

XML content:

`Content-Type: application/xml`

~~~~~~~~~~ {.xml}
<project>
    <name>name</name>
    <config>
        <property key="propname" value="propvalue"/>
        <!-- ... -->
    </config>
</project>
~~~~~~~~~~

JSON content:

`Content-Type: application/json`

~~~~~~~~~~ {.json}
{ "name": "myproject", "config": { "propname":"propvalue" } }
~~~~~~~~~~

Response:  XML or JSON project definition of the form indicated in the [Getting Project Info](#getting-project-info) section.

### Getting Project Info ###

Get information about a project.

    GET /api/1/project/[PROJECT]

**Response:**

An Item List of `projects` with one `project`.  XML or JSON is determined by the `Accept` request header. The `project` is of the form:

`Content-Type: application/xml`

~~~~~~~~~~ {.xml}
<project>
    <name>Project Name</name>
    <description>...</description>
    <!-- additional items -->
</project>
~~~~~~~~~~

If the project defines a Resource Model Provider URL, then the additional items are:

~~~~~~~~~~ {.xml}
<resources>
    <providerURL>URL</providerURL>
</resources>
~~~~~~~~~~

Updated in version 11:

    GET /api/11/project/[PROJECT]


`Content-Type: application/xml`

~~~~~~~~~~ {.xml}
<project url="http://server:4440/api/11/project/NAME">
    <name>Project Name</name>
    <description>...</description>
    <!-- additional items -->
</project>
~~~~~~~~~~

`Content-Type: application/json` *since version 11*

~~~~~~~~~~ {.json}
{
  "description": "",
  "name": "NAME",
  "url": "http://server:4440/api/11/project/NAME",
  "config": {  }
}

~~~~~~~~~~

**API version 11 and later**: If the user has `configure` authorization for the project, then the project configuration properties are included in the response.

~~~~~~~~~~ {.xml}
<config>
    <property key="[name]" value="[value]"/>
    <!-- ... -->
</config>
~~~~~~~~~~

### Project Deletion ###

Delete an existing projects on the server. Requires 'delete' authorization.

    DELETE /api/11/project/[PROJECT]

Response:

    204 No Content

### Project Configuration ###

Retrieve or modify the project configuration data.  Requires `configure` authorization for the project.

#### GET Project Configuration ####

    GET /api/11/project/[PROJECT]/config

Response, based on `Accept` header:

`Content-Type: application/xml`

~~~~~ {.xml}
<config>
    <property key="name" value="value"/>
    <!-- ... -->
</config>
~~~~~

`Content-Type: application/json`

~~~~~ {.json}
{
    "key":"value",
    "key2":"value2..."
}
~~~~~

`Content-Type: text/plain` ([Java Properties](http://en.wikipedia.org/wiki/.properties)-formatted text.)

~~~~~ {.text}
key=value
key2=value
~~~~~

#### PUT Project Configuration ####

Replaces all configuration data with the submitted values.

**Request:**

    PUT /api/11/project/[PROJECT]/config

Content:

`Content-Type: application/xml`

~~~~~ {.xml}
<config>
    <property key="key" value="value"/>
    <!-- ... -->
</config>
~~~~~

`Content-Type: application/json`

~~~~~ {.json}
{
    "key":"value",
    "key2":"value2..."
}
~~~~~

`Content-Type: text/plain` ([Java Properties](http://en.wikipedia.org/wiki/.properties)-formatted text.)

~~~~~ {.text}
key=value
key2=value
~~~~~

Response: same as [GET Project Configuration](#get-project-configuration).

### Project Configuration Keys ###

Retrieve, change or delete individual configuration properties by their key.  Requires `configure` authorization for the project.

URL:

    /api/11/project/[PROJECT]/config/[KEY]

Request and response formats:

`application/xml`:

~~~ {.xml}
<property key="[KEY]" value="key value"/>
~~~

`application/json`:

~~~ {.json}
{ "[KEY]" : "key value" }
~~~

`text/plain`: the plain text key value

~~~ {.text}
key value
~~~

#### GET Project Configuration Key ####

Retrieve the value.

    GET /api/11/project/[PROJECT]/config/[KEY]

#### PUT Project Configuration Key ####

Set the value.

    PUT /api/11/project/[PROJECT]/config/[KEY]

#### DELETE Project Configuration Key ####

Delete the key.

    DELETE /api/11/project/[PROJECT]/config/[KEY]

Response will be

    204 No Content

### Project Archive Export ###

Export a zip archive of the project.  Requires `export` authorization for the project.

    GET /api/11/project/[PROJECT]/export

Response content type is `application/zip`

Optional parameters:

* `executionIds` a list (comma-separated) of execution IDs.  If this is specified then the archive will
contain *only* executions that are specified, and will not contain Jobs, ACLs, or project configuration/readme files.
    * optionally use `POST` method with with `application/x-www-form-urlencoded` content for large lists of execution IDs
    * optionally, specify `executionIds` multiple times, with a single ID per entry.

GET Examples:

    GET /api/11/project/AlphaProject/export?executionIds=1,4,9
    GET /api/11/project/AlphaProject/export?executionIds=1&executionIds=4&executionIds=9

Post:

    POST /api/11/project/AlphaProject/export
    Content-Type: application/x-www-form-urlencoded

    executionIds=1&executionIds=4&executionIds=9&...

### Project Archive Import ###

**Request:** 

Import a zip archive to the project. Requires `import` authorization for the project.

    PUT /api/14/project/[PROJECT]/import{?jobUuidOption,importExecutions,importConfig,importACL}

Parameters:

+ `jobUuidOption` (optional, string, `preserve/remove`) ... Option declaring how duplicate Job UUIDs should be handled. If `preserve` (default) then imported job UUIDs will not be modified, and may conflict with jobs in other projects. If `remove` then all job UUIDs will be removed before importing.
+ `importExecutions` (optional, boolean, `true/false`) ... If true, import all executions and logs from the archive (default). If false, do not import executions or logs.
+ `importConfig` (optional,boolean,`true/false`) ... If true, import the project configuration from the archive. If false, do not import the project configuration (default).
+ `importACL` (optional,boolean,`true/false`) ... If true, import all of the ACL Policies from the archive. If false, do not import the ACL Policies (default).

Expected Request Content:

`Content-Type: application/zip`

**Response:**

Note: the import status indicates "failed" if any Jobs had failures,
otherwise it indicates "successful" even if other files in the archive were not imported.

Response will indicate whether the imported contents had any errors:

*All imported jobs and files were successful:*

`application/xml`

~~~ {.xml}
<import status="successful">
</import>
~~~

`application/json`

~~~ {.json}
{"import_status":"successful"}
~~~

*Some imported files failed:*

`application/xml`

~~~ {.xml}
<import status="failed">
    <errors count="[#]">
        <error>Job ABC could not be validated: ...</error>
        <error>Job XYZ could not be validated: ...</error>
    </errors>
    <executionErrors count="[#]">
        <error>Execution 123 could not be imported: ...</error>
        <error>Execution 456 could not be imported: ...</error>
    </executionErrors>
    <aclErrors count="[#]">
        <error>file.aclpolicy could not be validated: ...</error>
        <error>file2.aclpolicy could not be validated: ...</error>
    </aclErrors>
</import>
~~~

`application/json`

~~~ {.json}
{
    "import_status":"failed",
    "errors": [
        "Job ABC could not be validated: ...",
        "Job XYZ could not be validated: ..."
    ],
    "execution_errors": [
        "Execution 123 could not be imported: ...",
        "Execution 456 could not be imported: ..."
    ],
    "acl_errors": [
        "file.aclpolicy could not be validated: ...",
        "file2.aclpolicy could not be validated: ..."
    ]
}
~~~

### Updating and Listing Resources for a Project

Update or retrieve the Resources for a project.  A GET request returns the resources
for the project, and a POST request will update the resources. (**API version 2** required.)

#### List Resources for a Project

**Request:**

    GET /api/2/project/[PROJECT]/resources

See [Listing Resources](#listing-resources).

#### Update Resources for a Project

**Request:**

    POST /api/2/project/[PROJECT]/resources

POSTing to this URL will set the resources for the project to the content of the request.

Expected POST Content: For API version 2: either `text/xml` or `text/yaml` Content-Type containing the
Resource Model definition in [resource-xml](../man5/resource-xml.html) or [resource-yaml](../man5/resource-yaml.html) formats as the request body. (Note: any MIME type ending with '/yaml' or '/x-yaml' or '/xml' will be accepted).

**Since API version 3**: You can also POST data using a content type supported by a Resource Format Parser plugin.  This requires using API version `3`.

POST Result: A success or failure API response. (See [Response Format](#response-format)).

Example POST request:

    POST /api/2/project/test/resources
    Content-Type: text/yaml

    node1:
      hostname: node1
      username: bob

    node2:
      hostname: node2
      username: bob

**Response:**

    200 OK

    <result success="true">
        <success>
            <message>Resources were successfully updated for project test</message>
        </success>
    </result>


### Refreshing Resources for a Project

**DEPRECATED**

Refresh the resources for a project via its Resource Model Provider URL. The URL can be
specified as a request parameter, or the pre-configured URL for the project
can be used. (**API version 2** required.)

**Request:**

    POST /api/2/project/[PROJECT]/resources/refresh

Optional Parameters:

`providerURL`

:   Specify the Resource Model Provider URL to refresh the resources from.  If
    not specified then the configured provider URL in the `project.properties`
    file will be used.

**Response:**

A success or failure result with a message.

The URL requested as the `providerURL` must be allowed by the `project.properties` and `framework.properties` configuration settings according to these rules:

* If the `providerURL` matches the value of `project.resources.url`, it is allowed.
* Otherwise, these properties are checked as regular expressions to match the URL:
    * `project.resources.allowedURL.X` in project.properties (X starts at 0).
    * `framework.resources.allowedURL.X` in framework.properties
* If both files define allowedURL regexes, the URL must match a regex in both of them.
* Otherwise, if only one file defines regexes, the URL must match one of them.
* Otherwise if no regexes are defined in either file, the URL is rejected.

Multiple regexes can be specified in those config files by adding multiple properties:

    project.resources.allowedURL.0=^http://myserver:9090/resources/.*$
    project.resources.allowedURL.1=^http://server2:9090/resources/.*$

### Project Readme File

The `readme.md` and `motd.md` files,
which are Markdown formatted and displayed in the Project listing page,
can be managed via the API. (See [Project Readme.md](http://rundeck.org/docs/administration/project-setup.html#project-readme.md).)

**Request:**

    /api/13/project/[PROJECT]/readme.md
    /api/13/project/[PROJECT]/motd.md

Method: `GET`, `PUT` and `DELETE`.

Format: XML, JSON and plain text formats.

#### GET Readme File

    GET /api/13/project/[PROJECT]/readme.md
    GET /api/13/project/[PROJECT]/motd.md

Response format depends on the `Accept:` HTTP header.

`text/plain`:

    The readme contents

`application/xml`:

```{.xml}
<contents>The readme contents</contents>
```

*Note*: XML output will use CDATA to preserve formatting of the contents

`application/json`:

```{.json}
{"contents":"The readme contents"}
```

If the file does not exist, then the response will be : `404 Not Found`

#### PUT Readme File

    PUT /api/13/project/[PROJECT]/readme.md
    PUT /api/13/project/[PROJECT]/motd.md

To create or modify the contents, use a `PUT` request, and `Content-Type` header to specify the same format.  Use the same format as returned by the GET responses.

#### DELETE Readme File

    DELETE /api/13/project/[PROJECT]/readme.md
    DELETE /api/13/project/[PROJECT]/motd.md

Deletes the resource if it exists.

Response: `204 No Content`

### Project ACLs

Manage a set of ACL Policy files for a project.  These files will apply to the specified project only,
and must either have a `context:` section which specifies the project context, or have no `context:` section.

The request and response formats for Project ACL Policies matches that of the
[System ACL Policies][/api/V/system/acl/*],
however the URL is rooted under the Project's URL path: `/api/13/project/[PROJECT]/acl/*`.

For more information about ACL Policies see:

* [ACLPOLICY format][ACLPOLICY]
* [Access Control Policy](../administration/access-control-policy.html)

#### List Project ACL Policies

**Request:**

    GET /api/13/project/[PROJECT]/acl/

See [List System ACL Policies](#list-system-acl-policies) for request and response.

#### Get a Project ACL Policy

**Request:**

    GET /api/13/project/[PROJECT]/acl/name.aclpolicy

See [Get an ACL Policy](#get-an-acl-policy) for request and response.

#### Create a Project ACL Policy

**Request:**

    POST /api/13/project/[PROJECT]/acl/name.aclpolicy
    
See [Create an ACL Policy](#create-an-acl-policy) for request and response.

#### Update a Project ACL Policy

**Request:**

    PUT /api/13/project/[PROJECT]/acl/name.aclpolicy

See [Update an ACL Policy](#update-an-acl-policy) for request and response.

#### Delete a Project ACL Policy

**Request:**

    DELETE /api/13/project/[PROJECT]/acl/name.aclpolicy

See [Delete an ACL Policy](#delete-an-acl-policy)

## Listing History

List the event history for a project.

**Request:**

    GET /api/14/project/[PROJECT]/history

(**Deprecated URL**: `/api/14/history`, requires URL parameter: `project`.)

Optional Parameters:

* History query parameters:
    * `jobIdFilter`: include events for a job ID.
    * `reportIdFilter`: include events for an event Name.
    * `userFilter`: include events created by a user
    * `statFilter`: include events based on result status.  this can be 'succeed','fail', or 'cancel'.
    * `jobListFilter`: include events for the job by name, format: 'group/name'.  To use multiple values, include this parameter multiple times.  (Since *API v5*)
    * `excludeJobListFilter`: exclude events for the job by name, format: 'group/name'. To use multiple values, include this parameter multiple times. (Since *API v5*)
* Date query parameters:
    * `recentFilter`: Use a simple text format to filter events that occurred within a period of time. The format is "XY" where X is an integer, and "Y" is one of:
        * `h`: hour
        * `d`: day
        * `w`: week
        * `m`: month
        * `y`: year
        So a value of "2w" would return events within the last two weeks.
    * `begin`: Specify exact date for earliest result.
    * `end`: Specify exact date for latest result.
* Paging parameters:
    * `max`: indicate the maximum number of events to return. The default maximum to return is 20.
    * `offset`: indicate the 0-indexed offset for the first event to return.

The format for the `end`, and `begin` filters is either:  a unix millisecond timestamp, or a W3C dateTime string in the format "yyyy-MM-ddTHH:mm:ssZ".

The format for the `jobListFilter` and `excludeJobListFilter` is the job's group and name separated by a '/' character, such as: "group1/job name", or "my job" if there is no group.

**Response:**

`Content-Type: application/xml`: an Item List of `events`.  Each `event` has this form:

~~~~~~~~~~ {.xml}
<event starttime="[unixtime]" endtime="[unixtime]">
  <title>[job title, or "adhoc"]</title>
  <status>[status]</status>
  <summary>[summary text]</summary>
  <node-summary succeeded="[X]" failed="[Y]" total="[Z]"/>
  <user>[user]</user>
  <project>[project]</project>
  <date-started>[start date]</date-started>
  <date-ended>[end date]</date-ended>
  <!-- if the execution was aborted, the username who aborted it: -->
  <abortedby>[username]</abortedby>
  <!-- if associated with an Execution, include the execution id: -->
  <execution id="[execid]" href="[api href]" permalink="[gui href]"/>
  <!-- if associated with a Job, include the Job ID: -->
  <job id="[jobid]"  href="[api href]" permalink="[gui href]"/>
</event>
~~~~~~~~~~

The `events` element will also have `max`, `offset`, and `total` attributes, to indicate the state of paged results.  E.G:

~~~~ {.xml}
<events count="8" total="268" max="20" offset="260">
...
</events>
~~~~ {.xml}

`total` is the total number of events matching the query parameters.
`count` is the number of events included in the results.
`max` is the paging size as specified in the request, or with the default value of 20.
`offset` is the offset specified, or default value of 0.

**As of v14**: the `<execution>` and `<job>` elements will have a `href` attribute with the URL to the API for that resource, and a `permalink` attribute with the URL to the GUI view for the job or execution.

`Content-Type: application/json`:

~~~~~~ {.json}
{
  "paging": {
    "count": 10,
    "total": 110,
    "max": 20,
    "offset": 100
  },
  "events": [...]
}
~~~~~~

The `events` array contains elements like:

~~~~~ {.json}
{
  "starttime": #unixtime,
  "endtime": #unixtime,
  "title": "[job title, or "adhoc"]",
  "status": "[status]",
  "statusString": "[string]",
  "summary": "[summary text]",
  "node-summary": {
    "succeeded": #X,
    "failed": #Y,
    "total": #Z
  },
  "user": "[user]",
  "project": "[project]",
  "date-started": "[yyyy-MM-ddTHH:mm:ssZ]",
  "date-ended": "[yyyy-MM-ddTHH:mm:ssZ]",
  "job": {
    "id": "[uuid]",
    "href": "[api href]"
  },
  "execution": {
    "id": "[id]",
    "href": "[api href]"
  }
}
~~~~~

## Resources/Nodes

### Listing Resources

List or query the resources for a project.

**Request:**

    GET /api/14/project/[PROJECT]/resources

(**Deprecated URL**: `/api/1/resources` requires `project` query parameter.)

Optional Parameters:

* `format` : Result format. Default is "xml", can use "yaml" or "json", or an installed ResourceFormat plugin name.  

* Node Filter parameters: You can select resources to include and exclude in the result set, see [Using Node Filters](#using-node-filters) below.

**Note:** If no query parameters are included, the result set will include all Node resources for the project.

**Response:**

Depending on the `format` parameter, a value of "xml" will return [resource-xml](../man5/resource-xml.html) and "yaml" will return [resource-yaml](../man5/resource-yaml.html), and "json" will return [resource-json](../man5/resource-json.html) formatted results.  Any other supported format value will return content in the specified format.

### Getting Resource Info

Get a specific resource within a project.

**Request:**

    GET /api/14/project/[PROJECT]/resource/[NAME]

(**Deprecated URL**: `/api/1/resource/[NAME]` requires `project` query parameter.)

Optional Parameters:

* `format` : Result format.  Default is "xml", can use "yaml" or "json", or an installed ResourceFormat plugin name.

**Response:**

Depending on the `format` parameter, a value of "xml" will return [resource-xml](../man5/resource-xml.html) and "yaml" will return [resource-yaml](../man5/resource-yaml.html), and "json" will return [resource-json](../man5/resource-json.html) formatted results.

The result will contain a single item for the specified resource.

### Using Node Filters

Refer to the [User Guide - Node Filters](../manual/node-filters.html) Documentation for information on
the node filter syntax and usage.

A basic node filter looks like:

    attribute: value attribute2: value2

To specify a Node Filter string as a URL parameter for an API request, use a parameter named `filter`.
Your HTTP client will have to correctly escape the value of the `filter` parameter.  For example you can
use `curl` like this;

    curl --data-urlencoded "filter=attribute: value"

Common attributes:

* `name` - node name
* `tags` - tags
* `hostname`
* `username`
* `osFamily`, `osName`, `osVersion`, `osArch`

Custom attributes can also be used.

Note: previous Rundeck versions supported individual URL parameters for specific node filter attributes,
these are deprecated, but mentioned below.

##### Deprecated Node Filter parameters

To include certain resources, specify the inclusion filters:

* `hostname`, `tags`, `os-[name,family,arch,version]`, `name`

To exclude certain resources, specify the exclusion filters as above but with `exclude-` prepended:

* `exclude-hostname`, `exclude-tags`, etc..

To specify which type of filter (inclusion or exclusion) takes precedence, specify:

* `exclude-precedence` : Whether exclusion filters take precedence. "true"/"false" (default is "true").

If using only inclusion filters, the result set will be only those resources which *do* match the filters.

If using only exclusion filters, the result set will be only those resources which *do not* match the filters.

When using both types of filters, the result will depend on the `exclude-precedence` value (default true).

* When `exclude-precedence` is true:  
    1. First select the resources which *do not* match the **exclusion** filters.
    2. Then select from those the resources which *do* match the **inclusion** filters.

* When `exclude-precedence` is false:
    1. First select all resources.
    2. Then remove the resources which *do not* match the **exclusion** filters.
    3. Then add the resources which *do* match the **inclusion** filters.

The difference between these results is apparent when you have resources which are matched by both the exclusion and the inclusion filters.  The precedence determines whether those resources are included or not.

Using set logic, if "A" is the set of all resources, "X" is the set of all resources matching the exclusion filters, and "I" is the set of all resources matching the inclusion filters, then:

* when `exclude-precedence=true` then the result is:
    * $( A - X ) \cap I$
    * which is also $I - X$
* when `exclude-precedence=false` then the result is:
    * $( A - X ) \cup I$



## SCM

Rundeck SCM Plugins can be used to synchronize Job definitions with an external Source Control Management repository.

Currently Rundeck includes a single built-in plugin for Git repositories.

There are two "integration" types of SCM Plugins: `import` and `export`, and they are managed separately.

A Project can be configured with a single Import and Export plugin.  After setting up these plugins, Project and Job level "status" can be read.  Changes to Jobs within a project affect the status for Import or Export.

Plugins provide "actions" which are available based on the Project or Job status.  For example, a plugin can provide a "commit" action for a Job, which allows a user to save the changes for the job.

The specific actions, and their behaviors depend on the plugin.  The actions can be listed and performed via the API.

### List SCM Plugins

Lists the available plugins for the specified integration.  Each plugin is identified by a `type` name.

**Request**

    GET /api/15/project/[PROJECT]/scm/[INTEGRATION]/plugins

**Response**

A list of plugin description.

Each plugin has these properties:

* `type` identifier for the plugin
* `configured` true/false whether a configuration is stored for the plugin
* `enabled` true/false whether the plugin is enabled
* `title` display title for the plugin
* `description` descriptive text for the plugin


`Content-Type: application/xml`:

~~~~~~~~~~ {.xml}
<scmPluginList>
  <integration>[$integration]</integration>
  <plugins>
    <scmPluginDescription>
      <configured>[$boolean]</configured>
      <description>[$string]</description>
      <enabled>[$boolean]</enabled>
      <title>[$string]</title>
      <type>[$type]</type>
    </scmPluginDescription>
  </plugins>
</scmPluginList>
~~~~~~~~~~

`Content-Type: application/json`:

~~~~~~~~~~ {.json}
{
  "integration": "$integration",
  "plugins": [
    {
      "configured": $boolean,
      "description": "$string",
      "enabled": $boolean,
      "title": "$string",
      "type": "$type"
    }
  ]
}
~~~~~~~~~~


### Get SCM Plugin Input Fields

List the input fields for a specific plugin.  The `integration` and `type` must be specified.

The response will list each input field.

**Request**

    GET /api/15/project/[PROJECT]/scm/[INTEGRATION]/plugin/[TYPE]/input

**Response**

Input fields have a number of properties:

* `name` identifier for the field, used when submitting the input values.
* `defaultValue` a default value if the input does not specify one
* `description` textual description
* `renderOptions` a key/value map of options, such as declaring that GUI display the input as a password field.
* `required` true/false whether the input is required
* `scope` 
* `title` display title for the field
* `type` data type of the field: `String`, `Integer`, `Select` (multi-value), `FreeSelect` (open-ended multi-value), `Boolean` (true/false)
* `values` if the type is `Select` or `FreeSelect`, a list of string values to choose from


`Content-Type: application/xml`:

~~~~~~~~~~ {.xml}
<scmPluginSetupInput>
  <integration>$integration</integration>
  <type>$type</type>
  <fields>
    <scmPluginInputField>
      <defaultValue>$string</defaultValue>
      <description>$string</description>
      <name>$string</name>
      <renderingOptions>
        <entry key="$string">$string</entry>
        <!-- <entry ... -->
      </renderingOptions>
      <required>$boolean</required>
      <scope>$string</scope>
      <title>$string</title>
      <type>$string</type>
      <values>
        <!-- may be empty -->
        <string>$string</string>
        <string>$string</string>
        <!-- <string ... -->
      </values>
    </scmPluginInputField>
    <!-- 
    <scmPluginInputField>...</scmPluginInputField>
     -->
  </fields>
</scmPluginSetupInput>
~~~~~~~~~~

`Content-Type: application/json`:

~~~~~~~~~~ {.json}
{
  "fields": [
    {
      "defaultValue": "$string",
      "description": "$string",
      "name": "$string",
      "renderingOptions": {
        "$string": "$string"
      },
      "required": $boolean,
      "scope": "$string",
      "title": "$string",
      "type": "$string",
      "values": null or array
    }
    //...

  ],
  "integration": "$integration",
  "type": "$type"
}
~~~~~~~~~~

### Setup SCM Plugin for a Project

Configure and enable a plugin for a project.  

The request body is expected to contain entries for all of the `required` input fields for the plugin.

If a validation error occurs with the configuration, then the response will include detail about the errors.

**Request**

    POST /api/15/project/[PROJECT]/scm/[INTEGRATION]/plugin/[TYPE]/setup

Content:

`Content-Type: application/xml`

~~~~~ {.xml}
<scmPluginConfig>
    <config>
        <entry key="key">value</entry>
        <entry key="key2">value2</entry>
        <!-- ... -->
    </config>
</scmPluginConfig>
~~~~~

`Content-Type: application/json`

~~~~~ {.json}
{
    "config":{
        "key":"value",
        "key2":"value2..."
    }
}
~~~~~


**Response**

If a validation error occurs, the response will include information about the result.

    HTTP/1.1 400 Bad Request

`Content-Type: application/xml`:

~~~~~~~~~~ {.xml}
<scmActionResult>
  <message>Some input values were not valid.</message>
  <nextAction />
  <success>false</success>
  <validationErrors>
    <entry key="dir">required</entry>
    <entry key="url">required</entry>
  </validationErrors>
</scmActionResult>
~~~~~~~~~~

`Content-Type: application/json`:

~~~~~~~~~~ {.json}
{
  "message": "Some input values were not valid.",
  "nextAction": null,
  "success": false,
  "validationErrors": {
    "dir": "required",
    "url": "required"
  }
}
~~~~~~~~~~

If the result is successul:

    HTTP/1.1 200 OK

`Content-Type: application/xml`:

~~~~~~~~~~ {.xml}
<scmActionResult>
  <message>$string</message>
  <success>true</success>
  <nextAction />
  <validationErrors/>
</scmActionResult>
~~~~~~~~~~

`Content-Type: application/json`:

~~~~~~~~~~ {.json}
{
  "message": "$string",
  "nextAction": null,
  "success": true,
  "validationErrors": null
}
~~~~~~~~~~

If a follow-up **Action** is expected to be called, the action ID will be identified by the `nextAction` value.

See [Get Project SCM Status](#get-project-scm-status).

### Enable SCM Plugin for a Project

Enable a plugin that was previously configured. (Idempotent)

**Request**

    POST /api/15/project/[PROJECT]/scm/[INTEGRATION]/plugin/[TYPE]/enable

No content body is expected.

**Response**

Same response as [Setup SCM Plugin for a Project](#setup-scm-plugin-for-a-project).

### Disable SCM Plugin for a Project

Disable a plugin. (Idempotent)

**Request**

    POST /api/15/project/[PROJECT]/scm/[INTEGRATION]/plugin/[TYPE]/disable

No content body is expected.

**Response**

Same response as [Setup SCM Plugin for a Project](#setup-scm-plugin-for-a-project).

### Get Project SCM Status

Get the SCM plugin status and available actions for the project.

**Request**

    GET /api/15/project/[PROJECT]/scm/[INTEGRATION]/status

**Response**

If no plugin is configured:

    HTTP/1.1 404 Not Found

Otherwise:

    HTTP/1.1 200 OK

The plugin status has these properties:

* `actions` empty, or a list of action ID strings
* `integration` the integration
* `message` a string indicating the status message
* `synchState` a value indicating the state
* `project` project name

Import plugin values for `synchState`:

* `CLEAN` - no changes
* `UNKNOWN` - status unknown
* `REFRESH_NEEDED` - plugin needs to refresh
* `IMPORT_NEEDED` - some changes need to be imported
* `DELETE_NEEDED` - some jobs need to be deleted

Export plugin values for `synchState`:

* `CLEAN` - no changes
* `REFRESH_NEEDED` - plugin needs to refresh
* `EXPORT_NEEDED` - some changes need to be exported
* `CREATE_NEEDED` - some jobs need to be added to the repo


`Content-Type: application/xml`:

~~~~~~~~~~ {.xml}
<scmProjectStatus>
  
  <actions>
    <string>action1</string>
    <string>action2</string>
  </actions>
  
  <integration>$integration</integration>
  <message>$string</message>
  <project>$project</project>
  <synchState>$state</synchState>
</scmProjectStatus>
~~~~~~~~~~

`Content-Type: application/json`:

~~~~~~~~~~ {.json}
{
  "actions": ['action1','action2',..],
  "integration": "$integration",
  "message": null,
  "project": "$project",
  "synchState": "$state"
}
~~~~~~~~~~

### Get Project SCM Config

Get the configuration properties for the current plugin.

**Request**

    GET /api/15/project/[PROJECT]/scm/[INTEGRATION]/config

**Response**

If no plugin for the given integration is configured for the project, a `404` response is sent:

    HTTP/1.1 404 Not Found

Otherwise the response contains:

* `config` a set of key/value pairs for the configuration
* `enabled` true/false if it is enabled
* `integration` integration name
* `project` project name
* `type` plugin type name

`Content-Type: application/xml`:

~~~~~~~~~~ {.xml}
<scmProjectPluginConfig>
  <config>
    <entry key="key">value</entry>
    <entry key="key2">value2</entry>
    <!-- <entry ..>...</entry> -->
  </config>
  <enabled>$boolean</enabled>
  <integration>$integration</integration>
  <project>$project</project>
  <type>$type</type>
</scmProjectPluginConfig>
~~~~~~~~~~

`Content-Type: application/json`:

~~~~~~~~~~ {.json}
{
  "config": {
    "key": "$string",
    "key2": "$string"
  },
  "enabled": $boolean,
  "integration": "$integration",
  "project": "$project",
  "type": "$type"
}
~~~~~~~~~~

### Get Project SCM Action Input Fields

Get the input fields and selectable items for a specific action.  

Each action may have a set of Input Fields describing user-input values.

Export actions may have a set of `scmExportActionItem`s which describe Job changes that can be
included in the action.

Import actions may have a set of `scmImportActionItem`s which describe paths from the import repo
which can be selected for the action, they will also be associated with a Job after they are matched.

**Request**

    GET /api/15/project/[PROJECT]/scm/[INTEGRATION]/action/[ACTION_ID]/input

**Response**

`Content-Type: application/xml`:

The content of `<scmPluginInputField>` is the same as shown in [Get SCM Plugin Input Fields](#get-scm-plugin-input-fields).

`scmExportActionItem` values:

* `itemId` - ID of the repo item, e.g. a file path
* `job` - job information
    * `groupPath` group path, or empty/null
    * `jobId` job ID
    * `jobName` job name
* `deleted` - boolean, whether the job was deleted and requires deleting the associated repo item
* `renamed` - boolean if the job was renamed
* `originalId` - ID of a repo item if the job was renamed and now is stored at a different repo path, or empty/null

`scmImportActionItem` values:

* `itemId` - ID of the repo item, e.g. a file path
* `job` - job information, may be empty/null
    * `groupPath` group path, or empty
    * `jobId` job ID
    * `jobName` job name
* `tracked` - boolean, true if there is an associated `job`



~~~~~~~~~~ {.xml}
<scmActionInput>
  <actionId>$actionId</actionId>
  <description />
  <fields>
    <scmPluginInputField>...</scmPluginInputField>
  </fields>
  <integration>$integration</integration>
  <title>$string</title>
  <importItems>
    <!-- import only -->
    <scmImportActionItem>
      <itemId>$string</itemId>
      <job>
        <!-- job tag may be empty if no associated job-->
          <groupPath>$jobgroup</groupPath>
          <jobId>$jobid</jobId>
          <jobName>$jobname</jobName>
      </job>
      <tracked>$boolean</tracked>
    </scmImportActionItem>
  </importItems>
  <exportItems>
    <!-- export only -->
    <scmExportActionItem>
      <deleted>$boolean</deleted>
      <itemId>$string</itemId>
      <job>
        <groupPath>$jobgroup</groupPath>
        <jobId>$jobid</jobId>
        <jobName>$jobname</jobName>
      </job>
      <originalId>$string</originalId>
      <renamed>$boolean</renamed>
    </scmExportActionItem>
  </exportItems>
</scmActionInput>
~~~~~~~~~~

`Content-Type: application/json`:

The content of `"fields"` array is the same as shown in [Get SCM Plugin Input Fields](#get-scm-plugin-input-fields).

~~~~~~~~~~ {.json}
{
  "actionId": "$actionId",
  "description": "$string",
  "fields": [
    { "name": ...
    }
  ],
  "integration": "$integration",
  "title": "$string",
  "importItems": [
    {
      "itemId": "$string",
      "job": {
        "groupPath": "$jobgroup",
        "jobId": "$jobid",
        "jobName": "$jobname"
      },
      "tracked": $boolean
    }
  ],
  "exportItems": [
    {
      "deleted": $boolean,
      "itemId": "$string",
      "job": {
        "groupPath": "$jobgroup",
        "jobId": "$jobid",
        "jobName": "$jobname"
      },
      "originalId": "$string",
      "renamed": $boolean
    }
  ]
}
~~~~~~~~~~

### Perform Project SCM Action

Perform the action for the SCM integration plugin, with a set of input parameters,
selected Jobs, or Items, or Items to delete.

Depending on the [available Input Fields for the action](#get-project-scm-action-input-fields), the action will
expect a set of `input` values.  

The set of `jobs` and `items` to choose from will be included in the Input Fields response,
however where an Item has an associated Job, you can supply either the Job ID, or the Item ID.

When there are items to be deleted (`export` integration), you can specify the Item IDs in the `deleted`
section.  However, if the item is associated with a renamed Job, including the Job ID will have the same effect.

Note: including the Item ID of an associated job, instead of the Job ID,
will not automatically delete a renamed item.


**Request**

    POST /api/15/project/[PROJECT]/scm/[INTEGRATION]/action/[ACTION_ID]

`Content-Type: application/xml`:

~~~~~~~~~~ {.xml}
<scmAction>
    <input>
        <entry key="message">$commitMessage</entry>
    </input>
    <jobs>
        <job jobId="$jobId"/>
    </jobs>
    <items>
        <item itemId="$itemId"/>
    </items>
    <deleted></deleted>
</scmAction>
~~~~~~~~~~

`Content-Type: application/json`:

~~~~~~~~~~ {.json}
{
    "input":{
        "message":"$commitMessage"
    },
    "jobs":[
        "$jobId"
    ],
    "items":[
        "$itemId"
    ],
    "deleted":null
}
~~~~~~~~~~

**Response**

Same response as [Setup SCM Plugin for a Project](#setup-scm-plugin-for-a-project).

### Get Job SCM Status

**Request**

    GET /api/15/job/[ID]/scm/[INTEGRATION]/status

**Response**

Note: `import` status will not include any actions for the job, refer to the Project status to list import actions.

Import plugin values for `$synchState`:

* `CLEAN` - no changes
* `UNKNOWN` - status unknown, e.g. the job was not imported via SCM
* `REFRESH_NEEDED` - plugin needs to refresh
* `IMPORT_NEEDED` - Job changes need to be imported
* `DELETE_NEEDED` - Job need to be deleted

Export plugin values for `$synchState`:

* `CLEAN` - no changes
* `REFRESH_NEEDED` - plugin needs to refresh
* `EXPORT_NEEDED` - job changes need to be exported
* `CREATE_NEEDED` - Job needs to be added to the repo


`Content-Type: application/xml`:

~~~~~~~~~~ {.xml}
<scmJobStatus>
  <actions>
    <string>$action</string>
    <!--
    <string>$action2</string>
    -->
  </actions>
  <commit>
    <author>$commitAuthor</author>
    <commitId>$commitId</commitId>
    <date>$commitDate</date>
    <info>
      <entry key="key">value</entry>
      <!-- <entry key="..">...</entry> -->
    </info>
    <message>$commitMessage</message>
  </commit>
  <id>$jobId</id>
  <integration>$integration</integration>
  <message>$statusMessage</message>
  <project>$project</project>
  <synchState>$synchState</synchState>
</scmJobStatus>
~~~~~~~~~~

`Content-Type: application/json`:

~~~~~~~~~~ {.json}
{
  "actions": [
    "$action"
  ],
  "commit": {
    "author": "$commitAuthor",
    "commitId": "$commitId",
    "date": "$commitDate",
    "info": {
      "key": "value.."
    },
    "message": "$commitMessage"
  },
  "id": "$jobId",
  "integration": "$integration",
  "message": "$statusMessage",
  "project": "$project",
  "synchState": "$synchState"
}
~~~~~~~~~~

### Get Job SCM Diff

Retrieve the file diff for the Job, if there are changes for the integration.

The format of the diff content depends on the specific plugin. For the Git plugins,
a unified diff format is used.

**Request**

    GET /api/15/job/[ID]/scm/[INTEGRATION]/diff

**Response**

The `commit` info will be the same structure as in [Get Job SCM Status](#get-job-scm-status).

For `import` only, `incomingCommit` will indicate the to-be-imported change.

For `application/xml`, the `diffContent` will use a CDATA section to preserve whitespace.


`Content-Type: application/xml`:

~~~~~~~~~~ {.xml}
<scmJobDiff>
  <commit>
    <!-- commit info -->
  </commit>
  <diffContent><![CDATA[...]]></diffContent>
  <id>$jobId</id>
  <incomingCommit>
    <!-- import only: incoming commit info -->
  </incomingCommit>
  <integration>$integration</integration>
  <project>$project</project>
</scmJobDiff>
~~~~~~~~~~

`Content-Type: application/json`:

~~~~~~~~~~ {.json}
{
  "commit": {
    ...
  },
  "diffContent": "...",
  "id": "$jobId",
  "incomingCommit": {
    ...
  },
  "integration": "$integration",
  "project": "$project"
}
~~~~~~~~~~

### Get Job SCM Action Input Fields

Get the input fields and selectable items for a specific action for a job.  

Each action may have a set of Input Fields describing user-input values.

Export actions will include one `scmExportActionItem` for the Job.

Import actions may have a set of `scmImportActionItem` for the job.

**Request**

    GET /api/15/job/[ID]/scm/[INTEGRATION]/action/[ACTION_ID]/input

**Response**

The same response format as in [Get Project SCM Action Input Fields](#get-project-scm-action-input-fields).

### Perform Job SCM Action

**Request**

    POST /api/15/job/[ID]/scm/[INTEGRATION]/action/[ACTION_ID]

Request Content is nearly exactly as expected in [Perform Project SCM Action](#perform-project-scm-action),
however the `jobIds` do not need to be specified, as the `ID` of the job is already specified.
The `items` and `deleted` sections are not used.

Only the `input` values need to be specified:

`Content-Type: application/xml`:

~~~~~~~~~~ {.xml}
<scmAction>
    <input>
        <entry key="message">$commitMessage</entry>
    </input>
</scmAction>
~~~~~~~~~~

`Content-Type: application/json`:

~~~~~~~~~~ {.json}
{
    "input":{
        "message":"$commitMessage"
    }
}
~~~~~~~~~~

**Response**


Same response as [Setup SCM Plugin for a Project](#setup-scm-plugin-for-a-project).


## Index

[/api/V/execution/[ID]][]

* `GET` [Execution Info](#execution-info)
* `DELETE` [Delete an Execution](#delete-an-execution)

[/api/V/execution/[ID]/abort][]

* `GET` [Aborting Executions](#aborting-executions)

[/api/V/execution/[ID]/output/state][]

* `GET` [Execution Output with State](#execution-output-with-state)

[/api/V/execution/[ID]/output][]

* `GET` [Tailing Output](#tailing-output)

[/api/V/execution/[ID]/output/step/[STEPCTX]][]

[/api/V/execution/[ID]/output/node/[NODE]/step/[STEPCTX]][]

[/api/V/execution/[ID]/output/node/[NODE]][]

[/api/V/execution/[ID]/output][]

* `GET` [Execution Output](#execution-output)

[/api/V/execution/[ID]/state][]

* `GET` [Execution State](#execution-state)

[/api/V/executions/delete][]

* `POST` [Bulk Delete Executions](#bulk-delete-executions)

[/api/V/job/[ID]][]

* `GET` [Getting a Job Definition](#getting-a-job-definition)
* `DELETE` [Deleting a Job Definition](#deleting-a-job-definition)

[/api/V/job/[ID]/executions][]

* `POST` [Running a Job](#running-a-job)
* `GET` [Getting Executions for a Job](#getting-executions-for-a-job)
* `DELETE` [Delete all Executions for a Job](#delete-all-executions-for-a-job)


[/api/V/job/[ID]/execution/enable][]

* `POST` [Enable Executions for a Job](#enable-executions-for-a-job)

[/api/V/job/[ID]/execution/disable][]

* `POST` [Disable Executions for a Job](#disable-executions-for-a-job)

[/api/V/job/[ID]/run][]

* `POST` [Running a Job](#running-a-job)

[/api/V/job/[ID]/schedule/enable][]

* `POST` [Enable Scheduling for a Job](#enable-scheduling-for-a-job)

[/api/V/job/[ID]/schedule/disable][]

* `POST` [Disable Scheduling for a Job](#disable-scheduling-for-a-job)
    
[/api/V/job/[ID]/scm/[INTEGRATION]/status][] 

- `GET` [Get SCM status for a Job][/api/V/job/[ID]/scm/[INTEGRATION]/status]

[/api/V/job/[ID]/scm/[INTEGRATION]/action/[ACTION_ID]][] 

- `POST` [Perform SCM action for a Job.][/api/V/job/[ID]/scm/[INTEGRATION]/action/[ACTION_ID]]

[/api/V/job/[ID]/scm/[INTEGRATION]/action/[ACTION_ID]/input][] 

- `GET` [Get Job SCM Action Input Fields.][/api/V/job/[ID]/scm/[INTEGRATION]/action/[ACTION_ID]/input]

[/api/V/jobs/delete][]

* `DELETE` [Bulk Job Delete](#bulk-job-delete)

[/api/V/jobs/execution/enable][]

* `POST` [Bulk Toggle Job Execution](#bulk-toggle-job-execution)

[/api/V/jobs/execution/disable][]

* `POST` [Bulk Toggle Job Execution](#bulk-toggle-job-execution)

[/api/V/jobs/schedule/enable][]

* `POST` [Bulk Toggle Job Schedules](#bulk-toggle-job-schedules)

[/api/V/jobs/schedule/disable][]

* `POST` [Bulk Toggle Job Schedules](#bulk-toggle-job-schedules)

[/api/V/project/[PROJECT]][]

* `GET` [Getting Project Info](#getting-project-info)
* `DELETE` [Project Deletion](#project-deletion)

[/api/V/project/[PROJECT]/acl/*][]

* `GET` [List Project ACL Policies](#list-project-acl-policies)
* `GET` [Get a Project ACL Policy](#get-a-project-acl-policy)
* `POST` [Create a Project ACL Policy](#create-a-project-acl-policy)
* `PUT` [Update a Project ACL Policy](#update-a-project-acl-policy)
* `DELETE` [Delete a Project ACL Policy](#delete-a-project-acl-policy)

[/api/V/project/[PROJECT]/config][]

* `GET` [GET Project Configuration](#get-project-configuration)
* `PUT` [PUT Project Configuration](#put-project-configuration)

[/api/V/project/[PROJECT]/config/[KEY]][]

* `GET` [GET Project Configuration Key](#get-project-configuration-key)
* `PUT` [PUT Project Configuration Key](#put-project-configuration-key)
* `DELETE` [DELETE Project Configuration Key](#delete-project-configuration-key)

[/api/V/project/[PROJECT]/executions][]

* `GET` [Execution Query](#execution-query)

[/api/V/project/[PROJECT*]/executions/running][]

* `GET` [Listing Running Executions](#listing-running-executions)

[/api/V/project/[PROJECT]/export][]

* `GET` [Project Archive Export](#project-archive-export)

[/api/V/project/[PROJECT]/[FILE.md]][]

* `GET` [GET Readme File](#get-readme-file)
* `PUT` [PUT Readme File](#put-readme-file)
* `DELETE` [DELETE Readme File](#delete-readme-file)

[/api/V/project/[PROJECT]/history][]

* `GET` [Listing History](#listing-history)

[/api/V/project/[PROJECT]/import][]

* `PUT` [Project Archive Import](#project-archive-import)

[/api/V/project/[PROJECT]/jobs][]

* `GET` [Listing Jobs](#listing-jobs)

[/api/V/project/[PROJECT]/jobs/export][]

* `GET` [Exporting Jobs](#exporting-jobs)

[/api/V/project/[PROJECT]/jobs/import][]

* `POST` [Importing Jobs](#importing-jobs)

[/api/V/project/[PROJECT]/resources][]

* `GET` [Listing Resources](#listing-resources)
* `POST` [Update Resources for a Project](#update-resources-for-a-project)

[/api/V/project/[PROJECT]/resource/[NAME]][]

* `GET` [Getting Resource Info](#getting-resource-info)

[/api/V/project/[PROJECT]/resources/refresh][]

* `POST` [Refreshing Resources for a Project](#refreshing-resources-for-a-project)

[/api/V/project/[PROJECT]/run/command][]

* `POST` [Running Adhoc Commands](#running-adhoc-commands)

[/api/V/project/[PROJECT]/run/script][]

* `POST` [Running Adhoc Scripts](#running-adhoc-scripts)

[/api/V/project/[PROJECT]/run/url][]

* `POST` [Running Adhoc Script URLs](#running-adhoc-script-urls)

[/api/V/project/[PROJECT]/scm/[INTEGRATION]/plugins][]

* `GET` [List SCM plugins for a project.][/api/V/project/[PROJECT]/scm/[INTEGRATION]/plugins]

[/api/V/project/[PROJECT]/scm/[INTEGRATION]/plugin/[TYPE]/input][]

* `GET` [Get SCM plugin setup input fields.][/api/V/project/[PROJECT]/scm/[INTEGRATION]/plugin/[TYPE]/input]

[/api/V/project/[PROJECT]/scm/[INTEGRATION]/plugin/[TYPE]/setup][]

* `POST` [Setup SCM for a project.][/api/V/project/[PROJECT]/scm/[INTEGRATION]/plugin/[TYPE]/setup]

[/api/V/project/[PROJECT]/scm/[INTEGRATION]/plugin/[TYPE]/enable][]

* `POST` [Enable SCM for a project.][/api/V/project/[PROJECT]/scm/[INTEGRATION]/plugin/[TYPE]/enable]

[/api/V/project/[PROJECT]/scm/[INTEGRATION]/plugin/[TYPE]/disable][]

* `POST` [Disable SCM for a project.][/api/V/project/[PROJECT]/scm/[INTEGRATION]/plugin/[TYPE]/disable]

[/api/V/project/[PROJECT]/scm/[INTEGRATION]/status][]

* `GET` [Get SCM status for a project.][/api/V/project/[PROJECT]/scm/[INTEGRATION]/status]

[/api/V/project/[PROJECT]/scm/[INTEGRATION]/config][]

* `GET` [Get SCM config for a project.][/api/V/project/[PROJECT]/scm/[INTEGRATION]/config]]

[/api/V/project/[PROJECT]/scm/[INTEGRATION]/action/[ACTION_ID]][]

* `POST` [Perform SCM action for a project.][/api/V/project/[PROJECT]/scm/[INTEGRATION]/action/[ACTION_ID]]

[/api/V/project/[PROJECT]/scm/[INTEGRATION]/action/[ACTION_ID]/input][]

* `GET` [Get Project SCM Action Input Fields.][/api/V/project/[PROJECT]/scm/[INTEGRATION]/action/[ACTION_ID]/input]

[/api/V/projects][]

* `GET` [Listing Projects](#listing-projects)
* `POST` [Project Creation](#project-creation)

[/api/V/scheduler/takeover][]

* `PUT` [Takeover Schedule in Cluster Mode](#takeover-schedule-in-cluster-mode)

[/api/V/scheduler/jobs][]

* `GET` [List Scheduled Jobs For this Cluster Server][/api/V/scheduler/jobs]

[/api/V/scheduler/server/[UUID]/jobs][]

* `GET` [List Scheduled Jobs For a Cluster Server][/api/V/scheduler/server/[UUID]/jobs]

[/api/V/storage/keys/[PATH]/[FILE]][]

* `PUT` [Upload Keys](#upload-keys)
* `GET` [List keys](#list-keys)
* `GET` [Get Key Metadata](#get-key-metadata)
* `GET` [Get Key Contents](#get-key-contents)
* `DELETE` [Delete Keys](#delete-keys)

[/api/V/system/acl/*][]

* `GET` [List System ACL Policies](#list-system-acl-policies)
* `GET` [Get an ACL Policy](#get-an-acl-policy)
* `POST` [Create an ACL Policy](#create-an-acl-policy)
* `PUT` [Update an ACL Policy](#update-an-acl-policy)
* `DELETE` [Delete an ACL Policy](#delete-an-acl-policy)

[/api/V/system/executions/enable][]

* `POST` [Set Active Mode](#set-active-mode)

[/api/V/system/executions/disable][]

* `POST` [Set Passive Mode](#set-passive-mode)

[/api/V/system/info][]

* `GET` [System Info](#system-info)

[/api/V/system/logstorage][]

* `GET` [Log Storage Info][/api/V/system/logstorage]

[/api/V/system/logstorage/incomplete][]

* `GET` [List Executions with Incomplete Log Storage][/api/V/system/logstorage/incomplete]

[/api/V/system/logstorage/incomplete/resume][]

* `POST` [Resume Incomplete Log Storage][/api/V/system/logstorage/incomplete/resume]

[/api/V/tokens][]

[/api/V/tokens/[USER]][]

* `GET` [List Tokens](#list-tokens)
* `POST` [Create a Token](#create-a-token)

[/api/V/token/[ID]][]

* `GET` [Get a token](#get-a-token)
* `DELETE` [Delete a token](#delete-a-token)



[/api/V/project/[PROJECT]/scm/[INTEGRATION]/plugins]:#list-scm-plugins
[/api/V/project/[PROJECT]/scm/[INTEGRATION]/plugin/[TYPE]/input]:#get-scm-plugin-input-fields
[/api/V/project/[PROJECT]/scm/[INTEGRATION]/plugin/[TYPE]/setup]:#setup-scm-plugin-for-a-project
[/api/V/project/[PROJECT]/scm/[INTEGRATION]/plugin/[TYPE]/enable]:#enable-scm-plugin-for-a-project
[/api/V/project/[PROJECT]/scm/[INTEGRATION]/plugin/[TYPE]/disable]:#disable-scm-plugin-for-a-project
[/api/V/project/[PROJECT]/scm/[INTEGRATION]/status]:#get-project-scm-status
[/api/V/project/[PROJECT]/scm/[INTEGRATION]/config]:#get-project-scm-config
[/api/V/project/[PROJECT]/scm/[INTEGRATION]/action/[ACTION_ID]]:#perform-project-scm-action
[/api/V/project/[PROJECT]/scm/[INTEGRATION]/action/[ACTION_ID]/input]:#get-project-scm-action-input-fields
[/api/V/job/[ID]/scm/[INTEGRATION]/status]:#get-job-scm-status
[/api/V/job/[ID]/scm/[INTEGRATION]/action/[ACTION_ID]]:#perform-job-scm-action
[/api/V/job/[ID]/scm/[INTEGRATION]/action/[ACTION_ID]/input]:#get-job-scm-action-input-fields


[/api/V/execution/[ID]]: #execution-info

[/api/V/execution/[ID]/abort]:#aborting-executions

[/api/V/execution/[ID]/output/state]:#execution-output-with-state

[/api/V/execution/[ID]/output/step/[STEPCTX]]:#execution-output

[/api/V/execution/[ID]/output/node/[NODE]/step/[STEPCTX]]:#execution-output

[/api/V/execution/[ID]/output/node/[NODE]]:#execution-output

[/api/V/execution/[ID]/output]:#execution-output

[/api/V/execution/[ID]/state]:#execution-state

[/api/V/executions/delete]:#bulk-delete-executions



[/api/V/job/[ID]]:#getting-a-job-definition
[DELETE /api/V/job/[ID]]:#deleting-a-job-definition

[/api/V/job/[ID]/executions]:#getting-executions-for-a-job

[/api/V/job/[ID]/execution/enable]:#enable-executions-for-a-job

[/api/V/job/[ID]/execution/disable]:#disable-executions-for-a-job

[POST /api/V/job/[ID]/executions]:#running-a-job
[DELETE /api/V/job/[ID]/executions]:#delete-all-executions-for-a-job


[/api/V/job/[ID]/schedule/enable]:#enable-scheduling-for-a-job

[/api/V/job/[ID]/schedule/disable]:#disable-scheduling-for-a-job


[/api/V/job/[ID]/run]:#running-a-job

[/api/V/jobs/delete]:#bulk-job-delete
[/api/V/jobs/execution/enable]:#bulk-toggle-job-execution
[/api/V/jobs/execution/disable]:#bulk-toggle-job-execution
[/api/V/jobs/schedule/enable]:#bulk-toggle-job-schedules
[/api/V/jobs/schedule/disable]:#bulk-toggle-job-schedules

[/api/V/project/[PROJECT]]:#getting-project-info
[DELETE /api/V/project/[PROJECT]]:#project-deletion

[/api/V/project/[PROJECT]/acl/*]:#project-acls

[/api/V/project/[PROJECT]/config]:#get-project-configuration
[PUT /api/V/project/[PROJECT]/config]:#put-project-configuration


[/api/V/project/[PROJECT]/config/[KEY]]:#get-project-configuration-key
[PUT /api/V/project/[PROJECT]/config/[KEY]]:#put-project-configuration-key
[DELETE /api/V/project/[PROJECT]/config/[KEY]]:#delete-project-configuration-key


[/api/V/project/[PROJECT]/executions]:#execution-query


[/api/V/project/[PROJECT*]/executions/running]:#listing-running-executions


[/api/V/project/[PROJECT]/export]:#project-archive-export


[/api/V/project/[PROJECT]/[FILE.md]]:#get-readme-file
[PUT /api/V/project/[PROJECT]/[FILE.md]]:#put-readme-file
[DELETE /api/V/project/[PROJECT]/[FILE.md]]:#delete-readme-file

[/api/V/project/[PROJECT]/history]:#listing-history

[/api/V/project/[PROJECT]/import]:#project-archive-import

[/api/V/project/[PROJECT]/jobs]:#listing-jobs

[/api/V/project/[PROJECT]/jobs/export]:#exporting-jobs

[/api/V/project/[PROJECT]/jobs/import]:#importing-jobs

[/api/V/project/[PROJECT]/resources]:#listing-resources
[POST /api/V/project/[PROJECT]/resources]:#update-resources-for-a-project

[/api/V/project/[PROJECT]/resource/[NAME]]:#getting-resource-info

[/api/V/project/[PROJECT]/resources/refresh]:#refreshing-resources-for-a-project

[/api/V/projects]:#listing-projects

[POST /api/V/projects]:#project-creation

[/api/V/project/[PROJECT]/run/command]:#running-adhoc-commands

[/api/V/project/[PROJECT]/run/script]:#running-adhoc-scripts

[/api/V/project/[PROJECT]/run/url]:#running-adhoc-script-urls

[/api/V/scheduler/takeover]:#takeover-schedule-in-cluster-mode

[/api/V/scheduler/jobs]:#list-scheduled-jobs-for-this-cluster-server

[/api/V/scheduler/server/[UUID]/jobs]:#list-scheduled-jobs-for-a-cluster-server

[/api/V/storage/keys/[PATH]/[FILE]]:#list-keys
[PUT /api/V/storage/keys/[PATH]/[FILE]]:#upload-keys
[DELETE /api/V/storage/keys/[PATH]/[FILE]]:#delete-keys


[/api/V/system/acl/*]:#acls
[/api/V/system/info]:#system-info
[/api/V/system/executions/enable]:#set-active-mode
[/api/V/system/executions/disable]:#set-passive-mode

[/api/V/system/logstorage]:#log-storage-info
[/api/V/system/logstorage/incomplete]:#list-executions-with-incomplete-log-storage
[/api/V/system/logstorage/incomplete/resume]:#resume-incomplete-log-storage
[POST /api/V/system/logstorage/incomplete/resume]:#resume-incomplete-log-storage

[/api/V/tokens]:#list-tokens
[/api/V/tokens/[USER]]:#list-tokens
[POST /api/V/tokens/[USER]]:#create-a-token
[/api/V/token/[ID]]:#get-a-token
[DELETE /api/V/token/[ID]]:#delete-a-token

[ACLPOLICY]:../man5/aclpolicy.html