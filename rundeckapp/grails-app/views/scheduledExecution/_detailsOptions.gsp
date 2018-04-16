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
<%--
    _detailsOptions.gsp
    
    Author: Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
    Created: Aug 2, 2010 1:50:19 PM
    $Id$
 --%>

<g:set var="rkey" value="${g.rkey()}"/>

    <g:if test="${edit}">
        <div id="optundoredo" class="undoredocontrols">
            <g:render template="/common/undoRedoControls" model="[key: 'opts']"/>
        </div>
    </g:if>

<div class="optslist" id="optionContent">
    <!--
    header
    -->

    <div id="optheader" style="${wdgt.styleVisible(if:options && options.size()>0)}">
        <div class="optheader optctrlholder">
            <span class="optdetail">
                <span class="header" >Name</span>
            </span>
            <span class="valuesSet">
                <span class="header">Values</span>
            </span>
            <span class="enforceSet">
                <span class="header">Restriction</span>
            </span>
        </div>
    </div>
    <div class="clear"></div>
    <ul class="options">
        <g:render template="/scheduledExecution/optlistContent" model="${[options:options,edit:edit]}"/>
    </ul>
    <div id="optionDropFinal" class="dragdropfinal" data-abs-index="${options?.size()?:1}" data-is-final="true" style="display:none"></div>
    <g:embedJSON id="optDataList" data="${options.collect{[name:it.name,type:it.optionType,multivalued:it.multivalued, delimiter: it.delimiter]}}"/>
    <g:javascript>
    jQuery(function(){
        "use strict";
        _enableOptDragDrop();
        _optionData(loadJsonData('optDataList'));
    });
</g:javascript>

    <div class="empty note ${error?'error':''}" id="optempty" style="${wdgt.styleVisible(unless:options && options.size()>0)}">
        No Options
    </div>

    <g:if test="${edit}">
        <div id="optnewbutton" style="margin:5px 0 15px 0; ">
            <span class="btn btn-default btn-sm ready" onclick="_optaddnew();" title="Add a new Option">
                <b class="glyphicon glyphicon-plus"></b>
                Add an option
            </span>
        </div>
    </g:if>
</div>



<div id="optnewitem"></div>
