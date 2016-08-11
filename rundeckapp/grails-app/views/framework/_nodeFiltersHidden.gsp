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
  --}% <%--
    _nodeFilterHidden.gsp

    Author: Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
    Created: Apr 14, 2010 4:07:58 PM
    $Id$
 --%>
<g:hiddenField name="formInput" value="true"/>

<input type="hidden" name="filter" value="${enc(attr:query?.filter)}"  class="hiddenNodeFilter" data-bind="value: filter"/>
<input type="hidden" name="filterName" value="${enc(attr:filterName?:'')}"  class="hiddenNodeFilterName" data-bind="value: filterName"/>

<input type="hidden" name="nodeExcludePrecedence" value="${enc(attr:query?.nodeExcludePrecedence)}" />
