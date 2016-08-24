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

<%@ page import="com.dtolabs.rundeck.core.utils.OptsUtil" %>%{--
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
${enc(rawtext:prefix ?: '')}<g:link class="nodefilterlink ${enc(attr:linkclass)?:''}" action="nodes" params="${[filter: OptsUtil.join([key + ":",value]), project: params.project ?: request.project]}"
    data-node-filter="${enc(attr:OptsUtil.join([key + ":", value]))}"
    data-node-tag="${key=='tags'?value:''}"
title="Filter by ${enc(attr:key)} ${enc(attr:titletext?:'value')}"><g:if test="${linkicon}"><i class="${enc(attr:linkicon)}"></i></g:if
    ><g:else><g:enc>${linktext?:value}</g:enc></g:else></g:link>${enc(rawtext:suffix?:'')}
