%{--
  Copyright 2014 SimplifyOps Inc, <http://simplifyops.com>

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

<g:ifExecutionMode active="true" project="${projectName}">
  <div class="card" data-ko-bind="nodeFilter">
    <div class="card-content">
      <div class="row">
        <div class="col-xs-12">
          <div data-bind="if: !loading() && !error() && (!total() || total()==0)" class="spacing text-warning" id="emptyerror">
            <span class="errormessage">
              <g:message code="no.nodes.selected.match.nodes.by.selecting.or.entering.a.filter" />
            </span>
          </div>

          <div data-bind="if: error()" class="spacing text-danger" id="loaderror2">
            <i class="glyphicon glyphicon-warning-sign"></i>
            <span class="errormessage" data-bind="text: error"></span>
          </div>

          <div data-bind="if: total() > 0 || loading()">
            <span data-bind="if: loading()" class="text-info">
              <i class="glyphicon glyphicon-time"></i>
              <g:message code="loading.matched.nodes" />
            </span>

            <span data-bind="if: !loading() && !error() && total() > 0">
              <span class="text-muted">
                <span data-bind="messageTemplate: [total(), nodesTitle()]">
                  <g:message code="count.nodes.matched" />
                </span>
              </span>

              <span data-bind="if: total() > maxShown()" class="text-strong">
                <span data-bind="messageTemplate: [maxShown(), total()]">
                  <g:message code="count.nodes.shown" />
                </span>
              </span>

              <div class="pull-right" style="margin-top: -5px">
                <a href="#" data-bind="attr: {href: viewInNodesPageUrl()}, click: function(){return true;}">
                  <g:message code="view.in.nodes.page.prompt" />
                </a>
              </div>
            </span>
          </div>

          <g:render template="/framework/nodesEmbedKO" model="[showLoading: true, showTruncated: true]"/>
        </div>
      </div>
    </div>
  </div>

  <div class="col-xs-12" data-ko-bind="adhocCommand">
    <div class="" id="runtab">
      <form id="runbox" action="adhoc" method="post" data-bind="submit: function(){return false;}">
        <input type="hidden" name="doNodedispatch" value="true" />

        <span class="input-group multiple-control-input-group tight">
          <span class="input-group-btn">
            <button type="button" class="btn btn-default dropdown-toggle act_adhoc_history_dropdown" data-toggle="dropdown">
              <g:message code="recent" /> <span class="caret"></span>
            </button>
            <ul class="dropdown-menu">
              <li data-bind="if: recentCommandsNoneFound()" role="presentation" class="dropdown-header">
                <g:message code="none" />
              </li>
              <li data-bind="if: !recentCommandsLoaded()" role="presentation" class="dropdown-header">
                <g:message code="loading.text" />
              </li>
              <li data-bind="if: recentCommandsLoaded() && !recentCommandsNoneFound()" role="presentation" class="dropdown-header">
                <g:message code="your.recently.executed.commands" />
              </li>
              <!-- ko foreach: recentCommands -->
              <li>
                <a href="#" data-bind="click: fillCommand, attr: {title: filter() || ''}" class="act_fill_cmd">
                  <i class="exec-status icon" data-bind="css: statusClass()"></i>
                  <span data-bind="text: title"></span>
                </a>
              </li>
              <!-- /ko -->
            </ul>
          </span>

          <input name="exec" size="50" type="text" 
                 data-bind="value: commandString, enable: allowInput, valueUpdate: 'keyup'"
                 placeholder="${g.message(code:'enter.a.command')}"
                 id="runFormExec" class="form-control" autofocus="true" />

          <span class="input-group-btn">
            <button class="btn btn-default has_tooltip" type="button" 
                    data-toggle="collapse" data-target="#runconfig"
                    data-placement="left" data-container="body"
                    title="${g.message(code:'node.dispatch.settings')}">
              <i class="glyphicon glyphicon-cog"></i>
            </button>

            <a class="btn btn-cta btn-fill runbutton" 
               data-bind="css: {disabled: !canRun() || running()}, 
                          click: function(){runFormSubmit('runbox'); return false;},
                          enable: canRun() && !running()">
              <span data-bind="if: !running()">
                <span data-bind="if: nodefilter.total() > 0">
                  <span data-bind="messageTemplate: [nodefilter.total(), nodefilter.nodesTitle()]">
                    <g:message code="run.on.count.nodes" />
                  </span>
                  <span class="glyphicon glyphicon-play"></span>
                </span>
                <span data-bind="if: nodefilter.total() < 1">No Nodes</span>
              </span>
              <span data-bind="if: running()">
                <g:message code="running1" />
              </span>
            </a>
          </span>
        </span>

        <div class="collapse well well-sm" id="runconfig">
          <div class="form form-inline">
            <h5 style="margin-top: 0">
              <g:message code="node.dispatch.settings" />
              <div class="pull-right">
                <button class="close" data-toggle="collapse" data-target="#runconfig">&times;</button>
              </div>
            </h5>

            <div class="">
              <div class="form-group has_tooltip" style="margin-top: 6px" 
                   title="${g.message(code:'maximum.number.of.parallel.threads.to.use')}" 
                   data-placement="bottom">
                <g:message code="thread.count" />
              </div>

              <div class="form-group" style="margin-top: 6px">
                <input min="1" type="number" name="nodeThreadcount" id="runNodeThreadcount" size="2"
                       placeholder="${g.message(code:'maximum.threadcount.for.nodes')}"
                       value="1" class="form-control input-sm" />
              </div>

              <div class="form-group" style="margin-top: 6px; margin-left: 20px">
                <g:message code="on.node.failure" />
              </div>

              <div class="form-group">
                <div class="radio">
                  <input type="radio" name="nodeKeepgoing" value="true" id="nodeKeepgoingTrue" />
                  <label class="has_tooltip" 
                         title="${g.message(code:'continue.to.execute.on.other.nodes')}" 
                         data-placement="bottom"
                         for="nodeKeepgoingTrue">
                    &nbsp;&nbsp;<g:message code="continue" />
                  </label>
                </div>
              </div>

              <div class="form-group">
                <div class="radio">
                  <input type="radio" name="nodeKeepgoing" value="false" id="nodeKeepgoingFalse" />
                  <label class="has_tooltip" 
                         title="${g.message(code:'do.not.execute.on.any.other.nodes')}" 
                         data-placement="bottom"
                         for="nodeKeepgoingFalse">
                    &nbsp;&nbsp;<g:message code="stop" />
                  </label>
                </div>
              </div>
            </div>
          </div>
        </div>
      </form>
    </div>
  </div>

  <div class="col-sm-12" data-ko-bind="adhocCommand">
    <div data-bind="if: error(), css: {show: error()}" id="runerror" class="alert alert-warning collapse">
      <span class="errormessage" data-bind="text: error"></span>
      <a class="close" data-toggle="collapse" href="#runerror" aria-hidden="true">&times;</a>
    </div>

    <div id="runcontent" class="card card-modified exec-output card-grey-header nodes_run_content execution-output-content" style="display: none; margin-top: 20px"></div>
  </div>
</g:ifExecutionMode>

<g:if test="${eventReadAuth}">
  <div id="activity_section" class="vue-ui-socket">
    <ui-socket section="project-activity" location="main"></ui-socket>
  </div>
</g:if>

