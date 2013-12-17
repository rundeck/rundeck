<%@ page import="com.dtolabs.rundeck.server.authorization.AuthConstants" %>
<div class="list-group">
<g:if test="${flash.message}">
    <div class="list-group-item">
    <div class="alert alert-info">${flash.message}</div>
    </div>
</g:if>
<g:hasErrors bean="${scheduledExecution}">
    <div class="list-group-item">
    <div class="alert alert-danger">
        <g:renderErrors bean="${scheduledExecution}" as="list"/>
    </div>
    </div>
</g:hasErrors>
<g:set var="labelColSize" value="col-sm-2"/>
<g:set var="labelColClass" value="${labelColSize}  control-label"/>
<g:set var="fieldColSize" value="col-sm-10"/>
<g:set var="fieldColHalfSize" value="col-sm-5"/>
<g:set var="offsetColSize" value="col-sm-10 col-sm-offset-2"/>
<g:set var="NODE_FILTERS" value="${['Name','Tags']}"/>
<g:set var="NODE_FILTERS_X" value="${['','OsName','OsFamily','OsArch','OsVersion']}"/>
<g:set var="NODE_FILTERS_ALL" value="${['Name','Tags','','OsName','OsFamily','OsArch','OsVersion']}"/>
<g:set var="NODE_FILTER_MAP" value="${['':'Hostname','OsName':'OS Name','OsFamily':'OS Family','OsArch':'OS Architecture','OsVersion':'OS Version']}"/>

<g:set var="isWorkflow" value="${true}"/>
<g:set var="editSchedExecId" value="${scheduledExecution?.id? scheduledExecution.id:null}"/>
<g:javascript library="prototype/scriptaculous"/>
<g:javascript library="prototype/effects"/>
<g:javascript library="prototype/dragdrop"/>

<script type="text/javascript">
//<!CDATA[
        var selFrameworkProject='${scheduledExecution?.project?scheduledExecution?.project.encodeAsJavaScript():projects?.size()==1?projects[0].name.encodeAsJavaScript():''}';
        var selArgs='${scheduledExecution?.argString?.encodeAsJavaScript()}';
        var isWorkflow=${isWorkflow};
var node_filter_map =${NODE_FILTER_MAP.encodeAsJSON()};
var node_filter_keys =${NODE_FILTERS_ALL.encodeAsJSON()};
var curSEID =${editSchedExecId?editSchedExecId:"null"};
function getCurSEID(){
    return curSEID;
}
var applinks={
    frameworkNodesFragment:"${createLink(controller:'framework',action:'nodesFragment')}",
    workflowEdit:'${createLink(controller:"workflow",action:"edit")}',
    workflowRender:'${createLink(controller:"workflow",action:"render")}',
    workflowSave:'${createLink(controller:"workflow",action:"save")}',
    workflowReorder:'${createLink(controller:"workflow",action:"reorder")}',
    workflowRemove:'${createLink(controller:"workflow",action:"remove")}',
    workflowUndo:'${createLink(controller:"workflow",action:"undo")}',
    workflowRedo:'${createLink(controller:"workflow",action:"redo")}',
    workflowRevert:'${createLink(controller:"workflow",action:"revert")}',
    workflowRenderUndo:'${createLink(controller:"workflow",action:"renderUndo")}',

    editOptsRenderUndo:'${createLink(controller:"editOpts",action:"renderUndo")}',
    editOptsEdit:'${createLink(controller:"editOpts",action:"edit")}',
    editOptsRender:'${createLink(controller:"editOpts",action:"render")}',
    editOptsSave:'${createLink(controller:"editOpts",action:"save")}',
    editOptsRenderAll:'${createLink(controller:"editOpts",action:"renderAll")}',
    editOptsRenderSummary:'${createLink(controller:"editOpts",action:"renderSummary")}',
    editOptsRemove:'${createLink(controller:"editOpts",action:"remove")}',
    editOptsUndo:'${createLink(controller:"editOpts",action:"undo")}',
    editOptsRedo:'${createLink(controller:"editOpts",action:"redo")}',
    editOptsRevert:'${createLink(controller:"editOpts",action:"revert")}'
};

//]>
</script>
<g:javascript library="jobedit"/>

