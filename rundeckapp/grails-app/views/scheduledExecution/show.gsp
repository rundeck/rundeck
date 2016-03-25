<%@ page import="grails.util.Environment" %>
<html>
  <head>
    <meta http-equiv="Content-Type" content="text/html; charset=UTF-8"/>
    <meta name="layout" content="base" />
    <meta name="tabpage" content="jobs"/>
    <title><g:appTitle/> - <g:enc>${scheduledExecution?.jobName}</g:enc></title>
    <g:javascript library="prototype/effects"/>
    <g:render template="/framework/remoteOptionValuesJS"/>
    <g:javascript library="executionOptions"/>
    <asset:javascript src="menu/jobs.js"/>
    <g:embedJSON id="jobParams"
                 data="${[ filter: scheduledExecution?.filter, doNodeDispatch:scheduledExecution?.doNodedispatch,project:params.project?:request.project]}"/>
    <g:embedJSON id="pageParams" data="${[project:params.project?:request.project]}"/>

    <g:jsMessages code="Node,Node.plural"/>
      <script type="text/javascript">
      var pagehistory;
      function init(){
        var params=loadJsonData('jobParams');
        var jobNodeFilters=initJobNodeFilters(params);
        ko.applyBindings(jobNodeFilters,document.getElementById('schedExDetails'));

        pagehistory = new History(appLinks.reportsEventsAjax, appLinks.menuNowrunningAjax);
        ko.applyBindings(pagehistory, document.getElementById('activity_section'));
        setupActivityLinks('activity_section', pagehistory);
      }
      jQuery(init);
    </script>
  </head>

  <body>
        <tmpl:show scheduledExecution="${scheduledExecution}"  crontab="${crontab}"/>
  </body>
</html>


