<html>
  <head>
    <meta http-equiv="Content-Type" content="text/html; charset=UTF-8"/>
    <meta name="layout" content="base" />
    <meta name="tabpage" content="jobs"/>
    <title><g:message code="main.app.name"/> - ${scheduledExecution?.jobName.encodeAsHTML()} : ${scheduledExecution?.description?.encodeAsHTML()}</title>
    <g:javascript library="prototype/effects"/>
      <g:javascript>

        var runupdate;
        function loadNowRunning(){
            runupdate=new Ajax.PeriodicalUpdater({ success:'nowrunning'},'${createLink(controller: "menu", action: "nowrunningFragment")}',{
                evalScripts:true,
                parameters:{projFilter:'${session.project}',jobIdFilter:"${scheduledExecution.uuid}"},
                onFailure:function (response) {
                    showError("AJAX error: Now Running [" + runupdate.url + "]: " + response.status + " "
                                      + response.statusText);
                    runupdate.stop();
                }
            });
        }

        /**
         * START page init
         */

        function init() {
            loadNowRunning();
        }

        Event.observe(window,'load',init);
      </g:javascript>
  </head>

  <body>
        <tmpl:show scheduledExecution="${scheduledExecution}"  crontab="${crontab}"/>
  </body>
</html>