<script type="text/javascript">
//<!CDATA[

        function jobChosen(name,group){
            $('jobNameField').setValue(name);
            $('jobNameField').highlight();
            $('jobGroupField').setValue(group);
            $('jobGroupField').highlight();
            hideJobChooser();
        }
        function loadJobChooser(elem,target){
            if($('jobChooser').visible()){
                hideJobChooser();
                return;
            }
            var project=$F('schedEditFrameworkProject');
            jQuery(elem).button('loading');

            $(elem).addClassName('active');
            new Ajax.Updater(
                'jobChooserContent',
                '${createLink(controller:"menu",action:"jobsPicker")}',
                {
                parameters: {jobsjscallback:'jobChosen',projFilter:project,runAuthRequired:true},
                 onSuccess: function(transport) {
                    new MenuController().showRelativeTo(elem,target);
                     jQuery('#jobChooseBtn').button('reset');
                 },
                 onFailure: function() {
                     showError("Error performing request: groupTreeFragment");
                     jQuery('#jobChooseBtn').button('reset');
                 }
                });
        }
        function hideJobChooser(){
            $('jobChooser').hide();
            $('jobChooseBtn').removeClassName('active');
            jQuery('#jobChooseBtn').button('reset');
        }



        var wascancelled=false;
        function jobEditCancelled(){
            wascancelled=true;
        }
        /**
         * Validate the form
         *
         */
         function validateJobEditForm(form){
             var wfitem=$(form).down('div.wfitemEditForm');
             if(wfitem && !wascancelled){
                 doyft(wfitem.identify());
                 $(wfitem).scrollTo();
                 if ($(wfitem).down("span.cancelsavemsg")) {
                     $(wfitem).down("span.cancelsavemsg").show();
                 }
                 return false;
             }
            var optedit= $(form).down('div.optEditForm');
            if (optedit && !wascancelled) {
                doyft(optedit.identify());
                $(optedit).scrollTo();
                if($(optedit).down("span.cancelsavemsg")){
                    $(optedit).down("span.cancelsavemsg").show();
                }
                return false;
            }
             return true;
         }
        function pageinit(){
            _enableDragdrop();

            Event.observe(document,'keydown',function(evt){
                //escape key hides popup bubble
                if(evt.keyCode===27 ){
                    tooltipMouseOut();
                }
                return true;
            },false);
        }

        Event.observe(window,'load',pageinit);
//]>
</script>
<style lang="text/css">
    textarea.code{
        font-family: Courier,monospace;
        font-size:100%;
    }
    /** drag and drop for workflow editor */
    #workflowDropfinal.ready{
        padding:2px;
        margin-top:3px;
        display:block;
        -moz-border-radius : 3px;
        -webkit-border-radius: 3px;
        border-radius: 3px;
    }
    #workflowContent ol li{
        padding:0;
    }
    #workflowContent ol li.hoverActive{
        border-top: 2px solid blue;
    }
    #workflowDropfinal.hoverActive{
        border-top: 2px solid blue;
        padding:8px;

    }


    /*** ***/
    div.wfctrlholder{
        position:relative;
        padding-right:200px;
    }
    .wfitem{
        display: block;
        padding: 5px;
    }
    .wfitemcontrols{
        margin-left:10px;
        position:absolute;
        right:0;
        top:0;
        width:250px;
        text-align:right;
    }
    .controls.autohide{
        visibility: hidden;
    }
    li:hover .controls.autohide{
        visibility: visible;
    }

    /** option controls and view layout **/
    div.optctrlholder{
        position:relative;
        padding-right:40px;
    }
    .optctrl{
        margin-left:10px;
        position:absolute;
        right:0;
        top:0;
        width:120px;
        text-align:right;
    }
    .optview{
        /*position:relative;*/
    }
    .optdetail{
        /*float:left;*/
        width:540px;
        overflow:hidden;
        white-space:nowrap;
        height:16px;
        line-height:16px;
    }
    .enforceSet{
        /*position:absolute;*/
        /*right: 45px;*/
        display: inline-block;
        width:100px;
        overflow:hidden;
        white-space:nowrap;
        height:16px;
        line-height:16px;
    }
    .enforceSet span.any,.opt.item .enforceSet span.enforced,.opt.item .enforceSet span.regex{
        text-align:right;
    }
    .valuesSet{
        /*position:absolute;*/
        /*right: 150px;*/
        display: inline-block;
        width: 60px;
        overflow:hidden;
        white-space:nowrap;
        height:16px;
        line-height:16px;
    }
    span.truncatedtext{
        border-bottom:1px dotted #aaa;
    }

    /*** options edit form **/
    ul.options{
        margin:0;
        padding:0;
    }
    ul.options li{
        list-style:none;
        padding: 4px;
    }
    div.inputset > div {
        clear:both;
    }
    label.left,span.left{
        float:left;
        width:10em;
        display:block;
    }
    div.inputset > div > .right{
        margin-left: 0.5em;
    }
    label.left.half,span.left.half{
        width:50%;
    }

    /*** END Options */

    .nodefilterfield{
        margin-bottom: 5px;
    }
    .nodefilterfield label span.text{
        text-align:right;
        width: 10em;
        display:inline-block;
    }
    .filterkeyset{
        padding:3px 0;
    }

    #matchednodes .allnodes{
        width:600px;
    }

    /** add step styles */
    .add_step_buttons ul{
        margin:0;
        list-style-type: none;
        padding-left:0;
    }
    .add_step_buttons li.action{
        padding:5px;
        margin:0;
    }
    .add_step_buttons li.action:hover{
    }

    /**
    job edit form table
     */
    table.jobeditform > tbody > tr > td:first-child{
        width:120px;
    }
    /**
    Ace editor
    */
    div.ace_text{
        border:1px solid #aaa;
    }
    .pflowlist{
        margin-right: 10px;
    }
