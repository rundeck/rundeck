<g:if test="${flash.message}">
    <div class="message">${flash.message}</div>
</g:if>
<g:hasErrors bean="${scheduledExecution}">
    <div class="errors">
        <g:renderErrors bean="${scheduledExecution}" as="list"/>
    </div>
</g:hasErrors>
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
    frameworkTestScriptAuth:'${createLink(controller:"framework",action:"testScriptAuth")}',
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
            if(!project){
                $('jobChooseSpinner').innerHTML="Please choose a project";
                $('jobChooseSpinner').show();
                doyft('schedEditFrameworkProjectHolder');
                return;
            }
            $('jobChooseSpinner').loading();
            $('jobChooseSpinner').show();

            $(elem).addClassName('selected');
            $('jobChooseBtn').down('img').src=AppImages.disclosureOpen;
            new Ajax.Updater(
                'jobChooserContent',
                '${createLink(controller:"menu",action:"jobsPicker")}',
                {
                parameters: {jobsjscallback:'jobChosen',projFilter:project,runAuthRequired:true},
                 onSuccess: function(transport) {
                    new MenuController().showRelativeTo(elem,target);
                     $('jobChooseSpinner').hide();
                 },
                 onFailure: function() {
                     showError("Error performing request: groupTreeFragment");
                     $('jobChooseSpinner').hide();
                 }
                });
        }
        function hideJobChooser(){
            $('jobChooser').hide();
            $('jobChooseBtn').removeClassName('selected');
            $('jobChooseBtn').down('img').src=AppImages.disclosure;
            $('jobChooseSpinner').hide();
        }

        function _menuDidCreateProject(value){
            var selected=$F('schedEditFrameworkProject');
            new Ajax.Updater('schedEditFrameworkProjectHolder','${createLink(controller:'framework',action:'projectSelect')}',{
                parameters:{callback:'_editFormSelectProject',key:'project',noselection:'-Choose a Project-',nocreate:'true',selected:selected},
                onComplete:function(transport){
                    if (transport.request.success()) {
                        if($('schedEditFrameworkProjectHolder').down('select')){
                            $('schedEditFrameworkProjectHolder').down('select').setAttribute('id','schedEditFrameworkProject');
                        }
                    }
                }
            });
        }
        function _menuDidSelectProject(value){
            return true;
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
            Event.observe(document.body,'click',function(evt){
                //click outside of popup bubble hides it
                tooltipMouseOut();
            },false);
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
        padding-right:100px;
    }
    .wfitemcontrols{
        margin-left:10px;
        position:absolute;
        right:0;
        top:0;
        width:100px;
        text-align:right;
    }
    .controls.autohide{
        display:none;
    }
    li:hover .controls.autohide{
        display:block;
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
        width:40px;
        text-align:right;
    }
    .optview{
        /*position:relative;*/
    }
    .optdetail{
        float:left;
        display:block;
        width:380px;
        overflow:hidden;
        white-space:nowrap;
        height:16px;
        line-height:16px;
    }
    .enforceSet{
        position:absolute;
        right: 45px;
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
        position:absolute;
        right: 150px;
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
</style>
<g:set var="wasSaved" value="${ (params?.saved=='true') || scheduledExecution?.id || scheduledExecution?.jobName || scheduledExecution?.scheduled}"/>
    
<input type="hidden" name="id" value="${scheduledExecution?.id}"/>
<div class="note error" style="display: none" id="editerror">
    
</div>
<table class="simpleForm" cellspacing="0">
 <g:if test="${!scheduledExecution?.id}">
    <tr id="saveJobQ" style="${wdgt.styleVisible(unless:scheduledExecution?.scheduled)}">
        <td>Save this job?</td>
        <td>
            <label>
            <g:radio name="saved" value="false"
                checked="${!wasSaved}"
                id="savedFalse" />
            No</label>

            <label>
            <g:radio name="saved" value="true"
                checked="${wasSaved}"
                id="savedTrue"/>
            Yes</label>
            <g:javascript>
                <wdgt:eventHandlerJS for="savedFalse" state="unempty" >
                    <wdgt:action visible="false" targetSelector="tbody.savedJobFields"/>
                    <wdgt:action check="true" target="scheduledFalse"/>
                    <wdgt:action visible="false" target="scheduledExecutionEditCrontab"/>
                </wdgt:eventHandlerJS>
                <wdgt:eventHandlerJS for="savedTrue" state="unempty" >
                    <wdgt:action visible="true" targetSelector="tbody.savedJobFields"/>
                </wdgt:eventHandlerJS>
            </g:javascript>
        </td>
    </tr>
    </g:if>
    <tbody class="savedJobFields" style=" ${wdgt.styleVisible(if:wasSaved)}" >
    <tr>
        <td>
            <label for="schedJobName" class=" ${hasErrors(bean:scheduledExecution,field:'jobName','fieldError')} required" id="schedJobNameLabel">
                <g:message code="domain.ScheduledExecution.title"/> Name
            </label>
        </td>
        <td>
            <span class="input">
                <input type='text' name="jobName" value="${scheduledExecution?.jobName.encodeAsHTML()}" id="schedJobName" size="40"/>
                <g:hasErrors bean="${scheduledExecution}" field="jobName">
                    <img src="${resource( dir:'images',file:'icon-small-warn.png' )}" alt="Error"  width="16px" height="16px" id="schedJobNameErr"/>
                    <wdgt:eventHandler for="schedJobName" state="unempty"  frequency="1">
                        <wdgt:action target="schedJobNameLabel" removeClassname="fieldError"/>
                        <wdgt:action visible="false" target="schedJobNameErr"/>
                    </wdgt:eventHandler>
                </g:hasErrors>
            </span>
        </td>
    </tr>
    <tr>
        <td>
            <label for="schedJobGroup" class=" ${hasErrors(bean:scheduledExecution,field:'groupPath','fieldError')}">
                Group
            </label>
        </td>
        <td>
            <span class="input">
                <g:hasErrors bean="${scheduledExecution}" field="groupPath">
                    <img src="${resource( dir:'images',file:'icon-small-warn.png' )}" alt="Error"  width="16px" height="16px"/>
                </g:hasErrors>
                <input type='text' name="groupPath" value="${scheduledExecution?.groupPath?.encodeAsHTML()}" id="schedJobGroup" size="40"/>
                <!--<span class="action" onclick="$('schedJobGroup').setValue('');" title="Clear Group field">
                    <img src="${resource( dir:'images',file:'icon-tiny-removex-gray.png' )}" alt="Clear"  width="12px" height="12px"/>
                </span>-->
            </span>

            <span class="action button" onclick="loadGroupChooser(this);" id="groupChooseBtn" title="Select an existing group to use">Choose &hellip; <g:img file="icon-tiny-disclosure.png" width="12px" height="12px"/></span>
            <span id="groupChooseSpinner"></span>
            <div class="popout" id="groupChooser" style="display:none; width:300px; padding: 5px; background:white; position:absolute;">
                <div style="margin-bottom:5px;">
                    <span class="info note">Click on the name of the group to use</span>
                    <span class=" floatr action textbtn" style="text-align:right" onclick="hideGroupChooser();">
                        Close
                        <g:img file="icon-tiny-removex-gray.png" width="12px" height="12px"/>
                    </span>
                </div>
                <div id="groupChooserContent" style="overflow-y:auto;">
                </div>
            </div>

            <script type="text/javascript" src="${resource(dir:'js',file:'yellowfade.js')}"></script>
            <script type="text/javascript">
                function groupChosen(path){
                    $('schedJobGroup').setValue(path);
                    $('schedJobGroup').highlight();
                    hideGroupChooser();
                }
                function loadGroupChooser(elem){
                    if($('groupChooser').visible()){
                        hideGroupChooser();
                        return;
                    }
                    $('groupChooserContent').innerHTML='<img src="'+ appLinks.iconSpinner+'" alt=""/> Loading...';
                    $(elem).addClassName('selected');
                    $('groupChooseBtn').down('img').src=AppImages.disclosureOpen;
                    var project = $F('schedEditFrameworkProject');
                    if (!project) {
                        $('groupChooseSpinner').innerHTML = "Please choose a project";
                        $('groupChooseSpinner').show();
                        doyft('schedEditFrameworkProjectHolder');
                        return;
                    }
                    $('groupChooseSpinner').loading();
                    $('groupChooseSpinner').show();
                    new Ajax.Updater(
                        'groupChooserContent',
                        '${createLink(controller:"scheduledExecution",action:"groupTreeFragment")}',
                        {
                        parameters: {jscallback:"groupChosen",project:project},
                         onSuccess: function(transport) {
                            new MenuController().showRelativeTo(elem,'groupChooser');
                             $('groupChooseSpinner').hide();
                         },
                         onFailure: function() {
                             showError("Error performing request: groupTreeFragment");
                             $('jobChooseSpinner').hide();
                         }
                        });
                }
                function hideGroupChooser(){
                    $('groupChooser').hide();
                    $('groupChooseBtn').removeClassName('selected');
                    $('groupChooseBtn').down('img').src=AppImages.disclosure;
                }

            </script>
            <div class="info note">
                Enter a / separated path.
            </div>
        </td>
    </tr>

    <tr>
        <td>
            <label for="description" class="${hasErrors(bean:scheduledExecution,field:'description','fieldError')}">Description</label>
        </td>
        <td>
            <span class="input ${hasErrors(bean:scheduledExecution,field:'description','fieldError')}">
                <g:textArea name="description" value="${scheduledExecution?.description}" cols="70" rows="2" />

                <g:hasErrors bean="${scheduledExecution}" field="description">
                    <img src="${resource( dir:'images',file:'icon-small-warn.png' )}" alt="Error" width="16px" height="16px"/>
                </g:hasErrors>
            </span>
        </td>
    </tr>
    <tr>
        <td>
            <label for="schedJobUuid"
                   class=" ${hasErrors(bean: scheduledExecution, field: 'uuid', 'fieldError')}"
                   id="schedJobUuidLabel">
                UUID
            </label>
        </td>
        <td>
            <g:if test="${editSchedExecId && scheduledExecution?.uuid}">
                <span class="desc" title="UUID for this Job">
                    ${scheduledExecution?.uuid?.encodeAsHTML()}
                </span>
            </g:if>
            <g:else>
                <span class="input">
                    <input type='text' name="uuid" value="${scheduledExecution?.uuid?.encodeAsHTML()}"
                           id="schedJobUuid" size="40"/>
                    <g:hasErrors bean="${scheduledExecution}" field="uuid">
                        <img src="${resource(dir: 'images', file: 'icon-small-warn.png')}" alt="Error" width="16px"
                             height="16px" id="schedJobUuidErr"/>
                        <wdgt:eventHandler for="schedJobUuid" state="unempty" frequency="1">
                            <wdgt:action target="schedJobUuidLabel" removeClassname="fieldError"/>
                            <wdgt:action visible="false" target="schedJobUuidErr"/>
                        </wdgt:eventHandler>
                    </g:hasErrors>
                </span>
            </g:else>
        </td>
    </tr>
    <tr>
        <td>
            <g:message code="scheduledExecution.property.multipleExecutions.label" />
        </td>
        <td>
            <label>
                <g:radio value="false" name="multipleExecutions"
                         checked="${!scheduledExecution.multipleExecutions}"
                         id="multipleFalse"/>
                <g:message code="no" />
            </label>

            <label>
                <g:radio name="multipleExecutions" value="true"
                         checked="${scheduledExecution.multipleExecutions}"
                         id="multipleTrue"/>
                <g:message code="yes" />
            </label>

            <span class="info note">
                <g:message code="scheduledExecution.property.multipleExecutions.description" />
            </span>
        </td>
    </tr>
    </tbody>
    <tr>
        <td class="${hasErrors(bean:scheduledExecution,field:'project','fieldError')} required" id="schedProjErr">Project</td>
        <td>
            <div id="schedEditFrameworkProjectHolder">
                <g:select id="schedEditFrameworkProject" name="project" from="${projects*.name}" value="${scheduledExecution.project?scheduledExecution.project.toString():projects?.size()==1?projects[0].name:session.project?session.project:''}" onchange="_editFormSelectProject(this.value);" noSelection="['':'-Choose a Project-']"/>
            </div>
            <g:hasErrors bean="${scheduledExecution}" field="project">
                    <img src="${resource( dir:'images',file:'icon-small-warn.png' )}" alt="Error"  width="16px" height="16px" id="schedProjErrImg"/>
                    <wdgt:eventHandler for="schedEditFrameworkProject" state="unempty" >
                        <wdgt:action target="schedProjErr" removeClassname="fieldError"/>
                        <wdgt:action visible="false" target="schedProjErrImg"/>
                    </wdgt:eventHandler>
                </g:hasErrors>
        </td>
    </tr>

    <tbody id="optionsContent" class="savedJobFields" style=" ${wdgt.styleVisible(if:wasSaved)}">
        <tr>
            <td><span id="optsload"></span>Options:</td>
            <td>
                <span id="optssummarysect" class="autohilite autoedit">
                    <span id="optssummary" class="doeditopts " title="Click to edit options">
                        <g:render template="/scheduledExecution/optionsSummary" model="${[options:scheduledExecution?.options,edit:true]}"/>
                    </span>
                </span>

                <wdgt:eventHandler forSelector="span.doeditopts" action="click" >
                    <wdgt:action visible="true" target="editoptssect"/>
                    <wdgt:action visible="false" target="optssummarysect"/>
                    <g:if test="${!(scheduledExecution?.options)}">
                        <wdgt:action jshandler="_optaddnewIfNone" target="optssummarysect"/>
                    </g:if>
                </wdgt:eventHandler>
                
                <div style="display:none;" id="editoptssect" class="rounded">
                    <g:render template="/scheduledExecution/detailsOptions" model="${[options:scheduledExecution?.options,edit:true]}"/>
                    <g:if test="${scheduledExecution && scheduledExecution.argString}">
                        <span class="argString">${scheduledExecution?.argString.encodeAsHTML()}</span>
                    </g:if>
                    <g:hiddenField name="_sessionopts" value="true"/>
                
                    <div style="margin: 10px 0 5px 0;">
                        <span class="action button opteditcontrols" id="doneEditopts">Close</span>
                        <wdgt:eventHandler for="doneEditopts" action="click" >
                            <wdgt:action visible="false" target="editoptssect"/>
                            <wdgt:action visible="true" target="optssummarysect"/>
                            <wdgt:action jshandler="_summarizeOpts" target="optssummarysect"/>
                        </wdgt:eventHandler>
                    </div>
                </div>
            </td>
        </tr>
    </tbody>

    <tbody id="workflowContent" >
        <tr>
            <td>Workflow:</td>
            <td>
                <g:set var="editwf" value="${session.editWF && session.editWF[scheduledExecution.id.toString()]?session.editWF[scheduledExecution.id.toString()]:scheduledExecution.workflow}"/>
                <g:render template="/execution/execDetailsWorkflow" model="${[workflow:editwf,context:scheduledExecution,edit:true,error:scheduledExecution?.errors?.hasFieldErrors('workflow'),project:scheduledExecution?.project?:projects?.size()==1?projects[0].name:session.project?:'']}"/>
                <g:hiddenField name="_sessionwf" value="true"/>
                <g:if test="${null==editwf || null==editwf.commands || 0==editwf.commands.size()}">
                    <g:javascript>
                        fireWhenReady('workflowContent',function(){_wfiaddnew('command');});
                    </g:javascript>
                </g:if>
            </td>
        </tr>
    </tbody>
    <tbody class="savedJobFields" style="${wdgt.styleVisible(if:wasSaved)}" >
    <g:set var="defSuccess" value="${scheduledExecution.findNotification('onsuccess','email')}"/>
    <g:set var="isSuccess" value="${params.notifySuccessRecipients && 'true'==params.notifyOnsuccess ||  defSuccess}"/>
    <g:set var="defSuccessUrl" value="${scheduledExecution.findNotification('onsuccess','url')}"/>
    <g:set var="isSuccessUrl" value="${params.notifySuccessUrl && 'true'==params.notifyOnsuccessUrl ||  defSuccessUrl}"/>

    <g:set var="defFailure" value="${scheduledExecution.findNotification('onfailure','email')}"/>
    <g:set var="isFailure" value="${params.notifyFailureRecipients && 'true'==params.notifyOnfailure ||  defFailure}"/>
    <g:set var="defFailureUrl" value="${scheduledExecution.findNotification('onfailure','url')}"/>
    <g:set var="isFailureUrl" value="${params.notifyFailureUrl && 'true'==params.notifyOnfailureUrl ||  defFailureUrl}"/>
    <tr>
        <td>
            Send Notification?
        </td>
        <td>
            <label>
            <g:radio value="false" name="notified"
                checked="${!(isFailure||isSuccess || isSuccessUrl || isFailureUrl)}"
                id="notifiedFalse"/>
                No
            </label>

            <label>
                <g:radio name="notified" value="true"
                    checked="${isFailure||isSuccess || isSuccessUrl || isFailureUrl}"
                    id="notifiedTrue"/>
                Yes
            </label>

            <g:javascript>
                <wdgt:eventHandlerJS for="notifiedTrue" state="unempty">
                    <wdgt:action visible="true" targetSelector="tr.notifyFields"/>
                </wdgt:eventHandlerJS>
                <wdgt:eventHandlerJS for="notifiedFalse" state="unempty" >
                    <wdgt:action visible="false" targetSelector="tr.notifyFields"/>

                    <wdgt:action check="false" target="notifyOnsuccess"/>
                    <wdgt:action visible="false" target="notifSuccessholder" />
                    <wdgt:action check="false" target="notifyOnfailure"/>
                    <wdgt:action visible="false" target="notifFailureholder" />
                </wdgt:eventHandlerJS>
            </g:javascript>
        </td>
    </tr>
    <tr class="notifyFields" style="${wdgt.styleVisible(if:isFailure||isSuccess || isSuccessUrl || isFailureUrl)}">

        <!-- onsuccess-->
        <td>
            <label for="notifyOnsuccess" class=" ${hasErrors(bean:scheduledExecution,field:'notifySuccessRecipients','fieldError')} ${hasErrors(bean: scheduledExecution, field: 'notifySuccessUrl', 'fieldError')}">
                <g:message code="notification.event.onsuccess"/>
            </label>
        </td>
        <td>
            <div>
                <span>
                    <g:checkBox name="notifyOnsuccess" value="true" checked="${isSuccess?true:false}" />
                    <label for="notifyOnsuccess">Send Email</label>
                </span>
                <span id="notifSuccessholder" style="${wdgt.styleVisible(if:isSuccess)}">
                    <label>to: <g:textField name="notifySuccessRecipients"  cols="70" rows="3"  value="${defSuccess?defSuccess.content:params.notifySuccessRecipients}" size="60"/></label>
                    <div class="info note">comma-separated email addresses</div>
                    <g:hasErrors bean="${scheduledExecution}" field="notifySuccessRecipients">
                        <div class="fieldError">
                            <g:renderErrors bean="${scheduledExecution}" as="list" field="notifySuccessRecipients"/>
                        </div>
                    </g:hasErrors>
                </span>
                <wdgt:eventHandler for="notifyOnsuccess" state="checked" target="notifSuccessholder" visible="true"/>
            </div>
            <div>
                <span>
                    <g:checkBox name="notifyOnsuccessUrl" value="true" checked="${isSuccessUrl?true:false}" />
                    <label for="notifyOnsuccessUrl">Webhook</label>
                </span>
                <span id="notifSuccessholder2" style="${wdgt.styleVisible(if: isSuccessUrl)}">
                    <label>POST to URLs:
                        <g:set var="notifsuccessurlcontent" value="${defSuccessUrl?defSuccessUrl.content:params.notifySuccessUrl}"/>
                        <g:if test="${notifsuccessurlcontent && notifsuccessurlcontent.length()>30}">
                            <textarea name="notifySuccessUrl"
                                style="vertical-align:top;"
                                rows="6" cols="40">${notifsuccessurlcontent?.encodeAsHTML()}</textarea>
                        </g:if>
                        <g:else>
                            <g:textField name="notifySuccessUrl"  cols="70" rows="3"  value="${notifsuccessurlcontent?.encodeAsHTML()}" size="60"/>
                        </g:else>
                    </label>
                    <div class="info note">comma-separated URLs</div>
                    <g:hasErrors bean="${scheduledExecution}" field="notifySuccessUrl">
                        <div class="fieldError">
                            <g:renderErrors bean="${scheduledExecution}" as="list" field="notifySuccessUrl"/>
                        </div>
                    </g:hasErrors>
                </span>
                <wdgt:eventHandler for="notifyOnsuccessUrl" state="checked" target="notifSuccessholder2" visible="true" />
            </div>
        </td>
    </tr>

    <tr class="notifyFields"  style="${wdgt.styleVisible(if:isFailure||isSuccess || isSuccessUrl || isFailureUrl)}">

        <!-- onfailure-->
        <td>
            <label for="notifyOnfailure" class=" ${hasErrors(bean:scheduledExecution,field:'notifyFailureRecipients','fieldError')}">
                <g:message code="notification.event.onfailure"/>
            </label>
        </td>
        <td>
            <div>
                <span>
                    <g:checkBox name="notifyOnfailure" value="true" checked="${isFailure?true:false}"/>
                    <label for="notifyOnfailure">Send Email</label>
                </span>
                <span id="notifFailureholder" style="${wdgt.styleVisible(if:isFailure)}">
                    <label>to: <g:textField name="notifyFailureRecipients"  cols="70" rows="3" value="${defFailure?defFailure.content:params.notifyFailureRecipients}" size="60"/></label>
                    <div class="info note">comma-separated email addresses</div>
                    <g:hasErrors bean="${scheduledExecution}" field="notifyFailureRecipients">
                        <div class="fieldError">
                            <g:renderErrors bean="${scheduledExecution}" as="list" field="notifyFailureRecipients"/>
                        </div>
                    </g:hasErrors>
                </span>
                <wdgt:eventHandler for="notifyOnfailure" state="checked" target="notifFailureholder" visible="true" />
            </div>
            <div>
                <span>
                    <g:checkBox name="notifyOnfailureUrl" value="true" checked="${isFailureUrl?true:false}"/>
                    <label for="notifyOnfailureUrl">Webhook</label>
                </span>
                <span id="notifFailureholder2" style="${wdgt.styleVisible(if:isFailureUrl)}">
                    <label>POST to URLs:
                        <g:set var="notiffailureurlcontent" value="${defFailureUrl?defFailureUrl.content:params.notifyFailureUrl}"/>
                        <g:if test="${notiffailureurlcontent && notiffailureurlcontent.length()>30}">
                            <textarea name="notifyFailureUrl"
                                style="vertical-align:top;"
                                rows="6" cols="40">${notiffailureurlcontent?.encodeAsHTML()}</textarea>
                        </g:if>
                        <g:else>
                            <g:textField name="notifyFailureUrl" cols="70" rows="3" value="${notiffailureurlcontent?.encodeAsHTML()}" size="60"/>
                        </g:else>
                    </label>
                    <div class="info note">comma-separated URLs</div>
                    <g:hasErrors bean="${scheduledExecution}" field="notifyFailureUrl">
                        <div class="fieldError">
                            <g:renderErrors bean="${scheduledExecution}" as="list" field="notifyFailureUrl"/>
                        </div>
                    </g:hasErrors>
                </span>
                <wdgt:eventHandler for="notifyOnfailureUrl" state="checked" target="notifFailureholder2" visible="true" />
            </div>
        </td>
    </tr>

    <tr >
        <td>
            Schedule to run repeatedly?
        </td>
        <td>
            <div style="margin:0 0 10px 0">

            <label>
            <g:radio value="false" name="scheduled"
                checked="${scheduledExecution?.scheduled?false:true}"
                id="scheduledFalse"/>
                No
            </label>

            <label>
                <g:radio name="scheduled" value="true"
                    checked="${scheduledExecution?.scheduled}"
                    id="scheduledTrue"/>
                Yes
            </label>
            </div>
            <div style="${wdgt.styleVisible(if:scheduledExecution?.scheduled)}" id="scheduledExecutionEditCrontab">
                <g:render template="editCrontab" model="[scheduledExecution:scheduledExecution, crontab:crontab]"/>

                <div class="clear"></div>
                <%--
                <div class="floatl clear crontab" style="margin-top: 5px;">
                    Execute as user:
                    <g:textField name="user" value="${scheduledExecution?.user}" size="12"/>
                </div>

                <div class="clear"></div>

                --%>
            </div>
            <g:javascript>
                <wdgt:eventHandlerJS for="scheduledTrue" state="unempty">
                    <wdgt:action visible="true" targetSelector="#scheduledExecutionEditCrontab"/>
                </wdgt:eventHandlerJS>
                <wdgt:eventHandlerJS for="scheduledFalse" state="unempty" >
                    <wdgt:action visible="false" target="scheduledExecutionEditCrontab"/>
                </wdgt:eventHandlerJS>
            </g:javascript>
        </td>
    </tr>
    </tbody>
    <tr>
        <td>
            <label for="doNodedispatch" id="doNodedispatchLabel">
                Dispatch to Nodes
            </label>
        </td>
        <td>
            <g:checkBox
                name="doNodedispatch"
                value="true"
                checked="${scheduledExecution?.doNodedispatch}"
                id="doNodedispatch"
                onchange="_matchNodes()"
            />
            <span class="info note">
            If not selected, the Job will only run on the local node.
            </span>
            <g:javascript>
                <wdgt:eventHandlerJS for="doNodedispatch" state="checked" oneway="true" >
                    <wdgt:action visible="true" targetSelector="tbody.nodeFilterFields"/>
                    <wdgt:action visible="true" target="nodeDispatchFields" />
                </wdgt:eventHandlerJS>
                <wdgt:eventHandlerJS for="doNodedispatch" state="unchecked" oneway="true" >
                    <wdgt:action visible="false" target="nodeDispatchFields"/>
                    <wdgt:action visible="false" targetSelector="tbody.nodeFilterFields"/>
                </wdgt:eventHandlerJS>
            </g:javascript>
        </td>
    </tr>
    <tbody style="${wdgt.styleVisible(if:scheduledExecution?.doNodedispatch)}" class="subfields nodeFilterFields">
        <tr>
            <td>
                <span class=" ${hasErrors(bean:scheduledExecution,field:'nodeInclude','fieldError')}">
                    Include
                </span>
            </td>
            <td>

                <g:hasErrors bean="${scheduledExecution}" field="nodeInclude">
                    <div class="fieldError">
                        <g:renderErrors bean="${scheduledExecution}" as="list" field="nodeInclude"/>
                    </div>
                </g:hasErrors>
                <g:hasErrors bean="${scheduledExecution}" field="nodeInclude">
                    <img src="${resource( dir:'images',file:'icon-small-warn.png' )}" alt="Error"  width="16px" height="16px"/>
                </g:hasErrors>
                <div id="nodeFilterDivInclude" >
                    <g:each var="key" in="${NODE_FILTERS_ALL}">
                            <div id="nodeFilterInclude${key}"  style="${wdgt.styleVisible(if:scheduledExecution?.('nodeInclude'+key))}"  class="nodefilterfield">
                            <span class="input">
                                <label ><span class="text">${NODE_FILTER_MAP[key]?NODE_FILTER_MAP[key]:key}:</span>
                                <g:set var="filtvalue" value="${scheduledExecution?.('nodeInclude'+key)?.encodeAsHTML()}"/>
                                <g:if test="${filtvalue && filtvalue.length()>30}">
                                    <textarea  name="nodeInclude${key}" id="schedJobNodeInclude${key}" onchange="_matchNodes();"
                                        style="vertical-align:top;"
                                        rows="6" cols="40">${filtvalue}</textarea>
                                </g:if>
                                <g:else>
                                    <input type='text' name="nodeInclude${key}" class="filterIncludeText"
                                            value="${filtvalue}" id="schedJobNodeInclude${key}" onchange="_matchNodes();" />
                                </g:else>
                                </label>
                                <span title="Remove filter for ${NODE_FILTER_MAP[key]?NODE_FILTER_MAP[key]:key}"
                                    class="filterRemove action"
                                    onclick="removeFilter('${key}',true);"
                                    ><img src="${resource( dir:'images',file:'icon-tiny-removex.png' )}" alt="remove" width="12px" height="12px"/></span>
                                <g:javascript>
                                    Event.observe(window,'load',function(){ $('schedJobNodeInclude${key}').onkeypress=_matchNodesKeyPress; });
                                </g:javascript>
                            </span>
                                <g:if test="${g.message(code:'node.metadata.'+key+'.defaults',default:'')}">
                                    <g:select from="${g.message(code:'node.metadata.'+key+'.defaults').split(',').sort()}" onchange="setFilter('${key}',true,this.value);_matchNodesKeyPress();" name="_${key}defaults"/>
                                </g:if>
                            </div>
                    </g:each>

                </div>
                <div class="filterkeyset">
                    <g:each var="key" in="${NODE_FILTERS}">
                        <span
                            style="${wdgt.styleVisible(unless:scheduledExecution?.('nodeInclude'+key))}"
                            title="Add Filter for ${NODE_FILTER_MAP[key]?NODE_FILTER_MAP[key]:key}"
                            class="filterAdd button textbtn action"
                            id="filterAddInclude${key}"
                            onclick="addFilter('${key}',true,'${NODE_FILTER_MAP[key]?NODE_FILTER_MAP[key]:key}');"
                            >${NODE_FILTER_MAP[key]?NODE_FILTER_MAP[key]:key}</span>
                    </g:each>
                    <span class="filterAdd button textbtn action" onclick="Element.show('${rkey}moreIncludeFilters');Element.hide(this);">more&hellip;</span>
                </div>
                <div id="${rkey}moreIncludeFilters" style="display:none;" class="filterkeyset">
                    <g:each var="key" in="${NODE_FILTERS_X}">
                        <span
                            style="${wdgt.styleVisible(unless:scheduledExecution?.('nodeInclude'+key))}"
                            title="Add Filter for ${NODE_FILTER_MAP[key]?NODE_FILTER_MAP[key]:key}"
                            class="filterAdd button textbtn action"
                            id="filterAddInclude${key}"
                            onclick="addFilter('${key}',true,'${NODE_FILTER_MAP[key]?NODE_FILTER_MAP[key]:key}');"
                            >${NODE_FILTER_MAP[key]?NODE_FILTER_MAP[key]:key}</span>
                    </g:each>
                </div>
                <g:render template="/common/nodefilterRegexSyntaxNote"/>
            </td>
        </tr>
        <tr>
            <td>
            </td>
            <td >
                <g:expander key="extNodeFilters">Extended Filters&hellip;</g:expander>
            </td>
        </tr>
    </tbody>
    <tbody  style="display:none" class="subfields" id="extNodeFilters">
        <tr>
            <td>
                <span class=" ${hasErrors(bean:scheduledExecution,field:'nodeExclude','fieldError')}">
                    Exclude
                </span>
            </td>
            <td>
                <div>
                    <g:hasErrors bean="${scheduledExecution}" field="nodeExclude">
                    <div class="fieldError">
                        <g:renderErrors bean="${scheduledExecution}" as="list" field="nodeExclude"/>
                    </div>
                </g:hasErrors>
                    <g:hasErrors bean="${scheduledExecution}" field="nodeExclude">
                        <img src="${resource( dir:'images',file:'icon-small-warn.png' )}" alt="Error"  width="16px" height="16px"/>
                    </g:hasErrors>
                </div>
                <div id="nodeFilterDivExclude" >
                    <g:each var="key" in="${NODE_FILTERS_ALL}">
                            <div id="nodeFilterExclude${key}" style="${wdgt.styleVisible(if:scheduledExecution?.('nodeExclude'+key))}" class="nodefilterfield">
                            <span class="input">
                                <label><span class="text">${NODE_FILTER_MAP[key]?NODE_FILTER_MAP[key]:key}:</span>
                                <g:set var="filtvalue" value="${scheduledExecution?.('nodeExclude'+key)?.encodeAsHTML()}"/>
                                <g:if test="${filtvalue && filtvalue.length()>30}">
                                    <textarea name="nodeExclude${key}" id="schedJobNodeExclude${key}" onchange="_matchNodes();"
                                        style="vertical-align:top;"
                                        rows="6" cols="40">${filtvalue}</textarea>
                                </g:if>
                                <g:else>
                                    <input type='text' name="nodeExclude${key}"
                                        value="${filtvalue}" id="schedJobNodeExclude${key}" onchange="_matchNodes();"/>
                                </g:else>
                                </label>
                                <span class="filterRemove action"
                                    onclick="removeFilter('${key}',false);"
                                    ><img src="${resource( dir:'images',file:'icon-tiny-removex.png' )}" alt="remove" width="12px" height="12px"/></span>
                                <g:javascript>
                                    Event.observe(window,'load',function(){ $('schedJobNodeExclude${key}').onkeypress=_matchNodesKeyPress; });
                                </g:javascript>
                            </span>
                                <g:if test="${g.message(code:'node.metadata.'+key+'.defaults',default:'')}">
                                    <g:select from="${g.message(code:'node.metadata.'+key+'.defaults').split(',').sort()}" onchange="setFilter('${key}',false,this.value);_matchNodesKeyPress();" name="_${key}defaults"/>
                                </g:if>
                            </div>
                    </g:each>

                </div>
                <div class="filterkeyset">
                    <g:each var="key" in="${NODE_FILTERS_ALL}">
                            <span
                                style="${wdgt.styleVisible(unless:scheduledExecution?.('nodeExclude'+key))}"
                            title="Add Filter: ${NODE_FILTER_MAP[key]?NODE_FILTER_MAP[key]:key}"
                            class="filterAdd button textbtn action"
                            id="filterAddExclude${key}"
                            onclick="addFilter('${key}',false,'${NODE_FILTER_MAP[key]?NODE_FILTER_MAP[key]:key}');"
                            >${NODE_FILTER_MAP[key]?NODE_FILTER_MAP[key]:key}</span>
                    </g:each>
                </div>
            </td>
        </tr>
        <tr>
            <td>Precedence to:</td>
            <td>
                <label  title="Include more nodes">
                <g:radio name="nodeExcludePrecedence" value="false"
                    checked="${!scheduledExecution?.nodeExcludePrecedence}"
                    id="nodeExcludePrecedenceFalse" onchange="_matchNodes()"/>
                    Included</label>

                <label title="Exclude more nodes">
                <g:radio name="nodeExcludePrecedence" value="true"
                    checked="${scheduledExecution?.nodeExcludePrecedence}"
                    id="nodeExcludePrecedenceTrue" onchange="_matchNodes()" />
                    Excluded</label>
            </td>
        </tr>
    </tbody>
    <tbody style="${wdgt.styleVisible(if:scheduledExecution?.doNodedispatch)}" class="subfields nodeFilterFields">
    <tr>
        <td onclick="_formUpdateMatchedNodes()"><span id="mnodeswait"></span> <span class="button action textbtn" title="click to refresh">Matched nodes</span></td>
        <td id="matchednodes" class="embed matchednodes" >
            <span class="action textbtn" onclick="_formUpdateMatchedNodes()">Update...</span>
        </td>
    </tr>


    </tbody>
    <tbody id="nodeDispatchFields" style="${wdgt.styleVisible(if:scheduledExecution?.doNodedispatch)} " class="subfields">
        <tr>
            <th colspan="2">Dispatch Options</th>
        </tr>
        <tr>
            <td>
                <label for="schedJobnodeThreadcount" class=" ${hasErrors(bean:scheduledExecution,field:'nodeThreadcount','fieldError')}">
                    Thread Count
                </label>
            </td>
            <td>
                <g:hasErrors bean="${scheduledExecution}" field="nodeThreadcount">
                    <div class="fieldError">
                        <g:renderErrors bean="${scheduledExecution}" as="list" field="nodeThreadcount"/>
                    </div>
                </g:hasErrors>
                <span class="input">
                    <input type='text' name="nodeThreadcount" value="${scheduledExecution?.nodeThreadcount?.encodeAsHTML()}" id="schedJobnodeThreadcount" size="3"/>
                    <g:hasErrors bean="${scheduledExecution}" field="nodeThreadcount">
                        <img src="${resource( dir:'images',file:'icon-small-warn.png' )}" alt="Error"  width="16px" height="16px"/>
                    </g:hasErrors>
                </span>
                <span class="info note">
                    Maximum number of parallel threads to use.
                </span>

            </td>
        </tr>
        <tr>
            <td>
                <label for="schedJobnodeRankAttribute" class=" ${hasErrors(bean:scheduledExecution,field:'nodeRankAttribute','fieldError')}">
                    <g:message code="scheduledExecution.property.nodeRankAttribute.label" />
                </label>
            </td>
            <td>
                <g:hasErrors bean="${scheduledExecution}" field="nodeRankAttribute">
                    <div class="fieldError">
                        <g:renderErrors bean="${scheduledExecution}" as="list" field="nodeRankAttribute"/>
                    </div>
                </g:hasErrors>
                <span class="input">
                    <input type='text' name="nodeRankAttribute" value="${scheduledExecution?.nodeRankAttribute?.encodeAsHTML()}" id="schedJobnodeRankAttribute" size="20"/>
                    <g:hasErrors bean="${scheduledExecution}" field="nodeRankAttribute">
                        <img src="${resource( dir:'images',file:'icon-small-warn.png' )}" alt="Error"  width="16px" height="16px"/>
                    </g:hasErrors>
                </span>
                <span class="info note">
                    <g:message code="scheduledExecution.property.nodeRankAttribute.description" />
                </span>
            </td>
        </tr>
        <tr>
            <td><g:message code="scheduledExecution.property.nodeRankOrder.label" /></td>
            <td>
                <label>
                    <g:radio name="nodeRankOrderAscending" value="true"
                             checked="${scheduledExecution?.nodeRankOrderAscending || null==scheduledExecution?.nodeRankOrderAscending}"
                             id="nodeRankOrderAscending"/>
                    <g:message code="scheduledExecution.property.nodeRankOrder.ascending.label" /></label>
                <label>
                    <g:radio name="nodeRankOrderAscending" value="false"
                             checked="${!scheduledExecution?.nodeRankOrderAscending && null!=scheduledExecution?.nodeRankOrderAscending}"
                             id="nodeRankOrderDescending"/>
                    <g:message code="scheduledExecution.property.nodeRankOrder.descending.label" /></label>

            </td>
        </tr>
        <tr>
            <td>Keep going on error?</td>
            <td>
                <label>
                <g:radio name="nodeKeepgoing" value="false"
                    checked="${!scheduledExecution?.nodeKeepgoing}"
                    id="nodeKeepgoingFalse"/>
                No</label>

                <label>
                <g:radio name="nodeKeepgoing" value="true"
                    checked="${scheduledExecution?.nodeKeepgoing}"
                    id="nodeKeepgoingTrue"/>
                Yes</label>
            </td>
        </tr>
    </tbody>
    <tr>
        <td>Log level</td>
        <td>
            <g:select name="loglevel"
                      from="${['1. Debug','2. Verbose','3. Information','4. Warning','5. Error']}"
                      keys="${['DEBUG','VERBOSE','INFO','WARN','ERR']}"
                      value="${scheduledExecution.loglevel?scheduledExecution.loglevel:'WARN'}"
                />
            <div class="info note">
                Higher numbers produce less output.
            </div>
        </td>
    </tr>


</table>
<g:javascript>
    if (typeof(initTooltipForElements) == 'function') {
        initTooltipForElements('.obs_tooltip');
    }
</g:javascript>
<div id="msg"></div>
