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
    <g:embedJSON id="jobParams"
                 data="${[filter: scheduledExecution?.filter, doNodeDispatch: scheduledExecution?.doNodedispatch, project: params.project
                         ?:
                         request.project]}"/>
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
            });
        }
        jQuery(init);
    </script>
</head>

<body>
<tmpl:show scheduledExecution="${scheduledExecution}" crontab="${crontab}"/>
</body>
</html>


