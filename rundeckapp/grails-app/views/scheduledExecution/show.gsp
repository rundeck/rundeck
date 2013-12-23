<%@ page import="grails.util.Environment" %>
<html>
  <head>
    <meta http-equiv="Content-Type" content="text/html; charset=UTF-8"/>
    <meta name="layout" content="base" />
    <meta name="tabpage" content="jobs"/>
    <title><g:message code="main.app.name"/> - ${scheduledExecution?.jobName.encodeAsHTML()} : ${scheduledExecution?.description?.encodeAsHTML()}</title>
    <g:javascript library="prototype/effects"/>
    <g:render template="/framework/remoteOptionValuesJS"/>
    <g:javascript library="executionOptions"/>
    <g:if test="${grails.util.Environment.current == Environment.DEVELOPMENT}">
        <g:javascript src="knockout-3.0.0.debug.js"/>
    </g:if>
    <g:else>
        <g:javascript src="knockout-3.0.0-min.js"/>
    </g:else>
    <g:javascript src="knockout.mapping-latest.js"/>
    <g:javascript src="moment.min.js"/>
    <asset:javascript src="momentutil.js"/>
    <g:javascript>
        var statusMessages= {
            succeed: "${g.message(code: 'status.label.succeed')}",
            fail: "${g.message(code: 'status.label.fail')}",
            cancel: "${g.message(code: 'status.label.cancel')}"
        };
        var executionLink="${g.createLink(controller: 'execution', action: 'show', id: 'ZZZ', absolute: true)}";

        function Report(data){
            var self=this;
            self.dateCompleted=ko.observable();
            self.dateStarted=ko.observable();
            self.execution=ko.observable();

            self.showExecutionLink=function(e){
                return executionLink.replace('ZZZ',ko.utils.unwrapObservable(self.jcExecId()));
            };
            self.statusText= ko.computed( function(){
                return statusMessages[ko.utils.unwrapObservable(self.status())];
            });
            self.duration= ko.computed(function(){
                return MomentUtil.duration(ko.utils.unwrapObservable(self.dateStarted()),ko.utils.unwrapObservable(self.dateCompleted()));
            });
            self.durationSimple= ko.computed(function(){
                return MomentUtil.formatDurationSimple(self.duration());
            });
            self.durationHumanize= ko.computed(function(){
                return MomentUtil.formatDurationHumanize(self.duration());
            });
            self.startTimeFormat= function(format){
                return MomentUtil.formatTime(self.dateStarted(),format);
            };
            self.endTimeSimple= ko.computed(function(){
                return MomentUtil.formatTimeSimple(self.dateCompleted());
            });
            self.endTimeFormat= function (format) {
                var value=self.dateCompleted();
                return MomentUtil.formatTime( value, format);
            };
            self.nodeFailCount= ko.computed( function(){
                var ncount=ko.utils.unwrapObservable(self.node);
                var ns=ncount.split('/');
                if(ns.length==3){
                    return parseInt(ns[1]);
                }
            });

            self.nodeSucceedCount= ko.computed(function(){
                var ncount=ko.utils.unwrapObservable(self.node);
                var ns=ncount.split('/');
                if(ns.length==3){
                    return parseInt(ns[0]);
                }
            });
            ko.mapping.fromJS(data,{},self);
        }
        var history={
            reports:ko.observableArray([]),
            href:ko.observableArray([]),
            selected:ko.observable(false),

            params:ko.observable()
        };
        var binding={
            'reports': {
                key: function (data) {
                    return ko.utils.unwrapObservable(data.id);
                },
                create: function (options) {
                    return new Report(options.data);
                }
            }
            };
        jQuery(document).ready(function(){
            ko.applyBindings(history);
           jQuery('a.activity_link').click(function(e){
                e.preventDefault();
               var me=jQuery(this)[0];
               var params = me.search;
               var url="${g.createLink(controller: 'reports',action: 'eventsAjax',absolute:true)}"+params;
               jQuery.getJSON(url,function(data){
                history.selected(true);
                jQuery('a.activity_link').removeClass('active');
                jQuery(me).addClass('active');
                ko.mapping.fromJS({ href: me.getAttribute('href'), reports:data.reports, params:params },binding,history);
               });

           });
        });
    </g:javascript>
  </head>

  <body>
        <tmpl:show scheduledExecution="${scheduledExecution}"  crontab="${crontab}"/>
  </body>
</html>


