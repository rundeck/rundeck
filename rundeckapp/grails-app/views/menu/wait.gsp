%{--
  - Copyright 2016 SimplifyOps, Inc. (http://simplifyops.com)
  -
  - Licensed under the Apache License, Version 2.0 (the "License");
  - you may not use this file except in compliance with the License.
  - You may obtain a copy of the License at
  -
  -     http://www.apache.org/licenses/LICENSE-2.0
  -
  - Unless required by applicable law or agreed to in writing, software
  - distributed under the License is distributed on an "AS IS" BASIS,
  - WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  - See the License for the specific language governing permissions and
  - limitations under the License.
  --}%

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
    <title><g:message code="archive.request.please.wait.pagetitle.wait" default="Export archive"/></title>
    <asset:javascript src="knockout.min.js"/>
    <asset:javascript src="knockout-mapping.js"/>
    <g:embedJSON data="${[
            ready:ready,
            token:token,
            refresh:true,
            notFound:notFound,
            errorMessage:requestError?.message,
            percentage:percentage,
            instance:instance,
            errors: errors,
            url:createLink(controller: 'project', action: 'exportWait', params: [project: params.project, token: params.token,instance:params.instance, iproject:params.iproject, format: 'json'])
    ]}" id="requestdata"/>
    <g:jsMessages code="archive.request.please.wait.pagetitle.ready"/>
    <g:javascript>
        function Wait() {
            var self = this;
            self.ready = ko.observable(false);
            self.notFound = ko.observable(false);
            self.errorMessage = ko.observable(null);
            self.refresh = ko.observable(false);
            self.percentage = ko.observable(0);
            self.instance = ko.observable(null);
            self.token = null;
            self.timeout = null;
            self.url = null;
            self.errors = ko.observable(null);
            self.refreshData = function () {
                jQuery.ajax({
                    url: self.url,
                    dataType: 'json',
                    success: function (data, status, xhr) {
                        ko.mapping.fromJS(data, {}, self);
                        if (data.ready) {
                            self.refresh(false);
                        }
                    },
                    error: function (data, jqxhr, err) {
                        ko.mapping.fromJS({errorMessage: err},{},self);
                        self.refresh(false);
                    }
                });
            }
        }
        var request = new Wait();
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
            request.ready.subscribe(function(newval){
                if(newval){
                    document.title = message('archive.request.please.wait.pagetitle.ready');
                }
            });
            ko.mapping.fromJS(loadJsonData('requestdata'), {}, request);
            ko.applyBindings(request);
        });
    </g:javascript>
</head>

<body>
<div class="content">
<div id="layoutBody">
<div class="panel panel-default panel-tab-content" data-bind="visible: ready() && !notFound() && !errorMessage() && !errors()">
    <div class="panel-heading">
        <g:if test="${!params.instance}">
        <g:message code="archive.request.download.title" args="${[params.project ?: request.project]}"/>
        </g:if>
        <g:if test="${params.instance}">
            <g:message code="export.another.instance.process" args="${[params.project ?: request.project,params.instance]}"/>
        </g:if>
    </div>

    <div class="panel-body">
        <g:if test="${!params.instance}">
        <g:link controller="project" action="exportWait"
                params="[project: params.project ?: request.project, token: params.token, download: true]"
                class="btn btn-success">
            <i class="glyphicon glyphicon-download-alt"></i>
            <g:enc>${params.project ?: request.project}</g:enc>.rdproject.jar
        </g:link>
        <div class="text-info" style="margin-top:1em;">
            <g:message code="archive.request.will.expire" />
        </div>
        </g:if>
        <g:if test="${params.instance}">
            <div class="alert alert-info">
                <g:message code="export.another.instance.success" />
            </div>

            <g:link url="${params.instance}/?project=${params.iproject}">Go to the other instance</g:link>
        </g:if>

    </div>
    <div class="panel-footer">
        <g:link class="btn btn-default btn-sm" controller="menu" action="projectExport" params="${[project: params.project]}">
            <g:message code="return.to.configuration" />
        </g:link>
    </div>

</div>

<div class="panel panel-danger panel-tab-content" data-bind="visible: notFound">
    <div class="panel-heading">
        <g:message code="request.error.notfound.title" />
    </div>

    <div class="panel-body">

        <g:message code="archive.request.token.not.found" args="${[token]}" />

    </div>

</div>

<div class="panel panel-danger panel-tab-content" data-bind="visible: errorMessage">
    <div class="panel-heading">
        <g:message code="archive.request.error" />
    </div>

    <div class="panel-body" data-bind="text: errorMessage">

        ${requestError?.message}

    </div>

</div>

<div class="panel panel-danger panel-tab-content" data-bind="visible: errors">
    <div class="panel-heading">
        <g:message code="export.another.instance.process" args="${[params.project ?: request.project,params.instance]}"/>
    </div>

    <div class="panel-body">

        <ul data-bind="foreach: errors()" class="list-unstyled">
            <li><pre data-bind="text: $data"></pre></li>
        </ul>


        <g:link url="${params.instance}/?project=${params.iproject}"><g:message code="export.another.instance.goto"></g:message></g:link>

    </div>

    <div class="panel-footer">
        <g:link class="btn btn-default btn-sm" controller="menu" action="projectExport" params="${[project: params.project]}">
            <g:message code="return.to.configuration" />
        </g:link>
    </div>

</div>

<div class="panel panel-default panel-tab-content" data-bind="visible: !ready() && !errorMessage() && !notFound() && !errors()">
    <div class="panel-heading">
        <g:message code="archive.request.exporting.title" args="${[params.project ?: request.project]}"/>
    </div>

    <div class="panel-body">
        <div class="container ">
        <div class="col-md-12 text-info" style="margin-bottom:1em">
            <g:message code="archive.request.please.wait" />
        </div>
        <div class="col-md-6">
        <g:render template="/common/progressBar" model="${[
                completePercent:percentage?:0,
                bind: 'percentage()',
                showpercent: true,
                height:28
        ]}"/>
        </div>
        </div>
    </div>
    <div class="panel-footer form-inline">

        <g:link controller="project" action="exportWait"
                params="[project: params.project ?: request.project, token: params.token,instance:params.instance, iproject:params.iproject]"
                class="btn btn-default btn-sm reload_button"
                data-loading="Loading...">
            <i class="glyphicon glyphicon-refresh"></i>
            <g:message code="refresh.this.page" />
        </g:link>
        <div class="checkbox">
          <input type="checkbox" id="dorefresh" value="true" data-bind="checked: refresh"/>
            <label for="dorefresh">
                <g:message code="refresh.every.5.seconds" />
            </label>
        </div>
    </div>
</div>
</div>
</div>
</body>
</html>
