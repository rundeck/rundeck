% API Reference | Version ${APIVERS}
% Alex Honor; Greg Schueler
% November 20, 2010

Rundeck provides a Web API for use with your application.  

API Version Number
----

The current API version is `5`.

For API endpoints described in this document, the *minimum* API version required for their
use is indicated by the URL used, e.g.:

    /api/2/system/info
    /api/1/projects

This means you must use at least the API version indicated to access the
endpoint, unless otherwise noted. Some features or functionality for the 
endpoint may only be supported in later versions. 

The API Version Number is required to be included in all API calls within the URL.

If the version number is not included or if the requested version number is unsupported, then the API call will fail.  The error response will include the code "api-version-unsupported" and have HTTP status code of `400 Bad Request`:

    <result error="true" apiversion="2">
        <error code="api-version-unsupported">
            <message>
            Unsupported API Version "1". API Request: /rundeck/api/1/project/test/jobs. Reason: Minimum supported version: 2
            </message>
        </error>
    </result>

### Changes

Changes introduced by API Version number:

**Version 5**:

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

XML
----

The majority of API calls require XML for input and output.  Some import/export features support YAML formatted documents, but XML is used for all API-level information.  (JSON support is planned for a future API version.)

Authentication
-----

Authentication can be done in two different ways, either with Token based authentication,
or with a username and password.

Note that in either case, **it is recommended that you enable SSL Support for the Rundeck server** so that communication is encrypted at all times. For more information about using SSL, see [Administration - Configuring Rundeck for SSL](../administration/ssl.html).

### Token Authentication

Token Authentication consists of including a string known as an "API Token" with every
request to the Rundeck API.

To obtain an API Token, you must first log in to the Rundeck GUI using a user account
that has "admin" credentials. Click on your username in the header of the page, and you will be shown your User Profile page.  From this page you can manage your API Tokens.  Click "Generate API Token" to create a new one.  The unique string that is shown is the API Token.

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

The XML Response format will conform to this document structure:

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

### System Info ###

Get Rundeck server information and stats.

URL:

    /api/1/system/info

Parameters: none

Result: Success response, with included system info and stats in this format:

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
    </system>

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

    <job id="ID">
        <name>Job Name</name>
        <group>Job Name</group>
        <project>Project Name</project>
        <description>...</description>
    </job>

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

    <job id="ID">
        <name>Job Name</name>
        <group>Job Name</group>
        <project>Project Name</project>
        <description>...</description>
    </job>

Note: If neither `groupPath` nor `groupPathExact` are specified, then the default `groupPath` value of "*" will be used (matching jobs in all groups).  `groupPathExact` cannot be combined with `groupPath`.  You can set either one to "-" to match only the top-level jobs which are not within a group.

### Running a Job

Run a job specified by ID.

URL:
    
    /api/1/job/[ID]/run

Optional parameters:

* `argString`: argument string to pass to the job, of the form: `-opt value -opt2 value ...`.
* `loglevel`: argument specifying the loglevel to use, one of: 'DEBUG','VERBOSE','INFO','WARN','ERR'
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

If you specify `format=xml`, then the output will be in [jobs-v20(5)](jobs-v20.html) format.

If you specify `format=yaml`, then the output will be in [jobs-v20-yaml(5)](jobs-v20-yaml.html) format.

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

Result:

A set of status results.  Each imported job definition will be either "succeeded", "failed" or "skipped".  These status sections contain a `count` attribute declaring how many jobs they contain.  Within each one there will be 0 or more `job` elements. 

    <succeeded count="x">
        <!-- job elements -->
    </succeeded>
    <failed count="x">
        <!-- job elements -->
    </failed>
    <skipped count="x">
        <!-- job elements -->
    </skipped>

Each Job element will be of the form:

    <job>
        <!-- ID may not exist if the job was not created yet -->
        <id>ID</id>
        <name>job name</name>
        <group>job group</group>
        <project>job project</project>
        <!--if within the failed section, then an error section will be included -->
        <error>Error message</error> 
    </job>

### Getting a Job Definition ###

