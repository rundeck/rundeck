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
<g:if test="${includeSaveButton}">
    <div class=" panel-body saved ">
        <a class="btn btn-default btn-sm pull-right" data-toggle="modal" href="#saveFilterModal">
            Save this filter&hellip;
        </a>
    </div>
</g:if>
<g:if test="${includeDeleteButton}">
    <div class=" panel-body saved ">
        <div class="well-sm well clearfix">
            <span class="h4">Saved filter: <strong><g:enc>${filterName}</g:enc></strong></span>
            <a class="btn btn-danger btn-sm pull-right" data-toggle="modal" href="#deleteFilterModal">
                Delete this filter&hellip;
                <i class="glyphicon glyphicon-remove"></i>
            </a>

        </div>
    </div>
</g:if>

<div class="modal fade" id="deleteFilterModal" role="dialog" aria-labelledby="deleteFilterModalLabel" aria-hidden="true">
    <div class="modal-dialog">
        <div class="modal-content">
            <div class="modal-header">
                <button type="button" class="close" data-dismiss="modal" aria-hidden="true">&times;</button>
                <h4 class="modal-title" id="deleteFilterModalLabel">Delete Saved Filter</h4>
            </div>

            <div class="modal-body container">
                <div class="form-group">
                    <label class="control-label col-sm-2">Name: </label>
                    <div class="col-sm-10">
                        <span class="form-control-static obs_selected_filter_name" data-bind="text: filterName"><g:enc>${filterName}</g:enc></span>
                        <g:hiddenField name="delFilterName" value="${filterName}" data-bind="value: filterName" class="obs_selected_filter_name"/>
                    </div>
                </div>
                <g:if test="${ko}">
                    <div class="form-group ">
                        <label class="control-label col-sm-2">
                            Filter:
                        </label>

                        <div class="col-sm-10">
                            <span data-bind="text: filter" class="form-control-static "></span>
                        </div>
                    </div>
                </g:if>
            </div>
            <div class="modal-body">
                <span class="text-danger">Really delete this filter?</span>
            </div>

            <div class="modal-footer">
                <button type="button" class="btn btn-default" data-dismiss="modal">No</button>
                <g:if test="${deleteActionSubmit}">
                    <g:actionSubmit action="${deleteActionSubmit}" value="Yes" formmethod="POST" class="btn btn-danger"/>
                </g:if>
                <g:elseif test="${deleteActionSubmitRemote}">
                    <g:submitToRemote value="Yes" url="${deleteActionSubmitRemote}" update="${update}"
                                      class="btn btn-danger"/>
                </g:elseif>
            </div>
        </div><!-- /.modal-content -->
    </div><!-- /.modal-dialog -->
</div><!-- /.modal -->

%{--tokens used by the ajax call to delete node filter--}%
<g:jsonToken id="ajaxDeleteFilterTokens" />
<div class="modal fade" id="deleteFilterKOModal" role="dialog" aria-labelledby="deleteFilterKOModalLabel" aria-hidden="true"
>
    <div class="modal-dialog" >
        <div class="modal-content" data-bind="with: nodeSummary().filterToDelete()">
            <div class="modal-header">
                <button type="button" class="close" data-dismiss="modal" aria-hidden="true">&times;</button>
                <h4 class="modal-title" id="deleteFilterKOModalLabel">Delete Saved Filter</h4>
            </div>

            <div class="modal-body container">
                <div class="form-group">
                    <label class="control-label col-sm-2">Name: </label>
                    <div class="col-sm-10">
                        <span class="form-control-static"
                              data-bind="text: name"></span>
                    </div>
                </div>
                <div class="form-group ">
                    <label class="control-label col-sm-2">
                        Filter:
                    </label>

                    <div class="col-sm-10">
                        <span data-bind="text: filter" class="form-control-static "></span>
                    </div>
                </div>
            </div>
            <div class="modal-body">
                <span class="text-danger">Really delete this filter?</span>
            </div>

            <div class="modal-footer">
                <button type="button" class="btn btn-default" data-dismiss="modal">No</button>
                <button type="button" class="btn btn-danger" data-bind="click: $root.nodeSummary().deleteFilter">Yes</button>
            </div>
        </div><!-- /.modal-content -->
    </div><!-- /.modal-dialog -->
</div><!-- /.modal -->


<div class="modal fade" id="saveFilterModal" role="dialog" aria-labelledby="saveFilterModalLabel" aria-hidden="true">
    <div class="modal-dialog">
        <div class="modal-content">
            <div class="modal-header">
                <button type="button" class="close" data-dismiss="modal" aria-hidden="true">&times;</button>
                <h4 class="modal-title" id="saveFilterModalLabel">Save Filter</h4>
            </div>

            <div class="modal-body container">
                <div class="form-group">
                    <label for="newFilterName" class="control-label col-sm-2">Name:</label>

                    <div class="col-sm-10"><g:textField name="newFilterName" class="form-control input-sm"/></div>
                </div>
                <g:if test="${ko}">
                    <div class="form-group ">
                        <label class="control-label col-sm-2">
                            Filter:
                        </label>
                        <div class="col-sm-10">
                            <span data-bind="text: filter" class="form-control-static "></span>
                        </div>
                    </div>
                </g:if>
            </div>

            <div class="modal-footer">
                <button type="button" class="btn btn-default" data-dismiss="modal">Cancel</button>
                <g:if test="${storeActionSubmit}">
                    <g:actionSubmit value="Save Filter" action="${storeActionSubmit}" formmethod="POST" class="btn btn-primary"/>
                </g:if>
                <g:elseif test="${storeActionSubmitRemote}">
                    <g:submitToRemote value="Save Filter" url="${storeActionSubmitRemote}" update="${update}"
                                      class="btn btn-primary"/>
                </g:elseif>
            </div>
        </div><!-- /.modal-content -->
    </div><!-- /.modal-dialog -->
</div><!-- /.modal -->