</style>

<input type="hidden" name="id" value="${scheduledExecution?.id}"/>


<div class="alert alert-danger" style="display: none" id="editerror">
    
</div>

    <div class="list-group-item"  >
        %{--name--}%
    <div class="form-group ${g.hasErrors(bean:scheduledExecution,field:'jobName','has-error')}" id="schedJobNameLabel">
        <label for="schedJobName"
               class="required ${labelColClass}"
               >
            <g:message code="domain.ScheduledExecution.title"/> Name
        </label>
        <div class="${fieldColHalfSize}">
            <g:textField name="jobName"
                         value="${scheduledExecution?.jobName.encodeAsHTML()}"
                         id="schedJobName"
                         class="form-control"
            />
            <g:hasErrors bean="${scheduledExecution}" field="jobName">
                <i alt="Error" id="schedJobNameErr" class="glyphicon glyphicon-warning-sign"></i>
                <wdgt:eventHandler for="schedJobName" state="unempty"  frequency="1">
                    <wdgt:action target="schedJobNameLabel" removeClassname="has-error"/>
                    <wdgt:action visible="false" target="schedJobNameErr"/>
                </wdgt:eventHandler>
            </g:hasErrors>
        </div>
        %{--group--}%
    %{--<div class="form-group ${hasErrors(bean: scheduledExecution, field: 'groupPath', 'has-error')}">--}%
        %{--<label for="schedJobGroup" class=" ${labelColClass}">--}%
            %{--Group--}%
        %{--</label>--}%
        <div class="${fieldColHalfSize}">
            <div class="input-group">
                <g:hasErrors bean="${scheduledExecution}" field="groupPath">
                    <span class="input-group-addon">
                      <i class="glyphicon glyphicon-warning-sign"></i>
                    </span>
                </g:hasErrors>
                <input type='text' name="groupPath" value="${scheduledExecution?.groupPath?.encodeAsHTML()}"
                       id="schedJobGroup"
                    class="form-control"
                    placeholder="${g.message(code:'scheduledExecution.groupPath.description')}"
                />

                <span class="input-group-btn">
                    <span class="btn btn-default" data-loading-text="Loading..."
                          id="groupChooseBtn" title="Click on the name of the group to use">
                        Choose &hellip; <i class="caret"></i>
                    </span>
                </span>
            </div>

            <script type="text/javascript" src="${resource(dir:'js',file:'yellowfade.js')}"></script>
            <script type="text/javascript">
                function groupChosen(path){
                    $('schedJobGroup').setValue(path);
                    $('schedJobGroup').highlight();
                    jQuery('#groupChooseBtn').popover('destroy');
                }
                function loadGroupChooser(){
                    jQuery('#groupChooseBtn').button('loading');
                    var project = jQuery('#schedEditFrameworkProject').val();
                    jQuery.get('${createLink(controller:"scheduledExecution",action:"groupTreeFragment")}?jscallback=groupChosen&project='+project
                            , function (d) {
                        jQuery('#groupChooseBtn').popover({html:true, container:'body', placement: 'left',content: d,trigger:'manual'}).popover('show');
                        jQuery('#groupChooseBtn').button('reset');
                    });

                }
                jQuery(window).load(function(){
                    jQuery('#groupChooseBtn').click(loadGroupChooser);
                });

            </script>

        </div>
    </div>

        %{--description--}%
    <div class="form-group ${hasErrors(bean: scheduledExecution, field: 'description', 'has-error')}">
        <label for="description" class="${labelColClass}">Description</label>
        <div class="${fieldColSize}">
            <g:textArea name="description" value="${scheduledExecution?.description}" cols="80" rows="3"
                class="form-control"
            />
            <g:hasErrors bean="${scheduledExecution}" field="description">
                <i class="glyphicon glyphicon-warning-sign"></i>
            </g:hasErrors>
        </div>
    </div>
