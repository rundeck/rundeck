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
    _optlistitemContent.gsp

    Author: Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
    Created: Aug 2, 2010 4:11:05 PM
    $Id$
 --%>
<g:set var="ukey" value="${g.rkey()}"/>

<div id="optvis_${enc(attr:option.name)}" >
    <g:set var="canMoveUp" value="${optIndex!=0}"/>
    <g:set var="canMoveDown" value="${optIndex<optCount-1}"/>
    <div class="optitem optctrlholder">
      <g:if test="${edit}">
          <span class="optctrl opteditcontrols controls ">
            <g:if test="${canMoveUp || canMoveDown}">
                <span class="dragHandle btn btn-xs" title="${message(code:"drag.to.reorder")}">
                  <g:icon name="resize-vertical"/>
                </span>
            </g:if>
            <g:else>
                <span class="btn btn-xs btn-default disabled" ><g:icon name="resize-vertical"/></span>
            </g:else>
              <div class="btn-group">
                <g:if test="${canMoveUp}">
                    <span class="btn btn-xs btn-default" onclick="_doReorderOption('${enc(js:option.name)}',{pos:-1});"
                          title="${message(code:"move.up")}">
                        <i class="glyphicon glyphicon-arrow-up"></i>
                    </span>
                </g:if>
                <g:else>
                    <span class="btn btn-xs btn-default disabled" >
                        <i class="glyphicon glyphicon-arrow-up"></i>
                    </span>
                </g:else>
                <g:if test="${canMoveDown}">

                    <span class="btn btn-xs btn-default" onclick="_doReorderOption('${enc(js:option.name)}',{pos:1});"
                          title="${message(code:"move.down")}">
                        <i class="glyphicon glyphicon-arrow-down"></i>
                    </span>
                </g:if>
                <g:else>
                    <span class="btn btn-xs btn-default disabled" >
                        <i class="glyphicon glyphicon-arrow-down"></i>
                    </span>
                </g:else>
              </div>
          </span>
      </g:if>
        <span class="opt item " id="opt_${enc(attr:option.name)}" >
            <g:render template="/scheduledExecution/optView" model="${[option:option,edit:edit]}"/>
        </span>

        <div id="optdel_${enc(attr:ukey)}" class="panel panel-danger collapse">
            <div class="panel-heading">
                <g:message code="delete.this.option" />
            </div>

            <div class="panel-body">
                <g:message code="really.delete.option.0" args="${[option.name]}"/>
            </div>

            <g:jsonToken id="reqtoken_del_${ukey}" url="${request.forwardURI}"/>

            <div class="panel-footer">
                <span class="btn btn-default btn-xs"
                      onclick="jQuery('#optdel_${enc(js:ukey)}').collapse('toggle');"><g:message code="cancel"/></span>
                <span class="btn btn-danger btn-xs"
                      onclick=" _doRemoveOption('${enc(js:option.name)}', jQuery(this).closest('li.optEntry'),'reqtoken_del_${enc(js:ukey)}');"><g:message
                        code="delete"/></span>
            </div>
        </div>

    <g:if test="${edit}">
        <g:jsonToken id="reqtoken_duplicate_${ukey}" url="${request.requestURI}"/>

        <span class="optctrl opteditcontrols controls " id="optctrls_${enc(attr:option.name)}" style="position:absolute; right:0;">
            <span class="btn btn-xs btn-default" onclick="_optedit('${enc(js:option.name)}',jQuery(this).closest('li.optEntry'));"
                  title="${message(code:"edit.this.option")}">
                <i class="glyphicon glyphicon-edit"></i>
                <g:message code="edit" />
            </span>
            <span class="btn btn-xs btn-default" onclick="_optcopy('${enc(js:option.name)}','reqtoken_duplicate_${ukey}');"
                  title="${message(code:"duplicate.this.option")}">
                <i class="glyphicon glyphicon-duplicate"></i>
                <g:message code="duplicate" />
            </span>
            <span class="btn btn-xs btn-danger "
                  data-toggle="collapse"
                  data-target="#optdel_${enc(attr:ukey)}"
                  title="${message(code:"delete.this.option")}">
                <i class="glyphicon glyphicon-remove"></i>
              </span>
        </span>
    </g:if>
    <g:if test="${edit}">

            <g:javascript>
            fireWhenReady('opt_${enc(js:option.name)}',function(){
                var options = jQuery('#opt_${enc(js:option.name)}').find( '.autoedit' )
                options.each(function (indx, elem) {
                    elem.addEventListener('click', function(evt){
                        _optedit('${enc(js:option.name)}',jQuery(this).closest('li.optEntry'));
                    }, false);
                });
                });
            </g:javascript>
    </g:if>
  </div>
</div>
