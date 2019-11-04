%{--
  - Copyright 2019 SimplifyOps, Inc. (http://simplifyops.com)
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
    _detailsScheduleDefinitions.gsp

    Author: Rodrigo Navarro <a href="mailto:rodrigo@rundeck.com">rodrigo@rundeck.com</a>
    Created: Oct 5, 2019 5:07:19 PM
 --%>
<g:embedJSON id="scheduleDataList" data="${scheduleDefinitions.collect{[name:it.name, crontabString: it.generateCrontabExression()]}}"/>
<g:javascript>
    jQuery(function(){
        "use strict";
        _loadScheduleDefinitionsData(loadJsonData('scheduleDataList'));
    });
</g:javascript>
<g:hiddenField name="schedulesDefinitionDataList"/>
