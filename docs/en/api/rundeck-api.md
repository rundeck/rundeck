% API Reference | Version ${APIVERS}
% Alex Honor; Greg Schueler
% November 20, 2010

Rundeck provides a Web API for use with your application.  

API Version Number
----

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

~~~~~~~~~~~~~~~~~~~ {.xml}
<result error="true" apiversion="2">
    <error code="api-version-unsupported">
        <message>
        Unsupported API Version "1". API Request: /rundeck/api/1/project/test/jobs. Reason: Minimum supported version: 2
        </message>
    </error>
</result>
~~~~~~~~~~~~~~~~~~~

### Changes

Changes introduced by API Version number:

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
    - `/api/11/project/[NAME]/config`
        - PUT and GET for [Project Configuration](#project-configuration)
    - `/api/11/project/[NAME]/config/[KEY]`
        + PUT, GET, DELETE for [Project Configuration Keys](#project-configuration-keys)
    - `/api/11/project/[NAME]/export`
        + GET to retrieve archive of a project - [Project Archive Export](#project-archive-export)
    - `/api/11/project/[NAME]/import`
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
    - `/api/11/project/[NAME]`
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
    * `/api/2/project/[NAME]/resources` - [Updating and Listing Resources for a Project](#updating-and-listing-resources-for-a-project)
        * `POST` request Content-Type can be any MIME type supported by a Resource Format Parser plugin.

**Version 2**:

* New endpoints
    * `/api/2/project/[NAME]/jobs` - [Listing Jobs for a Project](#listing-jobs-for-a-project)
    * `/api/2/project/[NAME]/resources` - [Updating and Listing Resources for a Project](#updating-and-listing-resources-for-a-project)
    * `/api/2/project/[NAME]/resources/refresh` - [Refreshing Resources for a Project](#refreshing-resources-for-a-project)
* Updated endpoints
    * `/api/1/jobs` - [Listing Jobs](#listing-jobs)
        * Additional parameters added

URLs
----

The Rundeck server has a "Base URL", where you access the server. Your Rundeck Server URL may look like: `http://myserver:4440`.

The root URL path for all calls to the API in this version is:

    $RUNDECK_SERVER_URL/api/2

XML and JSON
----

The majority of API calls use XML for input and output.  Some import/export features support YAML formatted documents, but XML is used for most API-level information.

As of API version 11, new and updated endpoints will support JSON format, with content type `application/json`.

JSON results can be retrieved by sending the HTTP "Accept" header with a `application/json` value.  JSON request content is supported when the HTTP "Content-Type" header specifies `application/json`.

If an "Accept" header is not specified, then the response will be either the same format as the request content (for POST, or PUT requests), or XML by default.

Authentication
-----

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

Response Format
------

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


API Contents
-----

### Authentication Tokens ###

Authentication tokens can be managed via the API itself.

#### List Tokens ####

List all tokens or all tokens for a specific user.

Request:

    GET /api/11/tokens
    GET /api/11/tokens/[USER]

Result:

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

#### Get a token ####

Get a specified auth token.

Request:

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

#### Create a Token ####

Create a new token for a specific user.

Request:

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

#### Delete a token ####

Delete a specified auth token.

Request:

    DELETE /api/11/token/[ID]

Response:

    204 No Content

### System Info ###

Get Rundeck server information and stats.

URL:

    /api/1/system/info

Parameters: none

Result: Success response, with included system info and stats in this format:

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
        </scheduler>
        <threads>
            <active>24</active>
        </threads>
    </stats>
    <metrics href='http://dignan:4440/metrics/metrics?pretty=true' contentType='text/json' />
    <threadDump href='http://dignan:4440/metrics/threads' contentType='text/plain' />
</system>
~~~~~~~~~~~~~~~

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

`threads/active`

:   Number of active Threads in the JVM


### Listing Jobs ###

List the jobs that exist for a project.

URL:

    /api/1/jobs

Required parameters:

* `project`: the project name

The following parameters can also be used to narrow down the result set.

* `idlist`: specify a comma-separated list of Job IDs to include
* `groupPath`: specify a group or partial group path to include all jobs within that group path. (Default value: "*", all groups).  Set to the special value "-" to match the top level jobs only.
* `jobFilter`: specify a filter for the job Name. Matches any job name that contains this value.
* `jobExactFilter`: specify an exact job name to match. (**API version 2** required.)
* `groupPathExact`: specify an exact group path to match.  Set to the special value "-" to match the top level jobs only. (**API version 2** required.)

Result:  An Item List of `jobs`. Each `job` is of the form:

~~~~~~~~~~ {.xml}
    <job id="ID">
        <name>Job Name</name>
        <group>Job Name</group>
        <project>Project Name</project>
        <description>...</description>
    </job>
~~~~~~~~~~~~

Note: If neither `groupPath` nor `groupPathExact` are specified, then the default `groupPath` value of "*" will be used (matching jobs in all groups).  `groupPathExact` cannot be combined with `groupPath`.  You can set either one to "-" to match only the top-level jobs which are not within a group.

### Listing Jobs for a Project ###

List the jobs that exist for a project. (**API version 2** required.)

URL:

    /api/2/project/[NAME]/jobs

The following parameters can also be used to narrow down the result set.

* `idlist`: specify a comma-separated list of Job IDs to include
* `groupPath`: specify a group or partial group path to include all jobs within that group path. (Default value: "*", all groups). Set to the special value "-" to match the top level jobs only
* `jobFilter`: specify a filter for the job Name. Matches any job name that contains this value.
* `jobExactFilter`: specify an exact job name to match.
* `groupPathExact`: specify an exact group path to match.  Set to the special value "-" to match the top level jobs only

Result:  An Item List of `jobs`. Each `job` is of the form:

~~~~~~~~~~ {.xml}
<job id="ID">
    <name>Job Name</name>
    <group>Job Name</group>
    <project>Project Name</project>
    <description>...</description>
</job>
~~~~~~~~~~

Note: If neither `groupPath` nor `groupPathExact` are specified, then the default `groupPath` value of "*" will be used (matching jobs in all groups).  `groupPathExact` cannot be combined with `groupPath`.  You can set either one to "-" to match only the top-level jobs which are not within a group.

### Running a Job

Run a job specified by ID.

URL:

    GET /api/1/job/[ID]/run
    POST /api/12/job/[ID]/executions

Optional parameters:

* `argString`: argument string to pass to the job, of the form: `-opt value -opt2 value ...`.
* `loglevel`: argument specifying the loglevel to use, one of: 'DEBUG','VERBOSE','INFO','WARN','ERROR'
* `asUser` : specifies a username identifying the user who ran the job. Requires `runAs` permission.
* Node filter parameters as described under [Using Node Filters](#using-node-filters)

Result:  An Item List of `executions` containing a single entry for the execution that was created.  See [Listing Running Executions](#listing-running-executions).

### Exporting Jobs

Export the job definitions for in XML or YAML formats.

URL:

    /api/1/jobs/export

Required parameters:

* `project`

Optional parameters:

* `format` : can be "xml" or "yaml" to specify the output format. Default is "xml"

The following parameters can also be used to narrow down the result set.

* `idlist`: specify a comma-separated list of Job IDs to export
* `groupPath`: specify a group or partial group path to include all jobs within that group path.
* `jobFilter`: specify a filter for the job Name

Result:

If you specify `format=xml`, then the output will be in [job-xml](../man5/job-xml.html) format.

If you specify `format=yaml`, then the output will be in [job-yaml](../man5/job-yaml.html) format.

If an error occurs, then the output will be in XML format, using the common `result` element described in the [Response Format](#response-format) section.

### Importing Jobs ###

Import job definitions in XML or YAML formats.

URL:

    /api/1/jobs/import

Method: `POST`

Expected Content-Type: `application/x-www-form-urlencoded` (**since 1.3**) or `multipart/form-data`

Required Content:

* `xmlBatch`: Either a `x-www-form-urlencoded` request parameter containing the input content (**since 1.3**), or a `multipart/form-data` multipart MIME request part containing the content.

Optional parameters:

* `format` : can be "xml" or "yaml" to specify the output format. Default is "xml"
* `dupeOption`: A value to indicate the behavior when importing jobs which already exist.  Value can be "skip", "create", or "update". Default is "create".
* `project` : (**since v8**) Specify the project that all job definitions should be imported to. If not specified, each job definition must define the project to import to.
* `uuidOption`: Whether to preserve or remove UUIDs from the imported jobs. Allowed values (**since V9**):
    *  `preserve`: Preserve the UUIDs in imported jobs.  This may cause the import to fail if the UUID is already used. (Default value).
    *  `remove`: Remove the UUIDs from imported jobs. Allows update/create to succeed without conflict on UUID.

Result:

A set of status results.  Each imported job definition will be either "succeeded", "failed" or "skipped".  These status sections contain a `count` attribute declaring how many jobs they contain.  Within each one there will be 0 or more `job` elements.

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
<job>
    <!-- ID may not exist if the job was not created yet -->
    <id>ID</id>
    <name>job name</name>
    <group>job group</group>
    <project>job project</project>
    <!--if within the failed section, then an error section will be included -->
    <error>Error message</error>
</job>
~~~~~~~~~~~~~~

### Getting a Job Definition ###

Export a single job definition in XML or YAML formats.

URL:

    /api/1/job/[ID]

Optional parameters:

* `format` : can be "xml" or "yaml" to specify the output format. Default is "xml"

Result:

If you specify `format=xml`, then the output will be in [job-xml](../man5/job-xml.html) format.

If you specify `format=yaml`, then the output will be in [job-yaml](../man5/job-yaml.html) format.

If an error occurs, then the output will be in XML format, using the common `result` element described in the [Response Format](#response-format) section.

### Deleting a Job Definition ###

Delete a single job definition.

URL:

    /api/1/job/[ID]

Method: `DELETE`

Result:

The common `result` element described in the [Response Format](#response-format) section, indicating success or failure and any messages.

If successful, then the `result` will contain a `success/message` element with the result message:

~~~~~~~~~~ {.xml}
<success>
   <message>Job was successfully deleted: ... </message>
</success>
~~~~~~~~~~

### Bulk Job Delete ###

Delete multiple job definitions at once.

URL:

    /api/5/jobs/delete

Method: `DELETE`, or `POST`

Query parameters:

* `ids`: The Job IDs to delete, can be specified multiple times
* `idlist`: The Job IDs to delete as a single comma-separated string.

Note: you can combine `ids` with `idlist`

Result:

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



### Getting Executions for a Job

Get the list of executions for a Job.

URL:

    /api/1/job/[ID]/executions

Optional Query Parameters:

* `status`: the status of executions you want to be returned.  Must be one of "succeeded", "failed", "aborted", or "running".  If this parameter is blank or unset, include all executions.
* Paging parameters:
    * `max`: indicate the maximum number of results to return. If unspecified, all results will be returned.
    * `offset`: indicate the 0-indexed offset for the first result to return.

Result: an Item List of `executions`.  See [Listing Running Executions](#listing-running-executions).


### Delete all Executions for a Job

Delete all executions for a Job.

Request:

    DELETE /api/12/job/[ID]/executions

Result: The same format as [Bulk Delete Executions](#bulk-delete-executions).

### Listing Running Executions

List the currently running executions for a project

URL:

    /api/1/executions/running

Required Parameters:

* `project`: the project name, or '*' for all projects (**Since API v9**)

Result: An Item List of `executions`.  Each `execution` of the form:

~~~~~~~~~~ {.xml}
<execution id="[ID]" href="[url]" status="[status]" project="[project]">
    <user>[user]</user>
    <date-started unixtime="[unixtime]">[datetime]</date-started>

    <!-- optional job context if the execution is associated with a job -->
    <job id="jobID" averageDuration="[milliseconds]">
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

The `[status]` value indicates the execution status.  It is one of:

* `running`: execution is running
* `succeeded`: execution completed successfully
* `failed`: execution completed with failure
* `aborted`: execution was aborted

The `[url]` value is a URL to the Rundeck server page to view the execution output.

`[user]` is the username of the user who started the execution.

`[unixtime]` is the millisecond unix timestamp, and `[datetime]` is a W3C dateTime string in the format "yyyy-MM-ddTHH:mm:ssZ".

If known, the average duration of the associated Job will be indicated (in milliseconds) as `averageDuration`. (Since API v5)

**API v9 and above**: `project="[project]"` is the project name of the execution.

`successfulNodes` and `failedNodes` list the names of nodes which succeeded or failed. **API v10 and above**.

The `job` section contains `options` if an `argstring` value is set (**API v10 and above**).  Inside `options` is a sequence of `<option>` elements with two attributes:

* `name` the parsed option name
* `value` the parsed option value


### Execution Info

Get the status for an execution by ID.

Request:

    GET /api/1/execution/[ID]

Result: an Item List of `executions` with a single item. See [Listing Running Executions](#listing-running-executions).

### Delete an Execution

Delete an execution by ID.

Request:

    DELETE /api/12/execution/[ID]

Result: `204 No Content`

*Authorization requirement*:

* Requires the `delete_execution` action allowed for a `project` in the `application` context. See: [Administration - Access Control Policy - Application Scope Resources and Actions](../administration/access-control-policy.html#application-scope-resources-and-actions)

### Bulk Delete Executions

Delete a set of Executions by their IDs.

Request:

    POST /api/12/executions/delete

The IDs can be specified in two ways:

1. Using a URL parameter `ids`, as a comma separated list, with no body content

        POST /api/12/executions/delete?ids=1,2,17
        Content-Length: 0

2. Using a request body of either XML or JSON data.

If using a request body, the formats are specified below:

**Content-Type: application/json**

    {"ids": [ 1, 2, 17 ] }

*OR* more simply:

    [ 1, 2, 17 ]

**Content-Type: application/xml**

    <executions>
        <execution id="1"/>
        <execution id="2"/>
        <execution id="17"/>
    </executions>

Response:

The response format will be either `xml` or `json`, depending on the `Accept` header.

**Content-Type: application/json**

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

The JSON fields will be:

* `failures`: a list of objects indicating the `id` and `message` for the failed deletion attempt
* `failedCount`: number of deletion attempts that failed
* `successCount`: number of deletion attempts that succeeded
* `allsuccessful`: true if all deletions were successful
* `requestCount`: number of requested execution deletions

**Content-Type: application/xml**

    <deleteExecutions requestCount='4' allsuccessful='false'>
      <successful count='0' />
      <failed count='4'>
        <execution id='131' message='Unauthorized: Delete execution 131' />
        <execution id='109' message='Not found: 109' />
        <execution id='81' message='Not found: 81' />
        <execution id='74' message='Not found: 74' />
      </failed>
    </deleteExecutions>

### Execution Query

Query for Executions based on Job or Execution details.

URL:

    /api/5/executions

Result: an Item List of `executions`. See [Listing Running Executions](#listing-running-executions).

The `executions` element will have paging attributes:

* `max`: maximum number of results per page
* `offset`: offset from first of all results
* `total`: total number of results
* `count`: number of results in the response

Required Parameters:

* `project`: the project name

The following parameters can also be used to narrow down the result set.

Parameters for Execution details:

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
        So a value of "2w" would return executions that completed within the last two weeks.
    * `begin`: Specify exact date for earliest execution completion time
    * `end`: Specify exact date for latest xecution completion time
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

### Execution State

Get detail about the node and step state of an execution by ID. The execution can be currently running or completed.

URL:

    /api/10/execution/[ID]/state

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

URL:

    /api/5/execution/[ID]/output
    /api/10/execution/[ID]/output/node/[NODE]
    /api/10/execution/[ID]/output/node/[NODE]/step/[STEPCTX]
    /api/10/execution/[ID]/output/step/[STEPCTX]

The log output for each execution is stored in a file on the Rundeck server, and this API endpoint allows you to retrieve some or all of the output, in several possible formats: json, XML, and plain text.  When retrieving the plain text output, some metadata about the log is included in HTTP Headers.  JSON and XML output formats include metadata about each output log line, as well as metadata about the state of the execution and log file, and your current index location in the file.

Output can be selected by Node or Step Context or both as of API v10.

Several parameters can be used to retrieve only part of the output log data.  You can use these parameters to more efficiently retrieve the log content over time while an execution is running.

The log file used to store the execution output is a formatted text file which also contains metadata about each line of log output emitted during an execution.  Several data values in this API endpoint refer to "bytes", but these do not reflect the size of the final log data; they are only relative to the formatted log file itself.  You can treat these byte values as opaque locations in the log file, but you should not try to correlate them to the actual textual log lines.

Optional Parameters:

* `offset`: byte offset to read from in the file. 0 indicates the beginning.
* `lastlines`: number of lines to retrieve from the end of the available output. If specified it will override the `offset` value and return only the specified number of lines at the end of the log.
* `lastmod`: epoch datestamp in milliseconds, return results only if modification changed since the specified date OR if more data is available at the given `offset`
* `maxlines`: maximum number of lines to retrieve forward from the specified offset.

Result: The output content in the requested format.

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

URL:

    /api/10/execution/[ID]/output/state
    /api/10/execution/[ID]/output/state?stateOnly=true

This API endpoint provides the sequential log of state changes for steps and nodes, optionally interleaved with the actual log output.

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

URL:

    /api/1/execution/[ID]/abort

Optional Parameters:

* `asUser` : specifies a username identifying the user who aborted the execution. Requires `runAs` permission.

Result:  The result will contain a `success/message` element will contain a descriptive message.  The status of the abort action will be included as an element:

~~~~~~~~~~ {.xml}
    <abort status="[abort-state]">
        <execution id="[id]" status="[status]"/>
    </abort>
~~~~~~~~~~

The `[abort-state]` will be one of: "pending", "failed", or "aborted".

### Running Adhoc Commands

Run a command string.

URL:

    /api/1/run/command

Required Parameters:

* `project`: the project name
* `exec`: the shell command string to run, e.g. "echo hello".

Optional Parameters:

* `nodeThreadcount`: threadcount to use
* `nodeKeepgoing`: if "true", continue executing on other nodes even if some fail.
* `asUser` : specifies a username identifying the user who ran the command. Requires `runAs` permission.

Node filter parameters as described under [Using Node Filters](#using-node-filters)

Result: A success message, and a single `<execution>` item identifying the
new execution by ID:

~~~~~~~~~~ {.xml}
    <execution id="X"/>
~~~~~~~~~~

### Running Adhoc Scripts

Run a script.

URL:

    /api/1/run/script

Method: `POST`

Required Parameters:

* `project`: the project name

Required Content:

The script file content can be submitted either as a form request or multipart attachment.

For Content-Type: `application/x-www-form-urlencoded`

* `scriptFile`: A `x-www-form-urlencoded` request parameter containing the script file content.

For Content-Type: `multipart/form-data`

* `scriptFile`: the script file contents (`scriptFile` being the `name` attribute of the `Content-Disposition` header)

Optional Parameters:

* `argString`: Arguments to pass to the script when executed.
* `nodeThreadcount`: threadcount to use
* `nodeKeepgoing`: if "true", continue executing on other nodes even if some fail.
* `asUser` : specifies a username identifying the user who ran the script. Requires `runAs` permission.
* `scriptInterpreter`: a command to use to run the script (*since version 8*)
* `interpreterArgsQuoted`: `true`/`false`: if true, the script file and arguments will be quoted as the last argument to the `scriptInterpreter` (*since version 8*)

Node filter parameters as described under [Using Node Filters](#using-node-filters)

Result: A success message, and a single `<execution>` item identifying the
new execution by ID:

    <execution id="X"/>

**Since API version 8**: The script interpreter and whether the arguments to the interpreter are quoted can be specified.

### Running Adhoc Script URLs

Run a script downloaded from a URL.  (**API version 4** required.)

URL:

    /api/4/run/url

Method: `POST`

Expected Content-Type: `application/x-www-form-urlencoded`

Required Parameters:

* `project`: the project name

Required Content:

* `scriptURL`: A URL pointing to a script file

Optional Parameters:

* `argString`: Arguments to pass to the script when executed.
* `nodeThreadcount`: threadcount to use
* `nodeKeepgoing`: if "true", continue executing on other nodes even if some fail.
* `asUser` : specifies a username identifying the user who ran the script. Requires `runAs` permission.
* `scriptInterpreter`: a command to use to run the script (*since version 8*)
* `interpreterArgsQuoted`: `true`/`false`: if true, the script file and arguments will be quoted as the last argument to the `scriptInterpreter` (*since version 8*)

Node filter parameters as described under [Using Node Filters](#using-node-filters)

Result: A success message, and a single `<execution>` item identifying the
new execution by ID:

~~~~~~~~~~ {.xml}
    <execution id="X"/>
~~~~~~~~~~

**Since API version 8**: The script interpreter and whether the arguments to the interpreter are quoted can be specified.

### Key Storage ###

Upload and manage public and private key files. For more information see the [Administration - Key Storage](../administration/key-storage.html) document.

Keys are stored via Rundeck's *Storage* facility.  This is a path-based interface to manage files.  The underlying storage may be on disk or in a database.

The Storage facility manages "resources", which may be files or directories.  File resources can have metadata associated with them (such as MIME content type).

Note: Private Keys can be uploaded but not retrieved directly with this API.  They can only be used internally by Rundeck.

URL:

    /api/11/storage/keys/[PATH]/[FILE]

#### Upload Keys ####

Specify the type of key via the `Content-type` header:

* `application/octet-stream` specifies a **private key**
* `application/pgp-keys` specifies a **public key**

Use `POST` to create a new file, or `PUT` to modify an existing file.

~~~
POST /api/11/storage/keys/[PATH]/[FILE]
Content-Type: [...]
~~~

~~~
PUT /api/11/storage/keys/[PATH]/[FILE]
Content-Type: [...]
~~~

#### List keys ####

Lists resources at the specified PATH, provides a JSON or XML response based on the `Accept` request header.

Each resource has a type of `file` or `directory`.

`GET /api/11/storage/keys/[PATH]/`

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


#### Get Key Metadata ####

Returns the metadata about the stored key file.

Provides a JSON or XML response based on the `Accept` request header:

~~~
GET /api/11/storage/keys/[PATH]/[FILE]
~~~

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

#### GET Key Contents ####

Provides the **public key** content if the `Accept` request header matches `*/*` or `application/pgp-keys`:

`GET /api/11/storage/keys/[PATH]/[FILE]`

**Retrieving private key file contents is not allowed.**

A GET request for a private key file if the `Accept` request header matches `*/*` or `application/octet-stream` will result in a `403 Unauthorized` response.

    GET /api/11/storage/keys/[PATH]/[FILE]
    Accept: application/octet-stream
    ...

Response:

    403 Unauthorized
    ...

#### Delete Keys ####

Deletes the file if it exists and returns `204` response.

`DELETE /api/11/storage/keys/[PATH]/[FILE]`


### Listing Projects ###

List the existing projects on the server.

URL:

    /api/1/projects

Result:  An Item List of `projects`, each `project` of the form specified in the [Getting Project Info](#getting-project-info) section.

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

    GET /api/1/project/[NAME]

Result:  An Item List of `projects` with one `project`.  XML or JSON is determined by the `Accept` request header. The `project` is of the form:

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

    GET /api/11/project/[NAME]


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

    DELETE /api/11/project/[NAME]

Response:

    204 No Content

### Project Configuration ###

Retrieve or modify the project configuration data.  Requires `configure` authorization for the project.

#### GET Project Configuration ####

`GET /api/11/project/[NAME]/config`

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

`PUT /api/11/project/[NAME]/config`

Request:

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

    /api/11/project/[NAME]/config/[KEY]

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

`GET /api/11/project/[NAME]/config/[KEY]`

#### PUT Project Configuration Key ####

Set the value.

`PUT /api/11/project/[NAME]/config/[KEY]`

#### DELETE Project Configuration Key ####

Delete the key.

`DELETE /api/11/project/[NAME]/config/[KEY]`

Response will be

    204 No Content

### Project Archive Export ###

Export a zip archive of the project.  Requires `export` authorization for the project.

    GET /api/11/project/[NAME]/export

Response content type is `application/zip`

### Project Archive Import ###

Import a zip archive to the project. Requires `import` authorization for the project.

    PUT /api/11/project/[NAME]/import{?jobUuidOption,importExecutions}

Parameters:

+ `jobUuidOption` (optional, string, `preserve/remove`) ... Option declaring how duplicate Job UUIDs should be handled. If `preserve` (default) then imported job UUIDs will not be modified, and may conflict with jobs in other projects. If `remove` then all job UUIDs will be removed before importing.
+ `importExecutions` (optional, string, `true/false`) ... If true, import all executions and logs from the archive (default). If false, do not import executions or logs.

Expected Request Content:

`Content-Type: application/zip`

Response will indicate whether the imported contents had any errors:

**All imported jobs successful:**

`application/xml`

~~~ {.xml}
<import status="successful">
</import>
~~~

`application/json`

~~~ {.json}
{"import_status":"successful"}
~~~

**Some imported jobs failed:**

`application/xml`

~~~ {.xml}
<import status="failed">
    <errors count="[#]">
        <error>Job ABC could not be validated: ...</error>
        <error>Job XYZ could not be validated: ...</error>
    </errors>
</import>
~~~

`application/json`

~~~ {.json}
{
    "import_status":"failed",
    "errors": [
        "Job ABC could not be validated: ...",
        "Job XYZ could not be validated: ..."
    ]
}
~~~

### Updating and Listing Resources for a Project

Update or retrieve the Resources for a project.  A GET request returns the resources
for the project, and a POST request will update the resources. (**API version 2** required.)

URL:

    /api/2/project/[NAME]/resources

Method: POST, GET

#### POST request

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

Result:

    200 OK

    <result success="true">
        <success>
            <message>Resources were successfully updated for project test</message>
        </success>
    </result>

#### GET request

Optional GET Parameters:

* `format` : Result format.  One of "xml" or "yaml". Default is "xml".
*  Query
parameters can also be used. This is an alternate interface to [Listing Resources](#listing-resources).

GET Result: Depending on the `format` parameter, a value of "xml" will return [resource-xml](../man5/resource-xml.html) and "yaml" will return [resource-yaml](../man5/resource-yaml.html) formatted results.

Example GET request:

    GET /api/2/project/test/resources

Response:

    200 OK
    Content-Type: text/xml

    <project>
        <node name="node1" hostname="node1" username="bob" />
        <node name="node2" hostname="node2" username="bob" />
    </project>

### Refreshing Resources for a Project

Refresh the resources for a project via its Resource Model Provider URL. The URL can be
specified as a request parameter, or the pre-configured URL for the project
can be used. (**API version 2** required.)

URL:

    /api/2/project/[NAME]/resources/refresh

Method: POST

Optional Parameters:

`providerURL`

:   Specify the Resource Model Provider URL to refresh the resources from.  If
    not specified then the configured provider URL in the `project.properties`
    file will be used.

Result: A success or failure result with a message.

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


### Listing History

List the event history for a project.

URL:

    /api/1/history

Required Parameters:

* `project` : project name

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

Result:  an Item List of `events`.  Each `event` has this form:

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
  <execution id="[execid]"/>
  <!-- if associated with a Job, include the Job ID: -->
  <job id="[jobid]"/>
</event>
~~~~~~~~~~

The `events` element will also have `max`, `offset`, and `total` attributes, to indicate the state of paged results.  E.G:

    <events count="8" total="268" max="20" offset="260">
    ...
    </events>

`total` is the total number of events matching the query parameters.
`count` is the number of events included in the results.
`max` is the paging size as specified in the request, or with the default value of 20.
`offset` is the offset specified, or default value of 0.

### Listing Resources

List or query the resources for a project.

URL:

    /api/1/resources

Required Parameters:

* `project` : project name

Optional Parameters:

* `format` : Result format.  
    * for **API Version 2 or earlier**: One of "xml" or "yaml".
    * for **API Version 3**: any supported Resource Format Parser format name.
    * Default is "xml".

* Node Filter parameters: You can select resources to include and exclude in the result set, see [Using Node Filters](#using-node-filters) below.

**Note:** If no query parameters are included, the result set will include all Node resources for the project.

Result: Depending on the `format` parameter, a value of "xml" will return [resource-xml](../man5/resource-xml.html) and "yaml" will return [resource-yaml](../man5/resource-yaml.html) formatted results.  Any other supported format value will return content in the specified format.

#### Using Node Filters

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

### Getting Resource Info

Get a specific resource within a project.

URL:

    /api/1/resource/[NAME]

Required Parameters:

* `project` : project name

Optional Parameters:

* `format` : Result format.  One of "xml" or "yaml". Default is "xml".

Result: Depending on the `format` parameter, a value of "xml" will return [resource-xml](../man5/resource-xml.html) and "yaml" will return [resource-yaml](../man5/resource-yaml.html) formatted results.

The result will contain a single item for the specified resource.

### Takeover Schedule in Cluster Mode

**INCUBATOR**: this endpoint is available under the `/incubator` top-level path to indicate it is under development, and the specific behavior may change before it is finalized, or even completely removed.

Tell a Rundeck server in cluster mode to claim all scheduled jobs from another cluster server.

URL:

    /api/7/incubator/jobs/takeoverSchedule

HTTP Method:

    PUT

Required Content:

One of the following:

* XML content:

        <server uuid="[UUID]"/>

* JSON content:

        { server: { uuid: "[UUID]" } }

Result:

If request was XML, then Standard API response containing the following additional elements:

* `self`
    * `server`
        *  `@uuid` - this cluster server's uuid
*  `takeoverSchedule`
    *  `server`
        *  `@uuid` - requested server uuid to take over
    *  `jobs` - set of successful and failed jobs taken over
        *  `successful`/`failed` - job set
            *  `@count` number of jobs in the set
            *  `job` - one element for each job
                *  `@id` Job ID
                *  `@href` Job HREF

Example XML Response:

~~~~~~~~~~ {.xml}
<result success='true' apiversion='7'>
  <message>Schedule Takeover successful for 2/2 Jobs.</message>
  <self>
    <server uuid='C677C663-F902-4B97-B8AC-4AA57B58DDD6' />
  </self>
  <takeoverSchedule>
    <server uuid='8F3D5976-2232-4529-847B-8E45764608E3' />
    <jobs total='2'>
      <successful count='2'>
        <job id='a1aa53ac-73a6-4ead-bbe4-34afbff8e057'
        href='http://localhost:9090/rundeck/job/show/a1aa53ac-73a6-4ead-bbe4-34afbff8e057' />
        <job id='116e2025-7895-444a-88f7-d96b4f19fdb3'
        href='http://localhost:9090/rundeck/job/show/116e2025-7895-444a-88f7-d96b4f19fdb3' />
      </successful>
      <failed count='0'></failed>
    </jobs>
  </takeoverSchedule>
</result>
~~~~~~~~~~

If request was JSON, then the following JSON:

~~~~~~~~~~ {.json}
    {
      "takeoverSchedule": {
        "jobs": {
          "failed": [],
          "successful": [
            {
              "href": "http://dignan:4440/job/show/a1aa53ac-73a6-4ead-bbe4-34afbff8e057",
              "id": "a1aa53ac-73a6-4ead-bbe4-34afbff8e057"
            },
            {
              "href": "http://dignan:4440/job/show/116e2025-7895-444a-88f7-d96b4f19fdb3",
              "id": "116e2025-7895-444a-88f7-d96b4f19fdb3"
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
      "apiversion": 7,
      "success": true
    }
~~~~~~~~~~
