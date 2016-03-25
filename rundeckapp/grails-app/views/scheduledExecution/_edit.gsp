<%@ page import="rundeck.User; com.dtolabs.rundeck.server.authorization.AuthConstants" %>

<g:jsonToken id="job_edit_tokens" url="${request.forwardURI}"/>
<div class="list-group">
<g:if test="${flash.message}">
    <div class="list-group-item">
    <div class="alert alert-info"><g:enc>${flash.message}</g:enc></div>
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
<g:set var="fieldColShortSize" value="col-sm-4"/>
<g:set var="offsetColSize" value="col-sm-10 col-sm-offset-2"/>

<g:set var="editSchedExecId" value="${scheduledExecution?.id? scheduledExecution.extid:null}"/>
<g:javascript library="prototype/scriptaculous"/>
<g:javascript library="prototype/effects"/>
<g:javascript library="prototype/dragdrop"/>
<g:set var="project" value="${scheduledExecution?.project ?: params.project?:request.project?: projects?.size() == 1 ? projects[0].name : ''}"/>
<script type="text/javascript">
//<!CDATA[
        var selFrameworkProject='${enc(js:project)}';
        var selArgs='${enc(js:scheduledExecution?.argString)}';
var curSEID ='${enc(js:editSchedExecId?:"")}';
function getCurSEID(){
    return curSEID;
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
        function _updateBoxInfo(name, data) {

        }
        function setupUndoRedoControls(){
            jQuery('.undoredocontrols').on('click','.act_undo',function(e){
                _doUndoAction(jQuery(e.target).data('undo-key'));
            }).on('click','.act_redo',function(e){
                _doRedoAction(jQuery(e.target).data('undo-key'));
            }).on('click','.act_revert_popover',function(e){
                _initPopoverContentRef("#undoredo"+ jQuery(e.target).data('popover-key'));
                jQuery(e.target).popover('show');
            });
            jQuery('body').on('click','.act_revert_cancel',function(e){
                jQuery('#revertall_'+ jQuery(e.target).data('popover-key')).popover('hide');
            }).on('click','.act_revert_confirm',function(e){
                jQuery('#revertall_'+jQuery(e.target).data('popover-key')).popover('destroy');
                _doRevertAction(jQuery(e.target).data('undo-key'));
            });
        }
        var nodeFilter;
        var nodeFilterMap = {};
        function registerNodeFilters(obj,key){
            nodeFilterMap[key]=obj;
        }
        function handleNodeFilterLink(link){
            var holder = jQuery(link).parents('.node_filter_link_holder');
            var nflinkid=holder.data('node-filter-link-id');
            var nflinkid2=holder.attr('id');
            if(nflinkid && nodeFilterMap[nflinkid]){
                nodeFilterMap[nflinkid].selectNodeFilterLink(link);
            }else if(nflinkid2 && nodeFilterMap['#'+nflinkid2]){
                nodeFilterMap['#'+nflinkid2].selectNodeFilterLink(link);
            }else{
                nodeFilter.selectNodeFilterLink(link);
            }
        }
        function setupJobExecNodeFilterBinding(root,target,dataId){
            var filterParams = loadJsonData(dataId);
            var nodeSummary = new NodeSummary({baseUrl:appLinks.frameworkNodes});
            var jobRefNodeFilter = new NodeFilters(
                    appLinks.frameworkAdhoc,
                    appLinks.scheduledExecutionCreate,
                    appLinks.frameworkNodes,
                    Object.extend(filterParams, {
                        nodeSummary:nodeSummary,
                        nodefilterLinkId:root,
                        project: selFrameworkProject,
                        maxShown:20,
                        view: 'embed',
                        emptyMode: 'blank',
                        emptyMessage: "${g.message(code: 'JobExec.property.nodeFilter.null.description')}",
                        nodesTitleSingular: "${g.message(code: 'Node', default: 'Node')}",
                        nodesTitlePlural: "${g.message(code: 'Node.plural', default: 'Nodes')}"
                    })
            );
            ko.applyBindings(jobRefNodeFilter, jQuery(root)[0]);
            registerNodeFilters(jobRefNodeFilter,root);
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
            setupUndoRedoControls();

            //define NodeFilters mvvm for the job
            var filterParams = loadJsonData('filterParamsJSON');
            var nodeSummary = new NodeSummary({baseUrl:appLinks.frameworkNodes});
            nodeFilter = new NodeFilters(
                    appLinks.frameworkAdhoc,
                    appLinks.scheduledExecutionCreate,
                    appLinks.frameworkNodes,
                    Object.extend(filterParams, {
                        nodeSummary:nodeSummary,
                        maxShown:100,
                        nodefilterLinkId: '#nodegroupitem',
                         project: selFrameworkProject,
                         view:'embed',
                        nodesTitleSingular: "${g.message(code:'Node',default:'Node')}",
                        nodesTitlePlural: "${g.message(code:'Node.plural',default:'Nodes')}"
                    })
            );
            ko.applyBindings(nodeFilter,jQuery('#nodegroupitem')[0]);
            registerNodeFilters(nodeFilter, '#nodegroupitem');

            jQuery('body').on('click', '.nodefilterlink', function (evt) {
                evt.preventDefault();
                handleNodeFilterLink(this);
            })
            .on('change','.node_dispatch_radio',function(evt){
                nodeFilter.updateMatchedNodes();
            })
            ;
        }

        jQuery(pageinit);
//]>
</script>
<g:embedJSON id="filterParamsJSON"
             data="${[filterName: params.filterName, filter: scheduledExecution?.asFilter(),nodeExcludePrecedence: scheduledExecution?.nodeExcludePrecedence]}"/>
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

<g:if test="${scheduledExecution && scheduledExecution.id}">
    <input type="hidden" name="id" value="${enc(attr:scheduledExecution.extid)}"/>
</g:if>


<div class="alert alert-danger" style="display: none" id="editerror">

</div>

    <div class="list-group-item"  >
        %{--name--}%
    <div class="form-group ${g.hasErrors(bean:scheduledExecution,field:'jobName','has-error')}" id="schedJobNameLabel">
        <label for="schedJobName"
               class="required ${enc(attr:labelColClass)}"
               >
            <g:message code="scheduledExecution.jobName.label" />
        </label>
        <div class="${fieldColHalfSize}">
            <g:textField name="jobName"
                         value="${scheduledExecution?.jobName}"
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

        <div class="${fieldColHalfSize}">
            <div class="input-group">
                <g:hasErrors bean="${scheduledExecution}" field="groupPath">
                    <span class="input-group-addon">
                      <i class="glyphicon glyphicon-warning-sign"></i>
                    </span>
                </g:hasErrors>
                <input type='text' name="groupPath" value="${enc(attr:scheduledExecution?.groupPath)}"
                       id="schedJobGroup"
                    class="form-control"
                    placeholder="${g.message(code:'scheduledExecution.groupPath.description')}"
                />

                <span class="input-group-btn">
                    <span class="btn btn-default" data-loading-text="Loading..."
                          id="groupChooseBtn" title="Click on the name of the group to use">
                        <g:message code="choose.action.label" /> <i class="caret"></i>
                    </span>
                </span>
            </div>

            <script type="text/javascript" src="${resource(dir:'js',file:'yellowfade.js')}"></script>
        </div>
    </div>

        %{--description--}%
    <div class="form-group ${hasErrors(bean: scheduledExecution, field: 'description', 'has-error')}">
        <label for="description" class="${labelColClass}"><g:message code="scheduledExecution.property.description.label" /></label>
        <div class="${fieldColSize}">
            <g:textArea name="description"
                        value="${scheduledExecution?.description}"
                        cols="80"
                        rows="3"
                        class="form-control ace_editor"
                        data-ace-session-mode="markdown"
                        data-ace-height="120px"
            />
            <g:hasErrors bean="${scheduledExecution}" field="description">
                <i class="glyphicon glyphicon-warning-sign text-warning"></i>
            </g:hasErrors>
            <g:set var="allowHTML"
                   value="${!(grailsApplication.config.rundeck?.gui?.job?.description?.disableHTML in [true, 'true'])}"/>
            <div class="help-block">
                <g:if test="${allowHTML}">
                    <g:message code="ScheduledExecution.property.description.description"/>
                    <a href="http://en.wikipedia.org/wiki/Markdown" target="_blank" class="text-info">
                        <i class="glyphicon glyphicon-question-sign"></i>
                    </a>
                </g:if>
                <g:else>
                    <g:message code="ScheduledExecution.property.description.plain.description"/>
                </g:else>
            </div>
            <g:javascript>
            jQuery(function(){
                jQuery('textarea.ace_editor').each(function(){
                    _addAceTextarea(this);
                });
            });
            </g:javascript>
        </div>
    </div>
</div><!--/.nput-group-item -->

    <g:set var="projectName" value="${scheduledExecution.project?scheduledExecution.project.toString():params.project ?: request.project?: projects?.size() == 1 ? projects[0].name : ''}" />
    <g:hiddenField id="schedEditFrameworkProject" name="project" value="${projectName}" />

    %{--Options--}%
    <div id="optionsContent" class=" list-group-item" >
        <div class="form-group">
            <div class="${labelColSize} control-label text-form-label"><span id="optsload"></span><g:message code="options.prompt" /></div>
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
            <div class="${labelColSize}  control-label text-form-label"><g:message code="workflow.prompt" /></div>
            <div class="${fieldColSize}">
                <g:set var="editwf" value="${session.editWF && session.editWF[scheduledExecution.id.toString()]?session.editWF[scheduledExecution.id.toString()]:scheduledExecution.workflow}"/>
                <g:render template="/execution/execDetailsWorkflow" model="${[workflow:editwf,context:scheduledExecution,edit:true,error:scheduledExecution?.errors?.hasFieldErrors('workflow'),project:scheduledExecution?.project?:(params.project ?: request.project)?: projects?.size() == 1 ? projects[0].name :'']}"/>
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
<div class="list-group-item node_filter_link_holder" id="nodegroupitem">
<div class="form-group">
    <label class="${labelColSize} control-label">
        <g:message code="Node.plural" />
    </label>

    <div class="${fieldColSize} ">
        <label id="doNodedispatchLabelTrue" class="radio-inline">

            <input type="radio"
                   name="doNodedispatch"
                   value="true"
                    class="node_dispatch_radio"
                ${scheduledExecution?.doNodedispatch ? 'checked' : ''}
                   id="doNodedispatchTrue"/>
            <g:message code="dispatch.to.nodes" />
        </label>
        <label id="doNodedispatchLabelFalse" class="radio-inline">

            <input type="radio"
                   name="doNodedispatch"
                   value="false"
                   class="node_dispatch_radio"
                ${!scheduledExecution?.doNodedispatch ? 'checked' : ''}
                   id="doNodedispatchFalse"/>
            <g:message code="execute.locally" />
        </label>
    </div>
</div>

<div class="form-group">
    <div class="${offsetColSize}">
        <span class="help-block">
            <g:message code="scheduledExecution.property.doNodedispatch.description" />
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

<div class="form-group  ${hasErrors(bean: scheduledExecution, field: 'filter', 'has-error')}">
<div style="${wdgt.styleVisible(if: scheduledExecution?.doNodedispatch)}" class="subfields nodeFilterFields ">
    <label class="${labelColSize} control-label">
        <g:message code="node.filter" />
    </label>

    <div class="${fieldColSize}">
        <g:hiddenField name="formInput" value="true"/>
        <g:hasErrors bean="${scheduledExecution}" field="filter">

            <div class="text-warning">
                <g:renderErrors bean="${scheduledExecution}" as="list" field="filter"/>
                <i class="glyphicon glyphicon-warning-sign"></i>
            </div>

        </g:hasErrors>
        <g:set var="filtvalue" value="${scheduledExecution.asFilter()}"/>

                <span class="input-group nodefilters">
                    <g:if test="${session.user && User.findByLogin(session.user)?.nodefilters}">
                        <g:set var="filterset" value="${User.findByLogin(session.user)?.nodefilters}"/>
                    </g:if>
                    <g:render template="/framework/nodeFilterInputGroup"
                              model="[filterset: filterset, filtvalue: filtvalue, filterName: filterName]"/>
                </span>

        <div class=" collapse" id="queryFilterHelp">
            <div class="help-block">
                <g:render template="/common/nodefilterStringHelp"/>
            </div>
        </div>


    </div>


</div>
</div>

<div style="${wdgt.styleVisible(if: scheduledExecution?.doNodedispatch)}" class="subfields nodeFilterFields ">
<g:if test="${grailsApplication.config.rundeck?.nodefilters?.showPrecedenceOption || scheduledExecution?.nodeExcludePrecedence!=null && !scheduledExecution?.nodeExcludePrecedence }">

    <div class="form-group ${hasErrors(bean: scheduledExecution, field: 'nodeInclude', 'has-error')}">

        <label class="col-sm-2  control-label"><g:message code="precedence.to.prompt" /></label>

        <div class="col-sm-10">
            <label title="Include more nodes" class="radio-inline">
                <g:radio name="nodeExcludePrecedence" value="false"
                         data-bind="checked: nodeExcludePrecedence"
                         checked="${!scheduledExecution?.nodeExcludePrecedence}"
                         id="nodeExcludePrecedenceFalse"/>
                <g:message code="included" /></label>

            <label title="Exclude more nodes" class="radio-inline">
                <g:radio name="nodeExcludePrecedence" value="true"
                         data-bind="checked: nodeExcludePrecedence"
                         checked="${scheduledExecution?.nodeExcludePrecedence}"
                         id="nodeExcludePrecedenceTrue"/>
                <g:message code="excluded" /></label>
        </div>
    </div>%{--//extended filters--}%

</g:if>
<g:else>
    <g:hiddenField name="nodeExcludePrecedence" value="true"/>
</g:else>

<div class="subfields nodeFilterFields">

    <div class="form-group">
        <label class="${labelColClass}">
            <g:message code="matched.nodes.prompt" />
        </label>

        <div class=" col-sm-10  ">

            <div class="well well-sm embed matchednodes">
                <button type="button" class="pull-right btn btn-info btn-sm refresh_nodes"
                        data-loading-text="${g.message(code:'loading')}"
                    data-bind="click: $data.updateMatchedNodes"
                        title="${g.message(code:'click.to.refresh')}">
                    <g:message code="refresh" />
                    <i class="glyphicon glyphicon-refresh"></i>
                </button>
                <span class="text-muted" data-bind="if: loaded" >
                    <span data-bind="messageTemplate: [total,nodesTitle]"><g:message code="count.nodes.matched"/></span>
                </span>
                <div id='matchednodes' class="clearfix">
                    <g:render template="/framework/nodesEmbedKO" model="[showLoading:true,showTruncated:true]"/>
                </div>
            </div>
        </div>
    </div>


    <div id="nodeDispatchFields" class="subfields ">


        <div class="form-group ${hasErrors(bean: scheduledExecution, field: 'nodeThreadcount', 'has-error')}">
            <label for="schedJobnodeThreadcount" class="${labelColClass}">
                <g:message code="scheduledExecution.property.nodeThreadcount.label"/>
            </label>

            <div class="${fieldColSize}">
                <div class="row">
                <div class="col-sm-4">
                <input type='number' name="nodeThreadcount"
                       value="${enc(attr:scheduledExecution?.nodeThreadcount)}" id="schedJobnodeThreadcount"
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
                       value="${enc(attr:scheduledExecution?.nodeRankAttribute)}" id="schedJobnodeRankAttribute"
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

        <div class="form-group">
            <label class="${labelColClass}"><g:message code="scheduledExecution.property.nodesSelectedByDefault.label"/></label>

            <div class="${fieldColSize}">
                <div class="radio">
                    <label>
                        <g:radio
                                name="nodesSelectedByDefault"
                                value="true"
                                checked="${scheduledExecution.nodesSelectedByDefault==null||scheduledExecution.nodesSelectedByDefault}"
                                id="nodesSelectedByDefaultTrue"/>
                        <g:message code="scheduledExecution.property.nodesSelectedByDefault.true.description"/>
                    </label>
                </div>
                <div class="radio">
                    <label>
                        <g:radio name="nodesSelectedByDefault"
                                 value="false"
                                 checked="${scheduledExecution.nodesSelectedByDefault!=null && !scheduledExecution.nodesSelectedByDefault}"
                                 id="nodesSelectedByDefaultFalse"/>
                        <g:message code="scheduledExecution.property.nodesSelectedByDefault.false.description"/>
                    </label>
                </div>
            </div>
        </div>

        %{--orchestrator--}%
        <g:render template="editOrchestratorForm" model="[scheduledExecution:scheduledExecution, orchestratorPlugins: orchestratorPlugins,adminauth:adminauth]"/>
        %{--//orchestrator--}%
    </div>
</div>

</div>%{--//Node Dispatch--}%
</div>

    %{--Notifications--}%
    <div class="list-group-item"  >
            <g:set var="adminauth"
                value="${auth.resourceAllowedTest(type: 'project', name: scheduledExecution.project, action: [AuthConstants.ACTION_ADMIN, AuthConstants.ACTION_READ], context: 'application')}"/>

        <g:render template="editNotificationsForm" model="[scheduledExecution:scheduledExecution, notificationPlugins: notificationPlugins,adminauth:adminauth]"/>

    </div>%{--//Notifications--}%

%{--Schedule--}%
<div class="list-group-item">

    <div class="form-group">
        <div class="${labelColSize}  control-label text-form-label">
            <g:message code="schedule.to.run.repeatedly" />
        </div>
        <div class="${fieldColSize}">

            <label class="radio-inline">
            <g:radio name="scheduled" value="false"
                checked="${scheduledExecution?.scheduled?false:true}"
                id="scheduledFalse"/>
                <g:message code="no" />
            </label>

            <label class="radio-inline">
                <g:radio name="scheduled" value="true"
                    checked="${scheduledExecution?.scheduled}"
                    id="scheduledTrue"/>
                <g:message code="yes" />
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
    %{-- scheduleEnabled --}%
    <g:if test="${auth.jobAllowedTest(job: scheduledExecution, action: AuthConstants.ACTION_TOGGLE_SCHEDULE)}">
        <div class="form-group">
            <div class="${labelColSize} control-label text-form-label">
                <g:message code="scheduledExecution.property.scheduleEnabled.label"/>
            </div>

            <div class="${fieldColSize}">
                <label class="radio-inline">
                    <g:radio name="scheduleEnabled"
                             value="true"
                             checked="${scheduledExecution.hasScheduleEnabled()}"
                             id="scheduleEnabledTrue"/>
                    <g:message code="yes"/>
                </label>

                <label class="radio-inline">
                    <g:radio value="false"
                             name="scheduleEnabled"
                             checked="${!scheduledExecution.hasScheduleEnabled()}"
                             id="scheduleEnabledFalse"/>
                    <g:message code="no"/>
                </label>

                <span class="help-block">
                    <g:message code="scheduledExecution.property.scheduleEnabled.description"/>
                </span>
            </div>

        </div>
    </g:if>
    %{-- executionEnabled --}%
    <g:if test="${auth.jobAllowedTest(job: scheduledExecution, action: AuthConstants.ACTION_TOGGLE_EXECUTION)}">
        <div class="form-group">
            <div class="${labelColSize} control-label text-form-label">
                <g:message code="scheduledExecution.property.executionEnabled.label"/>
            </div>

            <div class="${fieldColSize}">
                <label class="radio-inline">
                    <g:radio name="executionEnabled" value="true"
                             checked="${scheduledExecution.hasExecutionEnabled()}"
                             id="executionEnabledTrue"/>
                    <g:message code="yes"/>
                </label>

                <label class="radio-inline">
                    <g:radio value="false" name="executionEnabled"
                             checked="${!scheduledExecution.hasExecutionEnabled()}"
                             id="executionEnabledFalse"/>
                    <g:message code="no"/>
                </label>

                <span class="help-block">
                    <g:message code="scheduledExecution.property.executionEnabled.description"/>
                </span>
            </div>
        </div>
    </g:if>
</div>%{--//Schedule--}%


%{--Log level--}%
<div class="list-group-item">
    <div class="form-group">
        <label class="${labelColClass}" for="loglevel"><g:message code="scheduledExecution.property.loglevel.label" /></label>
        <div class="${fieldColSize}">
            <label class="radio-inline">
                <g:radio name="loglevel" value="INFO" checked="${scheduledExecution?.loglevel != 'DEBUG'}"/>
                <g:message code="loglevel.normal" />
            </label>
            <label class="radio-inline">
                <g:radio name="loglevel" value="DEBUG" checked="${scheduledExecution?.loglevel == 'DEBUG'}"/>
                <g:message code="loglevel.debug" />
            </label>
            <div class="help-block">
                <g:message code="scheduledExecution.property.loglevel.help" />
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
    %{--Job timeout--}%
    <div class="form-group">
        <div class="${labelColSize} control-label text-form-label">
            <g:message code="scheduledExecution.property.timeout.label" default="Timeout"/>
        </div>

        <div class="${fieldColHalfSize}">

            <input type='text' name="timeout" value="${enc(attr:scheduledExecution?.timeout)}"
                   id="schedJobTimeout" class="form-control"/>

            <span class="help-block">
                <g:message code="scheduledExecution.property.timeout.description"/>
            </span>
        </div>
    </div>
    %{--Job retry--}%
    <div class="form-group ${hasErrors(bean: scheduledExecution, field: 'retry', 'has-error')}">
        <div class="${labelColSize} control-label">
            <label for="schedJobRetry"><g:message code="scheduledExecution.property.retry.label" default="Retry"/></label>
        </div>

        <div class="${fieldColHalfSize}">

            <input type='text' name="retry" value="${enc(attr:scheduledExecution?.retry)}"
                   id="schedJobRetry" class="form-control"/>
            <g:hasErrors bean="${scheduledExecution}" field="retry">

                <div class="text-danger">
                    <g:renderErrors bean="${scheduledExecution}" as="list" field="retry"/>
                </div>
            </g:hasErrors>
            <span class="help-block">
                <g:message code="scheduledExecution.property.retry.description"/>
            </span>
        </div>
    </div>
    %{--log limit--}%
    <div class="form-group">
        <label class="${labelColSize} control-label text-form-label" for="schedJobLogOutputThreshold">
            <g:message code="scheduledExecution.property.logOutputThreshold.label" default="Output Limit"/>
        </label>

        <div class="${fieldColShortSize}">

            <input type='text' name="logOutputThreshold" value="${enc(attr: scheduledExecution?.logOutputThreshold)}"
                   id="schedJobLogOutputThreshold" class="form-control"
                   placeholder="${message(code:"scheduledExecution.property.logOutputThreshold.placeholder")}"/>

            <span class="help-block">
                <g:message code="scheduledExecution.property.logOutputThreshold.description" default=""/>
            </span>
        </div>
        <label class="${labelColSize} control-label text-form-label" for="logOutputThresholdAction">
            <g:message code="scheduledExecution.property.logOutputThresholdAction.label" default="Action"/>
        </label>

        <div class="${fieldColShortSize}">
            <label class="radio" title="${message(code: "scheduledExecution.property.logOutputThresholdAction.halt.description")}">
                <g:radio name="logOutputThresholdAction" value="halt" checked="${!scheduledExecution?.logOutputThresholdAction || scheduledExecution?.logOutputThresholdAction=='halt'}"/>

                <g:message code="scheduledExecution.property.logOutputThresholdAction.halt.label"/>
            </label>
            <div class="input-group">
                <g:helpTooltip code="scheduledExecution.property.logOutputThresholdAction.halt.description" placement="left"/>
            <input type='text' name="logOutputThresholdStatus" value="${enc(attr: scheduledExecution?.logOutputThresholdStatus)}"
                       id="schedJobLogOutputThresholdStatus" class="form-control"
                       placeholder="${message(code:"scheduledExecution.property.logOutputThresholdStatus.placeholder")}"/>
            </div>

            <label class="radio" title="${message(code: "scheduledExecution.property.logOutputThresholdAction.truncate.description")}">
                <g:radio name="logOutputThresholdAction" value="truncate" checked="${scheduledExecution?.logOutputThresholdAction=='truncate'}"/>

                <g:message code="scheduledExecution.property.logOutputThresholdAction.truncate.label"/>
            </label>


            <span class="help-block">
                <g:message code="scheduledExecution.property.logOutputThresholdAction.description" default=""/>
            </span>
        </div>
    </div>


    %{--uuid--}%
    <div class="form-group ${hasErrors(bean: scheduledExecution, field: 'uuid', 'has-error')}" id="schedJobUuidLabel">
        <label for="schedJobUuid" class=" ${enc(attr:labelColClass)} text-muted">
            <g:message code="uuid" />
        </label>

        <div class="${fieldColSize}">
            <g:if test="${editSchedExecId && scheduledExecution?.uuid}">
                <p class="form-control-static text-muted" title="${g.message(code:'uuid.for.this.job')}">
                    <g:enc>${scheduledExecution?.uuid}</g:enc>
                </p>
            </g:if>
            <g:else>
                <input type='text' name="uuid" value="${enc(attr:scheduledExecution?.uuid)}"
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
    if (typeof(_initPopoverContentRef) == 'function') {
        _initPopoverContentRef();
    }
</g:javascript>
<!--[if (gt IE 8)|!(IE)]><!--> <g:javascript library="ace/ace"/><!--<![endif]-->
<div id="msg"></div>

    <g:render template="/framework/storageBrowseModalKO"/>
</div>
