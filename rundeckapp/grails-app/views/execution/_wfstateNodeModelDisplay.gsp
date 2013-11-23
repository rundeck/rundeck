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

<%@ page import="com.dtolabs.rundeck.core.execution.workflow.state.ExecutionState" %>

<div class="row">
<div class="col-sm-3">
    Node
</div>

<div class="col-sm-2">
    Step
</div>

<div class="col-sm-2">
    State
</div>

<div class="col-sm-2">
    Start time
</div>

<div class="col-sm-3">
    Duration
</div>
</div>

<g:render template="wfstateNodeDisplay" model="[node:'',template:'node']"/>
<g:each in="${workflowState.allNodes}" var="node" status="i">
    <g:render template="wfstateNodeDisplay" model="[node:node, workflowState: workflowState, i: i]"/>
</g:each>
