RunDeck API
===========

RunDeck provides a Web API for use with your application.  This describes the RunDeck API version `1`.

URLs
----

The RunDeck server has a "Base URL", where you access the server. Your RunDeck Server URL may look like: `http://myserver:4440`.

The root URL path for all calls to the API in this version is:

    $RUNDECK_SERVER_URL/api/1

In this document we will leave off the `$RUNDECK_SERVER_URL/api/1` and simply display URLs as `/...`.

XML
----

The majority of API calls require XML for input and output.  Some import/export features support YAML formatted documents, but XML is used for all API-level information.  (JSON support is planned for a future API version.)

Authentication
-----

Authentication is required prior to access to the API.  This means that you must submit authentication parameters (username, password) to the "Authentication URL" and retain a Session Cookie. 

The Session Cookie must be sent with all calls to the API to maintain the authenticated connection.

To submit authentication, submit a `POST` request to the URL:
    
    $RUNDECK_SERVER_URL/j_security_check

With these parameters:

* `j_username`: rundeck username
* `j_password`: password

If the response includes a redirect chain which includes or results in `$RUNDECK_SERVER_URL/user/login` or `$RUNDECK_SERVER_URL/user/error`, then the authentication request failed.

Otherwise, if the response is a redirect chain which results in `200 successful` response,  then the authentication was successful.  

The response should set a cookie named `JSESSIONID`.

API Version Number
------

The API Version Number is required to be included in all API calls within the URL.

If the version number is not included or if the requested version number is unsupported, then the API call will fail.


Response Format
------

The XML Response format will conform to this document structure:

    <result success/error="true">
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

### Listing Jobs ###

List the jobs that exist for a project.

URL:

    /jobs

Required parameters:

* `project`: the project name

The following parameters can also be used to narrow down the result set.

* `idlist`: specify a comma-separated list of Job IDs to include
* `groupPath`: specify a group or partial group path to include all jobs within that group path.
* `jobFilter`: specify a filter for the job Name

Result:  An Item List of `jobs`. Each `job` is of the form:

    <job id="ID">
        <name>Job Name</name>
        <group>Job Name</group>
        <project>Project Name</project>
        <description>...</description>
    </job>

### Running a Job

Run a job specified by ID.

URL:
    
    /job/[ID]/run

Optional parameters:

