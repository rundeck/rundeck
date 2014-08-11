%{--
  Copyright 2013 SimplifyOps Inc, <http://simplifyops.com>

  Licensed under the Apache License, Version 2.0 (the "License");
  you may not use this file except in compliance with the License.
  You may obtain a copy of the License at

      http://www.apache.org/licenses/LICENSE-2.0

  Unless required by applicable law or agreed to in writing, software
  distributed under the License is distributed on an "AS IS" BASIS,
  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  See the License for the specific language governing permissions and
  limitations under the License.
  --}%
<%--
   _queryFilterManager.gsp

   Author: Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
   Created: Apr 16, 2010 9:50:54 AM
   $Id$
--%>
<g:if test="${!rkey}">
    <g:set var="rkey" value="${g.rkey()}"/>
</g:if>
<g:if test="${!filterName}">

    <span class="textbtn textbtn-success floatr obs_hide_filtermgr"
          style="${wdgt.styleVisible(unless: params.saveFilter)}"
          onclick=" $$('.obs_hide_filtermgr').each(Element.toggle); " id="${enc(attr:rkey)}fsavebtn"
          title="Click to save this filter with a name">
        save this filter&hellip;
    </span>

    <div id="${enc(attr:rkey)}fsave" style="${params.saveFilter ? '' : 'display:none;'} "
         class=" panel-body clear obs_hide_filtermgr">
            <div class="row">
                <div class="col-sm-12">
                    <span class="h4">Save Filter</span>
                </div>
            </div>

            <div class="form-group">
                <label class="col-sm-2" for="existsFilterName">Filter:</label>

                <div class="col-sm-4">
                    <g:select class="form-control input-sm"
                              name="existsFilterName" optionKey="name" optionValue="name"
                              from="${filterset ? filterset.sort({ a, b -> a.name.compareTo(b.name) }) : filterset}"
                              value="${filterName}" noSelection="['': '-New-']"
                              onchange="if(this.value){\$('newFilterInput').hide();}else{\$('newFilterInput').show();}"/>
                </div>

                <div id="newFilterInput">
                    <label for="newFilterName" class="col-sm-2">Name:</label>

                    <div class="col-sm-4"><g:textField name="newFilterName" class="form-control input-sm"/></div>
                </div>
            </div>
    </div>

    <div class=" panel-footer text-right obs_hide_filtermgr"
         style="${params.saveFilter ? '' : 'display:none;'} ">
        <input type="button" onclick="$$('.obs_hide_filtermgr').each(Element.toggle);
        return false;" value="Cancel" class="btn btn-default btn-sm"/>

        <g:if test="${storeActionSubmit}">
            <g:actionSubmit value="Save Filter" action="${storeActionSubmit}" class="btn btn-primary btn-sm"/>
        </g:if>
        <g:elseif test="${storeActionSubmitRemote}">
            <g:submitToRemote value="Save Filter" url="${storeActionSubmitRemote}" update="${update}"
                              class="btn btn-primary btn-sm"/>
        </g:elseif>
    </div>
</g:if>
<g:if test="${filterName}">
    <div class=" panel-body saved clear">
        <div class="well-sm well">
            Saved filter: <strong>${g.enc(html:filterName)}</strong>
            <span class="textbtn textbtn-danger " onclick="['${rkey}fdel', '${rkey}fdelbtn'].each(Element.toggle);"
                  id="${rkey}fdelbtn" title="Click to delete this saved filter">
                delete&hellip;
                <i class="glyphicon glyphicon-remove"></i>
            </span>

            <div id="${enc(attr:rkey)}fdel" style="display:none">
                <g:hiddenField name="delFilterName" value="${filterName}"/>
                <span class="confirmMessage">Are you sure you want to delete this filter?</span>
                <input type="button" onclick="['${rkey}fdel', '${rkey}fdelbtn'].each(Element.toggle);
                return false;" value="No" class="btn btn-default btn-sm"/>
                <g:if test="${deleteActionSubmit}">
                    <g:actionSubmit action="${deleteActionSubmit}" value="Yes" class="btn btn-danger btn-sm"/>
                </g:if>
                <g:elseif test="${deleteActionSubmitRemote}">
                    <g:submitToRemote value="Yes" url="${deleteActionSubmitRemote}" update="${update}"
                                      class="btn btn-danger btn-sm"/>
                </g:elseif>
            </div>
        </div>
    </div>
</g:if>