</div><!--/.nput-group-item -->

    <g:set var="projectName" value="${scheduledExecution.project?scheduledExecution.project.toString():projects?.size()==1?projects[0].name:session.project?session.project:''}" />
    <g:hiddenField id="schedEditFrameworkProject" name="project" value="${projectName}" />

    %{--Options--}%
    <div id="optionsContent" class=" list-group-item" >
        <div class="form-group">
            <div class="${labelColSize} control-label text-form-label"><span id="optsload"></span>Options:</div>
            <div class="${fieldColSize}">

                <div  id="editoptssect" class="rounded">
                    <g:render template="/scheduledExecution/detailsOptions" model="${[options:scheduledExecution?.options,edit:true]}"/>
                    <g:if test="${scheduledExecution && scheduledExecution.argString}">
                        <g:render template="/execution/execArgString" model="[argString: scheduledExecution.argString]"/>
                    </g:if>
                    <g:hiddenField name="_sessionopts" value="true"/>
                
                </div>
            </div>
        </div>
    </div>%{--//Options--}%

    %{--Workflow--}%
    <div id="workflowContent" class="list-group-item" >
        <div class="form-group">
            <div class="${labelColSize}  control-label text-form-label">Workflow:</div>
            <div class="${fieldColSize}">
                <g:set var="editwf" value="${session.editWF && session.editWF[scheduledExecution.id.toString()]?session.editWF[scheduledExecution.id.toString()]:scheduledExecution.workflow}"/>
                <g:render template="/execution/execDetailsWorkflow" model="${[workflow:editwf,context:scheduledExecution,edit:true,error:scheduledExecution?.errors?.hasFieldErrors('workflow'),project:scheduledExecution?.project?:projects?.size()==1?projects[0].name:session.project?:'']}"/>
                <g:hiddenField name="_sessionwf" value="true"/>
                <g:if test="${null==editwf || null==editwf.commands || 0==editwf.commands.size()}">
                    <g:javascript>
                        fireWhenReady('workflowContent',function(){
                            $('wfnewtypes').show();
                            $('wfnewbutton').hide();
                        });
                    </g:javascript>
                </g:if>
            </div>
        </div>
    </div>%{--//Workflow--}%

%{--Node Dispatch--}%
<div class="list-group-item" id="nodegroupitem">
<div class="form-group">
    <label class="${labelColSize} control-label">
        Nodes
    </label>

    <div class="${fieldColSize} ">
        <label id="doNodedispatchLabelTrue" class="radio-inline">

            <input type="radio"
                   name="doNodedispatch"
                   value="true"
                ${scheduledExecution?.doNodedispatch ? 'checked' : ''}
                   id="doNodedispatchTrue"
                   onchange="_matchNodes()"/>
            Dispatch to Nodes
        </label>
        <label id="doNodedispatchLabelFalse" class="radio-inline">

            <input type="radio"
                   name="doNodedispatch"
                   value="false"
                ${!scheduledExecution?.doNodedispatch ? 'checked' : ''}
                   id="doNodedispatchFalse"
                   onchange="_matchNodes()"/>
            Execute locally
        </label>
    </div>
</div>

<div class="form-group">
    <div class="${offsetColSize}">
        <span class="help-block">
            Choose whether the Job will on filtered nodes or only run on the local node.
        </span>

        <g:javascript>
            <wdgt:eventHandlerJS for="doNodedispatchTrue" state="unempty" oneway="true">
                <wdgt:action visible="true" targetSelector=".nodeFilterFields"/>
                <wdgt:action visible="true" target="nodeDispatchFields"/>
            </wdgt:eventHandlerJS>
            <wdgt:eventHandlerJS for="doNodedispatchFalse" state="unempty" oneway="true">
                <wdgt:action visible="false" target="nodeDispatchFields"/>
                <wdgt:action visible="false" targetSelector=".nodeFilterFields"/>
            </wdgt:eventHandlerJS>
        </g:javascript>
    </div>
</div>

<div class="form-group">
<div class="${offsetColSize}">
<div style="${wdgt.styleVisible(if: scheduledExecution?.doNodedispatch)}" class="subfields nodeFilterFields container">

<div class="form-group ${hasErrors(bean: scheduledExecution, field: 'nodeInclude', 'has-error')}">
    <span class="h4 ">
        Include Nodes Matching:
    </span>

    <g:render template="/common/nodefilterRegexSyntaxNote"/>
    <div>

        <g:hasErrors bean="${scheduledExecution}" field="nodeInclude">
            <div class="has-error">
                <g:renderErrors bean="${scheduledExecution}" as="list" field="nodeInclude"/>
            </div>
        </g:hasErrors>
        <g:hasErrors bean="${scheduledExecution}" field="nodeInclude">
            <i class="glyphicon glyphicon-warning-sign"></i>
        </g:hasErrors>
        <div id="nodeFilterDivInclude" >
            <g:each var="key" in="${NODE_FILTERS_ALL}">
                <g:set var="predefinedDefaults" value="${g.message(code: 'node.metadata.' + key + '.defaults', default: '')}"/>
                <div id="nodeFilterInclude${key}"
                     style="${wdgt.styleVisible(if: scheduledExecution?.('nodeInclude' + key))}"
                     class="nodefilterfield form-group">
                    <span class="input">
                        <label class="control-label col-sm-2" for="schedJobNodeInclude${key}">${NODE_FILTER_MAP[key] ? NODE_FILTER_MAP[key] : key}:</label>
                        <g:set var="filtvalue"
                               value="${scheduledExecution?.('nodeInclude' + key)?.encodeAsHTML()}"/>
                        <div class="${predefinedDefaults?'col-sm-4':'col-sm-6'}">
                        <g:if test="${filtvalue && filtvalue.length() > 30}">
                            <textarea name="nodeInclude${key}" id="schedJobNodeInclude${key}" class="form-control"
                                      onchange="_matchNodes();"
                                      style="vertical-align:top;"
                                      rows="6" cols="40">${filtvalue}</textarea>
                        </g:if>
                        <g:else>
                            <input type='text' name="nodeInclude${key}" class="filterIncludeText form-control input-sm"
                                   value="${filtvalue}" id="schedJobNodeInclude${key}" onchange="_matchNodes();"/>
                        </g:else>
                    </div>



                        <g:if test="${predefinedDefaults}">
                            <div class="col-sm-2">
                            <g:select from="${predefinedDefaults.split(',').sort()}"
                                      onchange="setFilter('${key}',true,this.value);_matchNodesKeyPress();"
                                class="form-control col-sm-2 input-sm"
                                      name="_${key}defaults"/>
                            </div>
                        </g:if>



                    <div class="col-sm-4">
                        <span title="Remove filter for ${NODE_FILTER_MAP[key] ? NODE_FILTER_MAP[key] : key}"
                              class="filterRemove action textbtn textbtn-danger form-control-static"
                              onclick="removeFilter('${key}', true);">
                            <i class="glyphicon glyphicon-remove"></i>
                            remove
                              </span>
                        </div>
                        <g:javascript>
                                    Event.observe(window,'load',function(){ $('schedJobNodeInclude${key}').onkeypress=_matchNodesKeyPress; });
                        </g:javascript>
                    </span>
                </div>
            </g:each>

        </div>

        <div class="row">
        <div class="col-sm-10 col-sm-offset-2">
            <g:each var="key" in="${NODE_FILTERS}">
                <span
                        style="${wdgt.styleVisible(unless: scheduledExecution?.('nodeInclude' + key))}"
                        title="Add Filter for ${NODE_FILTER_MAP[key] ? NODE_FILTER_MAP[key] : key}"
                        class="filterAdd btn btn-default btn-sm"
                        id="filterAddInclude${key}"
                        onclick="addFilter('${key}', true, '${NODE_FILTER_MAP[key]?NODE_FILTER_MAP[key]:key}');">
                    <i class="glyphicon glyphicon-plus"></i>
                    ${NODE_FILTER_MAP[key] ? NODE_FILTER_MAP[key] : key}
                </span>
            </g:each>
            <span class="filterAdd button textbtn action" onclick="Element.show('${rkey}moreIncludeFilters');
            Element.hide(this);">more&hellip;</span>
        </div>

        <div id="${rkey}moreIncludeFilters" style="display:none;" class="col-sm-10 col-sm-offset-2">
            <g:each var="key" in="${NODE_FILTERS_X}">
                <span
                        style="${wdgt.styleVisible(unless: scheduledExecution?.('nodeInclude' + key))}"
                        title="Add Filter for ${NODE_FILTER_MAP[key] ? NODE_FILTER_MAP[key] : key}"
                        class="filterAdd btn btn-default btn-sm"
                        id="filterAddInclude${key}"
                        onclick="addFilter('${key}', true, '${NODE_FILTER_MAP[key]?NODE_FILTER_MAP[key]:key}');">
                    <i class="glyphicon glyphicon-plus"></i>
                    ${NODE_FILTER_MAP[key] ? NODE_FILTER_MAP[key] : key}
                </span>
            </g:each>
        </div>
        </div>
    </div>
</div>%{--//include filters--}%
<div class="form-group">
    <div>
    </div>

    <div>
        <g:expander key="extNodeFilters">Extended Filters&hellip;</g:expander>
    </div>
</div>%{--//extended filters toggle--}%
<div style="display:none" class="subfields" id="extNodeFilters">
    <div class="form-group">
        <span class="h4 ${hasErrors(bean: scheduledExecution, field: 'nodeExclude', 'has-error')}">
            Exclude Nodes Matching:
        </span>

        <div>
            <div>
                <g:hasErrors bean="${scheduledExecution}" field="nodeExclude">
                    <div class="has-error">
                        <g:renderErrors bean="${scheduledExecution}" as="list" field="nodeExclude"/>
                    </div>
                </g:hasErrors>
                <g:hasErrors bean="${scheduledExecution}" field="nodeExclude">
                    <i class="glyphicon glyphicon-warning-sign"></i>
                </g:hasErrors>
            </div>

            <div id="nodeFilterDivExclude">
                <g:each var="key" in="${NODE_FILTERS_ALL}">
                    <g:set var="predefinedDefaults" value="${g.message(code: 'node.metadata.' + key + '.defaults', default: '')}"/>
                    <div id="nodeFilterExclude${key}"
                         style="${wdgt.styleVisible(if: scheduledExecution?.('nodeExclude' + key))}"
                         class="nodefilterfield form-group">
                        <span class="input">
                            <label class="control-label col-sm-2" for="schedJobNodeExclude${key}">${NODE_FILTER_MAP[key] ? NODE_FILTER_MAP[key] : key}:</label>

                            <div class="${predefinedDefaults ? 'col-sm-4' : 'col-sm-6'}">
                            <g:set var="filtvalue"
                                       value="${scheduledExecution?.('nodeExclude' + key)?.encodeAsHTML()}"/>
                                <g:if test="${filtvalue && filtvalue.length() > 30}">
                                    <textarea name="nodeExclude${key}" id="schedJobNodeExclude${key}"
                                              onchange="_matchNodes();"
                                              style="vertical-align:top;"
                                              rows="6" cols="40">${filtvalue}</textarea>
                                </g:if>
                                <g:else>
                                    <input type='text' name="nodeExclude${key}"
                                           value="${filtvalue}" id="schedJobNodeExclude${key}"
                                        class="form-control input-sm"
                                           onchange="_matchNodes();"/>
                                </g:else>

                            <g:javascript>
                                    Event.observe(window,'load',function(){ $('schedJobNodeExclude${key}').onkeypress=_matchNodesKeyPress; });
                            </g:javascript>
                            </div>
                        </span>
                        <g:if test="${predefinedDefaults}">
                            <div class="col-sm-2">
                            <g:select from="${predefinedDefaults.split(',').sort()}"
                                      onchange="setFilter('${key}',false,this.value);_matchNodesKeyPress();"
                                class="form-control  input-sm"
                                      name="_${key}defaults"/>
                            </div>
                        </g:if>
                        <div class="col-sm-4">
                        <span class="filterRemove action textbtn textbtn-danger form-control-static"
                              onclick="removeFilter('${key}', false);">
                                <i class="glyphicon glyphicon-remove"></i>
                            remove
                              </span>
                        </div>
                    </div>
                </g:each>

            </div>

            <div class="row">
            <div class="col-sm-10 col-sm-offset-2">
                <g:each var="key" in="${NODE_FILTERS_ALL}">
                    <span
                            style="${wdgt.styleVisible(unless: scheduledExecution?.('nodeExclude' + key))}"
                            title="Add Filter: ${NODE_FILTER_MAP[key] ? NODE_FILTER_MAP[key] : key}"
                            class="filterAdd btn btn-default btn-sm"
                            id="filterAddExclude${key}"
                            onclick="addFilter('${key}', false, '${NODE_FILTER_MAP[key]?NODE_FILTER_MAP[key]:key}');">
                        <i class="glyphicon glyphicon-plus"></i>
                        ${NODE_FILTER_MAP[key] ? NODE_FILTER_MAP[key] : key}
                    </span>
                </g:each>
            </div>
            </div>
        </div>
    </div>

    <div class="form-group">
        <label class="col-sm-2  control-label">Precedence to:</label>

        <div class="col-sm-10">
            <label title="Include more nodes" class="radio-inline">
                <g:radio name="nodeExcludePrecedence" value="false"
                         checked="${!scheduledExecution?.nodeExcludePrecedence}"
                         id="nodeExcludePrecedenceFalse" onchange="_matchNodes()"/>
                Included</label>

            <label title="Exclude more nodes" class="radio-inline">
                <g:radio name="nodeExcludePrecedence" value="true"
                         checked="${scheduledExecution?.nodeExcludePrecedence}"
                         id="nodeExcludePrecedenceTrue" onchange="_matchNodes()"/>
                Excluded</label>
        </div>
    </div>
</div>%{--//extended filters--}%

<div style="${wdgt.styleVisible(if: scheduledExecution?.doNodedispatch)}" class="subfields nodeFilterFields">
    <h4>Matched Nodes:</h4>
    <div class="row">
        <div onclick="_formUpdateMatchedNodes()" class="col-sm-2">
            <span class="btn btn-info btn-sm" title="click to refresh">
                refresh
                <i class="glyphicon glyphicon-refresh"></i>
            </span>
            <span id="mnodeswait"></span>
        </div>

        <div id="matchednodes" class=" col-sm-10 embed matchednodes well">
            <span class="action textbtn" onclick="_formUpdateMatchedNodes()">Update...</span>
        </div>
    </div>


    <div id="nodeDispatchFields" style="${wdgt.styleVisible(if: scheduledExecution?.doNodedispatch)} "
         class="subfields">
        <h4>Dispatch Options</h4>

        <div class="form-group ${hasErrors(bean: scheduledExecution, field: 'nodeThreadcount', 'has-error')}">
            <label for="schedJobnodeThreadcount" class="${labelColClass}">
                <g:message code="scheduledExecution.property.nodeThreadcount.label"/>
            </label>

            <div class="${fieldColSize}">
                <div class="row">
                <div class="col-sm-4">
                <input type='text' name="nodeThreadcount"
                       value="${scheduledExecution?.nodeThreadcount?.encodeAsHTML()}" id="schedJobnodeThreadcount"
                       size="3"
                       class="form-control input-sm"/>
                </div>
                </div>
                <g:hasErrors bean="${scheduledExecution}" field="nodeThreadcount">
                    <div class="text-warning">
                        <i class="glyphicon glyphicon-warning-sign"></i>
                        <g:renderErrors bean="${scheduledExecution}" as="list" field="nodeThreadcount"/>
                    </div>
                </g:hasErrors>
                <span class="help-block">
                    <g:message code="scheduledExecution.property.nodeThreadcount.description"/>
                </span>

            </div>
        </div>

        <div class="form-group ${hasErrors(bean: scheduledExecution, field: 'nodeRankAttribute', 'has-error')}">
            <label for="schedJobnodeRankAttribute" class="${labelColClass}">
                <g:message code="scheduledExecution.property.nodeRankAttribute.label"/>
            </label>

            <div class="${fieldColSize}">
                <div class="row">
                    <div class="col-sm-4">
                <input type='text' name="nodeRankAttribute"
                       value="${scheduledExecution?.nodeRankAttribute?.encodeAsHTML()}" id="schedJobnodeRankAttribute"
                       class="form-control input-sm"/>
                    </div>
                </div>
                <g:hasErrors bean="${scheduledExecution}" field="nodeRankAttribute">
                    <div class="text-warning">
                        <i class="glyphicon glyphicon-warning-sign"></i>
                        <g:renderErrors bean="${scheduledExecution}" as="list" field="nodeRankAttribute"/>
                    </div>
                </g:hasErrors>
                <span class="help-block">
                    <g:message code="scheduledExecution.property.nodeRankAttribute.description"/>
                </span>
            </div>
        </div>

        <div class="form-group">
            <label class="${labelColClass}">
                <g:message code="scheduledExecution.property.nodeRankOrder.label"/>
            </label>

            <div class="${fieldColSize}">
                <label class="radio-inline">
                    <g:radio name="nodeRankOrderAscending" value="true"
                             checked="${scheduledExecution?.nodeRankOrderAscending || null == scheduledExecution?.nodeRankOrderAscending}"
                             id="nodeRankOrderAscending"/>
                    <g:message code="scheduledExecution.property.nodeRankOrder.ascending.label"/></label>
                <label class="radio-inline">
                    <g:radio name="nodeRankOrderAscending" value="false"
                             checked="${!scheduledExecution?.nodeRankOrderAscending && null != scheduledExecution?.nodeRankOrderAscending}"
                             id="nodeRankOrderDescending"/>
                    <g:message code="scheduledExecution.property.nodeRankOrder.descending.label"/></label>

            </div>
        </div>

        <div class="form-group">
            <label class="${labelColClass}"><g:message code="scheduledExecution.property.nodeKeepgoing.prompt"/></label>

            <div class="${fieldColSize}">
                <div class="radio">
                    <label>
                        <g:radio name="nodeKeepgoing"
                                 value="false"

                                 checked="${!scheduledExecution?.nodeKeepgoing}"

                                 id="nodeKeepgoingFalse"/>
                        <g:message code="scheduledExecution.property.nodeKeepgoing.false.description"/>
                    </label>
                </div>

                <div class="radio">
                    <label>
                        <g:radio
                                name="nodeKeepgoing"
                                value="true"

                                checked="${scheduledExecution?.nodeKeepgoing}"

                                id="nodeKeepgoingTrue"/>
                        <g:message code="scheduledExecution.property.nodeKeepgoing.true.description"/>
                    </label>
                </div>
            </div>
        </div>
    </div>
</div>
</div>
</div>
</div>
</div>%{--//Node Dispatch--}%

    %{--Notifications--}%
    <div class="list-group-item"  >
            <g:set var="adminauth"
                value="${auth.resourceAllowedTest(type: 'project', name: session.project, action: [AuthConstants.ACTION_ADMIN, AuthConstants.ACTION_READ], context: 'application')}"/>

        <g:render template="editNotificationsForm" model="[scheduledExecution:scheduledExecution, notificationPlugins: notificationPlugins,adminauth:adminauth]"/>

    </div>%{--//Notifications--}%

%{--Schedule--}%
<div class="list-group-item">

    <div class="form-group">
        <div class="${labelColSize}  control-label text-form-label">
            Schedule to run repeatedly?
        </div>
        <div class="${fieldColSize}">

            <label class="radio-inline">
            <g:radio value="false" name="scheduled"
                checked="${scheduledExecution?.scheduled?false:true}"
                id="scheduledFalse"/>
                No
            </label>

            <label class="radio-inline">
                <g:radio name="scheduled" value="true"
                    checked="${scheduledExecution?.scheduled}"
                    id="scheduledTrue"/>
                Yes
            </label>
        </div>
        <div class="${offsetColSize}" style="${wdgt.styleVisible(if:scheduledExecution?.scheduled)}" id="scheduledExecutionEditCrontab">
            <g:render template="editCrontab" model="[scheduledExecution:scheduledExecution, crontab:crontab]"/>
        </div>
            <g:javascript>
                <wdgt:eventHandlerJS for="scheduledTrue" state="unempty">
                    <wdgt:action visible="true" targetSelector="#scheduledExecutionEditCrontab"/>
                </wdgt:eventHandlerJS>
                <wdgt:eventHandlerJS for="scheduledFalse" state="unempty" >
                    <wdgt:action visible="false" target="scheduledExecutionEditCrontab"/>
                </wdgt:eventHandlerJS>
            </g:javascript>
    </div>
</div>%{--//Schedule--}%


%{--Log level--}%
<div class="list-group-item">
    <div class="form-group">
        <label class="${labelColClass}" for="loglevel">Log level</label>
        <div class="${fieldColSize}">
            <label class="radio-inline">
                <g:radio name="loglevel" value="INFO" checked="${scheduledExecution?.loglevel != 'DEBUG'}"/>
                Normal
            </label>
            <label class="radio-inline">
                <g:radio name="loglevel" value="DEBUG" checked="${scheduledExecution?.loglevel == 'DEBUG'}"/>
                Debug
            </label>
            <div class="help-block">
                Debug level produces more output
            </div>
        </div>
    </div>
    %{--multiple exec--}%
    <div class="form-group">
        <div class="${labelColSize} control-label text-form-label">
            <g:message code="scheduledExecution.property.multipleExecutions.label"/>
        </div>

        <div class="${fieldColSize}">
            <label class="radio-inline">
                <g:radio value="false" name="multipleExecutions"
                         checked="${!scheduledExecution.multipleExecutions}"
                         id="multipleFalse"/>
                <g:message code="no"/>
            </label>

            <label class="radio-inline">
                <g:radio name="multipleExecutions" value="true"
                         checked="${scheduledExecution.multipleExecutions}"
                         id="multipleTrue"/>
                <g:message code="yes"/>
            </label>

            <span class="help-block">
                <g:message code="scheduledExecution.property.multipleExecutions.description"/>
            </span>
        </div>
    </div>


    %{--uuid--}%
    <div class="form-group ${hasErrors(bean: scheduledExecution, field: 'uuid', 'has-error')}" id="schedJobUuidLabel">
        <label for="schedJobUuid" class=" ${labelColClass} text-muted">
            UUID
        </label>

        <div class="${fieldColSize}">
            <g:if test="${editSchedExecId && scheduledExecution?.uuid}">
                <p class="form-control-static text-muted" title="UUID for this Job">
                    ${scheduledExecution?.uuid?.encodeAsHTML()}
                </p>
            </g:if>
            <g:else>
                <input type='text' name="uuid" value="${scheduledExecution?.uuid?.encodeAsHTML()}"
                       id="schedJobUuid" size="36" class="form-control"/>
                <g:hasErrors bean="${scheduledExecution}" field="uuid">
                    <i class="glyphicon glyphicon-warning-sign" id="schedJobUuidErr"></i>
                    <wdgt:eventHandler for="schedJobUuid" state="unempty" frequency="1">
                        <wdgt:action target="schedJobUuidLabel" removeClassname="has-error"/>
                        <wdgt:action visible="false" target="schedJobUuidErr"/>
                    </wdgt:eventHandler>
                </g:hasErrors>
            </g:else>
        </div>
    </div>
</div>%{--//Log level--}%


<g:javascript>
    if (typeof(initTooltipForElements) == 'function') {
        initTooltipForElements('.obs_tooltip');
    }
</g:javascript>
<!--[if (gt IE 8)|!(IE)]><!--> <g:javascript library="ace/ace"/><!--<![endif]-->
<div id="msg"></div>
</div>
