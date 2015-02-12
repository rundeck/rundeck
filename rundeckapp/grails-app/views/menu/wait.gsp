<%--
  Created by IntelliJ IDEA.
  User: greg
  Date: 2/11/15
  Time: 3:40 PM
--%>

<%@ page contentType="text/html;charset=UTF-8" %>
<html>
<head>
    <meta name="layout" content="base"/>
    <meta name="tabpage" content="configure"/>
    <title><g:message code="Wait" default="Wait"/></title>
    <asset:javascript src="knockout.min.js"/>
    <asset:javascript src="knockout-mapping.js"/>
    <g:embedJSON data="${[
            ready:ready,
            token:token,
            refresh:true,
            notFound:notFound,
            errorMessage:requestError?.message,
            percentage:percentage,
            url:createLink(controller: 'project', action: 'exportWait', params: [project: params.project, token: params.token, format: 'json'])
    ]}" id="requestdata"/>
    <g:javascript>
        var request = {
            ready: ko.observable(false),
            notFound: ko.observable(false),
            errorMessage: ko.observable(null),
            refresh: ko.observable(false),
            percentage:ko.observable(0),
            token: null,
            timeout: null,
            url: null,
            refreshData: function () {
                jQuery.ajax(request.url, {
                    success: function (data, status, xhr) {
                        ko.mapping.fromJS(data, {}, request);
                        if (data.ready) {
                            request.refresh(false);
                        }
                    },
                    error: function (data, jqxhr, err) {
                        ko.mapping.fromJS({errorMessage: err},{},request);
                        request.refresh(false);
                    }
                });
            }
        };
        jQuery(function () {
            request.refresh.subscribe(function (newval) {
                if (newval) {
                    request.timeout = setInterval(function () {
                        request.refreshData();
                    }, 5000);
                } else if (request.timeout) {
                    clearInterval(request.timeout);
                    request.timeout = null;
                }
            });
            ko.mapping.fromJS(loadJsonData('requestdata'), {}, request);
            ko.applyBindings(request);
        });
    </g:javascript>
</head>

<body>
<div class="panel panel-default panel-tab-content" data-bind="visible: ready() && !notFound() && !errorMessage()">
    <div class="panel-heading">
        Download an archive of project <strong><g:enc>${params.project ?: request.project}</g:enc></strong>
    </div>

    <div class="panel-body">

        <g:link controller="project" action="exportWait"
                params="[project: params.project ?: request.project, token: params.token, download: true]"
                class="btn btn-success">
            <i class="glyphicon glyphicon-download-alt"></i>
            <g:enc>${params.project ?: request.project}</g:enc>.rdproject.jar
        </g:link>
        <div class="text-info">
            This archive will be available for 30 minutes.
        </div>

    </div>
    <div class="panel-footer">
        <g:link controller="menu" action="admin" params="${[project: params.project]}">
            &larr; Return to Configuration
        </g:link>
    </div>

</div>

<div class="panel panel-danger panel-tab-content" data-bind="visible: notFound">
    <div class="panel-heading">
        Not Found
    </div>

    <div class="panel-body">

        The token <code>${token}</code> was not found.  It may have expired.

    </div>

</div>

<div class="panel panel-danger panel-tab-content" data-bind="visible: errorMessage">
    <div class="panel-heading">
        There was an error exporting the Project Archive
    </div>

    <div class="panel-body" data-bind="text: errorMessage">

        ${requestError?.message}

    </div>

</div>

<div class="panel panel-default panel-tab-content" data-bind="visible: !ready() && !errorMessage() && !notFound()">
    <div class="panel-heading">
        Exporting an archive of project <strong><g:enc>${params.project ?: request.project}</g:enc></strong>&hellip;
    </div>

    <div class="panel-body">
        <div class="container ">
        <div class="col-md-12 text-info ">
            Please wait, your request is being processed.
        </div>
        <div class="col-md-6">
        <g:render template="/common/progressBar" model="${[
                completePercent:percentage?:0,
               bind: 'percentage()',
                showpercent: true,
        ]}"/>
        </div>
        </div>
    </div>
    <div class="panel-footer form-inline">

        <g:link controller="project" action="exportWait"
                params="[project: params.project ?: request.project, token: params.token]"
                class="btn  btn-link reload_button"
                data-loading="Loading...">
            <i class="glyphicon glyphicon-refresh"></i>
            Refresh this page
        </g:link>
        <div class="checkbox">
            <label>
                <input type="checkbox" id="dorefresh" value="true" data-bind="checked: refresh"/>
                Refresh every 5 seconds
            </label>
        </div>
    </div>
</div>
</body>
</html>