<%--
  Created by IntelliJ IDEA.
  User: greg
  Date: 10/4/13
  Time: 10:23 AM
  To change this template use File | Settings | File Templates.
--%>

<%@ page import="com.dtolabs.rundeck.server.authorization.AuthConstants" contentType="text/html;charset=UTF-8" %>
<html>
<head>
    <meta http-equiv="Content-Type" content="text/html; charset=UTF-8"/>
    <g:set var="appTitle"
       value="${grailsApplication.config.rundeck.gui.title ? grailsApplication.config.rundeck.gui.title : g.message(code: 'main.app.name')}"/>
    <meta name="layout" content="base"/>
    <meta name="tabpage" content="home"/>
    <title>${appTitle}</title>

</head>
<body>
<div class="row">
    <div class="col-sm-12">
        <span class="h3">Projects</span>
    </div>
</div>
<div class="row row-space">
<div class="col-sm-3">
<g:if test="${session.frameworkProjects}">
    <g:set var="projectSet" value="${session.frameworkProjects*.name.sort()}"/>
    <ul class="nav nav-pills nav-stacked">
    <g:each in="${projectSet}" var="project">
        <li class="${session.project==project?'active':''}">
            <a href='#'
                    class="obs_project" data-project="${project}">
                ${project.encodeAsHTML()}
            </a>
        </li>
    </g:each>
    </ul>
</g:if>
</div>
    <div class="col-sm-9">
        <div class="alert alert-warning" id="alert" style="display: none;">

        </div>
        <g:each in="${projectSet}" var="project">
        <div id="proj_${project.encodeAsHTML()}" style="${wdgt.styleVisible(if:session.project==project)};" class="view_project">

            <div id="projsummary_${project.encodeAsHTML()}" >
            </div>

            <div class="panel panel-info" id="projdesc_${project.encodeAsHTML()}" style="display: none;">
                <div class="panel-body content">
                </div>
            </div>
        </div>
        </g:each>
    </div>
</div>
<script id="projectSummary" type="text/template">
%{--Template for project details--}%
<div class="panel panel-default">
    <div class="list-group">
        <div class="list-group-item">
            <div class="row">
                <div class="col-sm-8">
                    <a class="h4" href="${g.createLink(controller:"framework",action:"selectProject",params:[page: 'jobs'])}&amp;project=<!= project !>">
                            <span class="<!= jobCount > 0 ? 'text-info' : 'text-muted' !>"><!= jobCount !></span> Jobs
                    </a>
                </div>
                <div class="col-sm-4">
                    <! if(auth.jobCreate) { !>
                    <div class="btn-group pull-right">
                        <button type="button" class="btn btn-default btn-sm dropdown-toggle" data-toggle="dropdown">
                            Create <g:message code="domain.ScheduledExecution.title"/>
                            <span class="caret"></span>
                        </button>
                        <ul class="dropdown-menu pull-right" role="menu">
                            <li><a href="${g.createLink(controller: "framework", action: "selectProject", params: [page: 'createJob'])}&amp;project=<!= project !>">
                                New <g:message
                                        code="domain.ScheduledExecution.title"/>&hellip;</a></li>
                            <li class="divider">
                            </li>
                            <li>
                                <a href="${g.createLink(controller: "framework", action: "selectProject", params: [page: 'uploadJob'])}&amp;project=<!= project !>"
                                        class="">
                                    <i class="glyphicon glyphicon-upload"></i>
                                    Upload Definition&hellip;
                                </a>
                            </li>
                        </ul>
                    </div>
                    <! } !>
                </div>
            </div>
        </div>
        <div class="list-group-item">
            <div class="row">
                <div class="col-sm-12">
                    <a class="h4"
                       href="${g.createLink(controller: "framework", action: "selectProject", params: [page: 'activity'])}&amp;project=<!= project !>">
                        <span class="<!= execCount > 0 ? 'text-info' : 'text-muted' !>"><!= execCount !></span> <strong>Executions</strong>
                        In the last day
                    </a>
                    <div>
                        <! if(userCount > 0 ) { !>
                        by
                        <span class="text-info">
                        <!= userCount !>
                        </span>
                        users:
                            <! for(var i=0;i < users.size() ; i++){ !>
                                <!- users[i] !><! if(i < users.size()-1 ){ !>,<! } !>
                            <! } !>
                        <! } !>
                    </div>
                </div>
            </div>
        </div>

        <div class="list-group-item">
            <div class="row">
                <div class="col-sm-12">
                    <a class="h4" href="${g.createLink(controller: "framework", action: "selectProject", params: [page: 'nodes'])}&amp;project=<!= project !>">
                        <! if(nodeCount !=null){ !>
                        <! if(nodeCount > 0 ) { !>
                        <span class="text-info">
                            <!= nodeCount !>
                        </span>
                        <! }else{ !>
                        <!= nodeCount !>
                        <! } !>
                        <! } !>
                        Nodes
                    </a>
                </div>
            </div>
        </div>
    </div>
</div>
</script>
<g:javascript>
    /*Loads project summary details via json and popuplates div by expanding the template*/
    function loadProjectSummary(project){
        var summary = jQuery('#projsummary_' + project);
        jQuery.getJSON("${createLink(controller: 'framework', action: 'apiProjectSummary')}?project="+project,
            function(data){
                data.project=project;
                summary.html( _.template(jQuery('#projectSummary').html(),data) );
            }
        ).fail(function(data, textStatus, error){
            var msg = "Sorry but there was an error: ";
            var err = textStatus + ", " + error;
            jQuery( "#alert" ).html( msg + err  ).show();
        });
    }
    /*Loads project readme and motd */
    function loadProjectDesc(project){
        var desc = jQuery('#projdesc_' + project);
        jQuery.get("${createLink(controller: 'framework', action: 'projectDescFragment')}?project="+project,
        function( data ) {
            jQuery(desc).children('.content').html(data);
            if(data==''){
                jQuery(desc).hide();
            }else{
                jQuery(desc).show();
            }
        }).fail(function(data, textStatus, error){
            var msg = "Sorry but there was an error: ";
            var err = textStatus + ", " + error;
            jQuery( "#alert" ).html( msg + err  ).show();
        });
    }
    jQuery(function () {
        jQuery('.obs_project').each(function (i, elem) {
            var project = jQuery(elem).data('project');
            jQuery(elem).click(function () {
                jQuery(elem).closest('ul.nav').children('li').removeClass('active');
                jQuery(elem).closest('li').addClass('active');
                jQuery('.view_project').hide();
                loadProjectDesc(project);
                loadProjectSummary(project);
                jQuery('#proj_' + project).show();
            });
        });
        <g:if test="${session.project}">
            loadProjectSummary('${session.project.encodeAsJavaScript()}');
            loadProjectDesc('${session.project.encodeAsJavaScript()}');
        </g:if>
    });
</g:javascript>
</body>
</html>
