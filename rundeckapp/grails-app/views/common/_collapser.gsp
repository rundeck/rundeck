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
 bootstrap collapse version of _expander.gsp
--%><span class="${enc(attr:classnames?:'')} ${classnames&&classnames.indexOf('btn-')>=0?'': 'btn-link'} btn  ${open=='true'?'active':''}" data-toggle="collapse" data-target="#${key}" id="_exp_${enc(attr:key)}"><!--
--><g:enc rawtext="${text != null && !imgfirst ? text:''}"/><!--
--> <i class="glyphicon glyphicon-chevron-${open == 'true' ? 'down' : 'right'}"></i> <!--
--><g:enc rawtext="${text != null && imgfirst ? text:''}"/><!--
--></span>