Export a single job definition in XML or YAML formats.

URL:

    /api/1/job/[ID]

Optional parameters:

* `format` : can be "xml" or "yaml" to specify the output format. Default is "xml"

Result:

If you specify `format=xml`, then the output will be in [jobs-v20(5)](jobs-v20.html) format.

If you specify `format=yaml`, then the output will be in [jobs-v20-yaml(5)](jobs-v20-yaml.html) format.

If an error occurs, then the output will be in XML format, using the common `result` element described in the [Response Format](#response-format) section.

### Deleting a Job Definition ###

Delete a single job definition.

URL:

    /api/1/job/[ID]

Method: `DELETE`

Result:

The common `result` element described in the [Response Format](#response-format) section, indicating success or failure and any messages.

If successful, then the `result` will contain a `success/message` element with the result message:

    <success>
    <message>Job was successfully deleted: ... </message>
    </success>

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

### Listing Running Executions

List the currently running executions for a project

URL:

    /api/1/executions/running

Required Parameters:

* `project`: the project name

Result: An Item List of `executions`.  Each `execution` of the form:

    <execution id="[ID]" href="[url]" status="[status]">
        <user>[user]</user>
        <date-started unixtime="[unixtime]">[datetime]</date-started>
        
        <!-- optional job context if the execution is associated with a job -->
        <job id="jobID" averageDuration="[milliseconds]">
            <name>..</name>
            <group>..</group>
            <description>..</description>
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
        
    </execution>

The `[status]` value indicates the execution status.  It is one of:

* `running`: execution is running
* `succeeded`: execution completed successfully
* `failed`: execution completed with failure
* `aborted`: execution was aborted

The `[url]` value is a URL to the Rundeck server page to view the execution output.

`[user]` is the username of the user who started the execution.

`[unixtime]` is the millisecond unix timestamp, and `[datetime]` is a W3C dateTime string in the format "yyyy-MM-ddTHH:mm:ssZ".

If known, the average duration of the associated Job will be indicated (in milliseconds) as `averageDuration`. (Since API v5)

### Getting Execution Info

Get the status for an execution by ID.

URL:

    /api/1/execution/[ID]

Result: an Item List of `executions` with a single item. See [Listing Running Executions](#listing-running-executions).

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

### Execution Output

Get the output for an execution by ID.  The execution can be currently running or may have already completed.

URL:

    /api/5/execution/[ID]/output

The log output for each execution is stored in a file on the Rundeck server, and this API endpoint allows you to retrieve some or all of the output, in several possible formats: json, XML, and plain text.  When retrieving the plain text output, some metadata about the log is included in HTTP Headers.  JSON and XML output formats include metadata about each output log line, as well as metadata about the state of the execution and log file, and your current index location in the file.

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

Each log entry will be included in a section called `entries`.

* In JSON, `entries` will contain an array of Objects, each containing the following format
* In XML, the `entries` element will contain a sequence of `entry` elements

Content of each Log Entry:

* `time`: Timestamp in format: "HH:MM:SS"
* `level`: Log level, one of: SEVERE,WARNING,INFO,CONFIG,FINEST
* `mesg`: The log message (JSON only)
* `user`: User name
* `command`: Workflow command context string
* `node`: Node name

The XML `entry` will have the log message as the text value.

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

### Aborting Executions

Abort a running execution by ID.

URL:

    /api/1/execution/[ID]/abort

Result:  The result will contain a `success/message` element will contain a descriptive message.  The status of the abort action will be included as an element:

    <abort status="[abort-state]">
        <execution id="[id]" status="[status]"/>
    </abort>

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

Node filter parameters as described under [Using Node Filters](#using-node-filters)

Result: A success message, and a single `<execution>` item identifying the
new execution by ID:

    <execution id="X"/>

### Running Adhoc Scripts

Run a script.

URL:

    /api/1/run/script

Method: `POST`

Expected Content-Type: `application/x-www-form-urlencoded`

Required Parameters:

* `project`: the project name

Required Content:

* `scriptFile`: A `x-www-form-urlencoded` request parameter containing the script file content.

Optional Parameters:

* `argString`: Arguments to pass to the script when executed.
* `nodeThreadcount`: threadcount to use
* `nodeKeepgoing`: if "true", continue executing on other nodes even if some fail.

Node filter parameters as described under [Using Node Filters](#using-node-filters)

Result: A success message, and a single `<execution>` item identifying the
new execution by ID:

    <execution id="X"/>

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

Node filter parameters as described under [Using Node Filters](#using-node-filters)

Result: A success message, and a single `<execution>` item identifying the
new execution by ID:

    <execution id="X"/>

### Listing Projects ###

List the existing projects on the server.

URL:

    /api/1/projects

Result:  An Item List of `projects`, each `project` of the form specified in the [Getting Project Info](#getting-project-info) section.

### Getting Project Info ###

Get information about a project.

URL:

    /api/1/project/[NAME]

Result:  An Item List of `projects` with one `project`.  The `project` is of the form:

    <project>
        <name>Project Name</name>
        <description>...</description>
        <!-- additional items -->
    </project>

If the project defines a Resource Model Provider URL, then the additional items are:

    <resources>
        <providerURL>URL</providerURL>
    </resources>

### Updating and Listing Resources for a Project

Update or retrieve the Resources for a project.  A GET request returns the resources
for the project, and a POST request will update the resources. (**API version 2** required.)

URL:

    /api/2/project/[NAME]/resources

Method: POST, GET

#### POST request

POSTing to this URL will set the resources for the project to the content of the request.

Expected POST Content: For API version 2: either `text/xml` or `text/yaml` Content-Type containing the 
Resource Model definition in [resources-v10(5)](resources-v10.html) or [resources-v10-yaml(5)](resources-v10-yaml.html) formats as the request body. (Note: any MIME type ending with '/yaml' or '/x-yaml' or '/xml' will be accepted).

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

GET Result: Depending on the `format` parameter, a value of "xml" will return [resources-v10(5)](resources-v10.html) and "yaml" will return [resources-v10-yaml(5)](resources-v10-yaml.html) formatted results.

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

The `events` element will also have `max`, `offset`, and `total` attributes, to indicate the state of paged results.  E.G:

    <events count="8" total="268" max="20" offset="260">
    ...
    </events>

`total` is the total number of events matching the query parameters.
`count` is the number of events included in the results.
`max` is the paging size as specified in the request, or with the default value of 20.
`offset` is the offset specified, or default value of 0.

### Creating History Event Reports

Create a history event report for any external process.

URL:

    /api/1/report/create

Required Parameters:

* `project` : project name
* `status` : report status, one of "succeeded", "failed" or "aborted"
* `title` : Title of the report. This should be a short string identifying the process that was run.  E.g. Rundeck jobs use the Job Group and Job Name, and adhoc commands use the string "adhoc".
* `summary` : A descriptive summary of the result.
* `nodesuccesscount`:  number of successful nodes: how many nodes the process succeeded on.
* `nodefailedcount`: number of failed nodes: how many nodes the process failed on.

Optional Parameters:

* `start`: Specify exact date for beginning of the process (defaults to the same as end).
* `end`: Specify exact date for end of the process (defaults to time the report is received).
* `script`: Full adhoc command or script that was run, if applicable.
* `jobID`: Any associated Rundeck Job ID
* `execID`: Any associated Rundeck Execution ID.

The format for the `end`, and `start` values is either:  a unix millisecond timestamp, or a W3C dateTime string in the format "yyyy-MM-ddTHH:mm:ssZ".

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

Result: Depending on the `format` parameter, a value of "xml" will return [resources-v10(5)](resources-v10.html) and "yaml" will return [resources-v10-yaml(5)](resources-v10-yaml.html) formatted results.  Any other supported format value will return content in the specified format.

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

Result: Depending on the `format` parameter, a value of "xml" will return [resources-v10(5)](resources-v10.html) and "yaml" will return [resources-v10-yaml(5)](resources-v10-yaml.html) formatted results.

The result will contain a single item for the specified resource.
