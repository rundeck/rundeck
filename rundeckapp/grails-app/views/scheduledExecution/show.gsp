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
        function init() {
            var params = loadJsonData('jobParams');
            var jobNodeFilters = initJobNodeFilters(params);
            ko.applyBindings(jobNodeFilters, document.getElementById('schedExDetails'));

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
            })

            //Rundeck-853 Override option list by providing options in URL (https://github.com/rundeck/rundeck/issues/853)
            setTimeout(function () {
                overrideOptions(window.location.href);
            }, 200);
        }

        function overrideOptions(href) {
            var data = parseUrlParams(href)
            for (var key in data){

                //1. when option is not strict and allowed to enter any values other then drop downs
                var element = document.getElementsByName("extra.option."+key);
                if(jQuery(element).is(":text")) {
                    element[0].value = data[key];
                }

                var selectElement = document.getElementById("extra.option."+key);
                if(jQuery(selectElement).is("select")) {
                    if(selectElement == undefined || selectElement == null) {
                        selectElement = document.getElementsByName("extra.option."+key)[0];
                    }
                    selectElement.value = data[key];
                }

                //2. when option is strict
                var selectElementStrict = document.getElementsByName("extra.option."+key)[0];
                if(jQuery(selectElementStrict).is("select")) {
                    selectElementStrict.value = data[key];
                }

                //3. when options are multivalued
                var checkboxeElements = document.getElementsByName("extra.option."+key);
                if(jQuery(checkboxeElements).is(":checkbox")) {
                    var elementValues = data[key].split(",");
                    if(elementValues.length > 0) {
                        jQuery('input[name="extra.option.'+key+'"]').prop('checked', false);
                    }
                    for(i = 0; i<elementValues.length; i++) {
                        jQuery('input[name="extra.option.'+key+'"][value='+elementValues[i]+']').prop('checked', true);
                    }
                }

            }
        }
        jQuery(init);
    </script>
</head>

<body>
<tmpl:show scheduledExecution="${scheduledExecution}" crontab="${crontab}"/>
</body>
</html>


