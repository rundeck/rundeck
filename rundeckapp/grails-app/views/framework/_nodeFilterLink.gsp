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
${prefix ?: ''}<g:link class="nodefilterlink textbtn tag" action="nodes" params="${[filter: OptsUtil.join([key + ":",value])]}"
    data-node-filter="${OptsUtil.join([key + ":", value])}"
title="Filter by ${key.encodeAsHTML()} value"><g:if test="${linkicon}"><i class="${linkicon}"></i></g:if
    ><g:else>${(linktext?:value)?.encodeAsHTML()}</g:else></g:link>${suffix?:''}