* `argString`: argument string to pass to the job, of the form: `-opt value -opt2 value ...`.
* Node filter parameters as described under [Using Node Filters](#using-node-filters)

Result:  An Item List of `executions` containing a single entry for the execution that was created.  See [Listing Running Executions](#listing-running-executions).

### Exporting Jobs

Export the job definitions for in XML or YAML formats.

URL:

    /jobs/export

Required parameters:

* `project`

Optional parameters:

* `format` : can be "xml" or "yaml" to specify the output format. Default is "xml"

The following parameters can also be used to narrow down the result set.

* `idlist`: specify a comma-separated list of Job IDs to export
* `groupPath`: specify a group or partial group path to include all jbos within that group path.
* `jobFilter`: specify a filter for the job Name

Result:

If you specify `format=xml`, then the output will be in [jobs-v20(5)](jobs-v20.html) format.

If you specify `format=yaml`, then the output will be in [jobs-v20-yaml(5)](jobs-v20-yaml.html) format.

If an error occurs, then the output will be in XML format, using the common `result` element described in the [Response Format](#response-format) section.

### Importing Jobs ###

Import job definitions in XML or YAML formats.

URL:

    /jobs/import

Method: `POST`

Expected Content-Type: `application/x-www-form-urlencoded`

Required Content:

* `xmlBatch`: A `x-www-form-urlencoded` request parameter containing the input file content.

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

    /job/[ID]

Optional parameters:

* `format` : can be "xml" or "yaml" to specify the output format. Default is "xml"

Result:

If you specify `format=xml`, then the output will be in [jobs-v20(5)](jobs-v20.html) format.

If you specify `format=yaml`, then the output will be in [jobs-v20-yaml(5)](jobs-v20-yaml.html) format.

If an error occurs, then the output will be in XML format, using the common `result` element described in the [Response Format](#response-format) section.

### Deleting a Job Definition ###

Delete a single job definition.

URL:

    /job/[ID]

Method: `DELETE`

Result:

The common `result` element described in the [Response Format](#response-format) section, indicating success or failure and any messages.

If successful, then the `result` will contain a `success/message` element with the result message:

    <success>
    <message>Job was successfully deleted: ... </message>
    </success>

### Listing Running Executions

List the currently running executions for a project

URL:

    /executions/running

Required Parameters:

* `project`: the project name

Result: An Item List of `executions`.  Each `execution` of the form:

    <execution id="[ID]" href="[url]" status="[status]">
        <user>[user]</user>
        <date-started unixtime="[unixtime]">[datetime]</date-started>
        
        <!-- optional job context if the execution is associated with a job -->
        <job id="jobID">
            <name>..</name>
            <group>..</group>
            <description>..</description>
        </job>
        
        <!-- description of the execution -->
        <description>...</description>
               
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

The `[url]` value is a URL to the RunDeck server page to view the execution output.

`[user]` is the username of the user who started the execution.

`[unixtime]` is the millisecond unix timestamp, and `[datetime]` is a W3C dateTime string in the format "yyyy-MM-ddTHH:mm:ssZ".

### Getting Execution Info

Get the status for an execution by ID.

URL:

    /execution/[ID]

Result: an Item List of `executions` with a single item. See [Running Executions](#running-executions).


### Aborting Executions

Abort a running execution by ID.

URL:

    /execution/[ID]/abort

Result:  The result will contain a `success/message` element will contain a descriptive message.  The status of the abort action will be included as an element:

    <abort status="[abort-state]">
        <execution id="[id]" status="[status]"/>
    </abort>

The `[abort-state]` will be one of: "pending", "failed", or "aborted".

### Running Adhoc Commands

Run a command string.

URL:

    /run/command

Required Parameters:

* `project`: the project name
* `exec`: the shell command string to run, e.g. "echo hello".

Optional Parameters:

* `nodeThreadcount`: threadcount to use
* `nodeKeepgoing`: if "true", continue executing on other nodes even if some fail.

Node filter parameters as described under [Using Node Filters](#using-node-filters)

### Running Adhoc Scripts

Run a script.

URL:

    /run/script

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

### Listing Projects ###

List the existing projects on the server.

URL:

    /projects

Result:  An Item List of `projects`, each `project` of the form specified in the [Getting Project Info](#getting-project-info) section.

### Getting Project Info ###

Get information about a project.

URL:

    /project/NAME

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

### Listing History

List the event history for a project.

URL:

    /history

Required Parameters:

* `project` : project name

Optional Parameters:

* History query parameters:
    * `jobIdFilter`: include events for a job ID.
    * `reportIdFilter`: include events for an event Name.
    * `userFilter`: include events created by a user
    * `statFilter`: include events based on result status.  this can be 'succeed','fail', or 'cancel'.
* Date query parameters:
    * `recentFilter`: Use a simple text format to filter events that occured within a period of time. The format is "XY" where X is an integer, and "Y" is one of:
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

    /report/create

Required Parameters:

* `project` : project name
* `status` : report status, one of "succeeded", "failed" or "aborted"
* `title` : Title of the report. This should be a short string identifying the process that was run.  E.g. RunDeck jobs use the Job Group and Job Name, and adhoc commands use the string "adhoc".
* `summary` : A descriptive summary of the result.
* `nodesuccesscount`:  number of successful nodes: how many nodes the process succeeded on.
* `nodefailedcount`: number of failed nodes: how many nodes the process failed on.

Optional Parameters:

* `start`: Specify exact date for beginning of the process (defaults to the same as end).
* `end`: Specify exact date for end of the process (defaults to time the report is received).
* `script`: Full adhoc command or script that was run, if applicable.
* `jobID`: Any associated RunDeck Job ID
* `execID`: Any associated RunDeck Execution ID.

The format for the `end`, and `start` values is either:  a unix millisecond timestamp, or a W3C dateTime string in the format "yyyy-MM-ddTHH:mm:ssZ".

### Listing Resources

List or query the resources for a project.

URL:

    /resources

Required Parameters:

* `project` : project name

Optional Parameters:

* `format` : Result format.  One of "xml" or "yaml". Default is "xml".

* Node Filter parameters: You can select resources to include and exclude in the result set, see [Using Node Filters](#using-node-filters) below.

**Note:** If no query parameters are included, the result set will include all Node resources for the project.

Result: Depending on the `format` parameter, a value of "xml" will return [resources-v10(5)](resources-v10.html) and "yaml" will return [resources-v10-yaml(5)](resources-v10-yaml.html) formatted results.

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

    /resource/[name]

Required Parameters:

* `project` : project name

Optional Parameters:

* `format` : Result format.  One of "xml" or "yaml". Default is "xml".

Result: Depending on the `format` parameter, a value of "xml" will return [resources-v10(5)](resources-v10.html) and "yaml" will return [resources-v10-yaml(5)](resources-v10-yaml.html) formatted results.

The result will contain a single item for the specified resource.