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

<%@ page import="grails.util.Environment" %>
<html>
<head>
    <meta http-equiv="Content-Type" content="text/html; charset=UTF-8"/>
    <meta name="layout" content="base"/>
    <meta name="tabpage" content="jobs"/>
    <title><g:appTitle/> - <g:enc>${scheduledExecution?.jobName}</g:enc></title>
    <g:javascript library="prototype/effects"/>
    <asset:javascript src="menu/joboptions.js"/>
    <asset:javascript src="menu/jobs.js"/>
    <asset:javascript src="util/markdeep.js"/>
    <g:embedJSON id="jobParams"
                 data="${[filter: scheduledExecution?.filter, doNodeDispatch: scheduledExecution?.doNodedispatch, project: params.project
                         ?:
                         request.project]}"/>
    <g:embedJSON id="jobDetail"
                 data="${[id: scheduledExecution?.extid, name: scheduledExecution?.jobName, group: scheduledExecution?.groupPath,
                          project: params.project ?: request.project]}"/>
    <g:embedJSON id="pageParams" data="${[project: params.project ?: request.project]}"/>

    <g:jsMessages code="Node,Node.plural,option.value.required,options.remote.dependency.missing.required,option.default.button.title,option.default.button.text,option.select.choose.text"/>
    <script type="text/javascript">
        var pagehistory;
        var joboptions;
        var remotecontroller;
        function loadTab(anchor) {
            "use strict";
            var tabs = jQuery('#jobtabs').find('a[data-toggle="tab"]').map(function (i, e) {
                return jQuery(e).attr('href');
            }).get();
            if (tabs.indexOf(anchor)>=0) {
                jQuery('a[href="' + anchor + '"]').tab('show');
            }
        }
        function init() {
            "use strict";
            var params = loadJsonData('jobParams');
            var jobNodeFilters = initJobNodeFilters(params);
            ko.applyBindings(jobNodeFilters, document.getElementById('definition'));

            pagehistory = new History(appLinks.reportsEventsAjax, appLinks.menuNowrunningAjax);
            ko.applyBindings(pagehistory, document.getElementById('activity_section'));
            setupActivityLinks('activity_section', pagehistory);

            //setup option edit
            var joboptiondata = loadJsonData('jobOptionData');
            joboptions = new JobOptions(joboptiondata);
            ko.applyBindings(joboptions, document.getElementById('optionSelect'));

            var remoteoptionloader = new RemoteOptionLoader({
                url: "${createLink(controller:'scheduledExecution',action:'loadRemoteOptionValues',params:[format:'json'])}",
                id:"${scheduledExecution.extid}",
                fieldPrefix: "extra.option."
            });
            remotecontroller = new RemoteOptionController({ loader: remoteoptionloader});
            remotecontroller.setupOptions(joboptions);
            remotecontroller.loadData(loadJsonData('remoteOptionData'));

            joboptions.remoteoptions = remotecontroller;
            remotecontroller.begin();

            jQuery('input').on('keydown', function (evt) {
                return noenter(evt);
            });

            loadTab(document.location.hash);
            jQuery(window).on('hashchange', function () {
                loadTab(document.location.hash);
            });
            jQuery(window).on('show.bs.tab', function (e) {
                var t = jQuery(e.target);
                if (t.attr('href').startsWith('#')) {
                    document.location.hash = t.attr('href');
                }
            });
        }
        jQuery(init);
    </script>
</head>

<body>
<tmpl:show scheduledExecution="${scheduledExecution}" crontab="${crontab}"/>
</body>
</html>


