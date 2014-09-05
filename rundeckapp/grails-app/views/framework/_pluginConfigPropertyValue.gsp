%{--
  - Copyright 2011 DTO Labs, Inc. (http://dtolabs.com)
  -
  - Licensed under the Apache License, Version 2.0 (the "License");
  - you may not use this file except in compliance with the License.
  - You may obtain a copy of the License at
  -
  -        http://www.apache.org/licenses/LICENSE-2.0
  -
  - Unless required by applicable law or agreed to in writing, software
  - distributed under the License is distributed on an "AS IS" BASIS,
  - WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  - See the License for the specific language governing permissions and
  - limitations under the License.
  --}%
 <%--
    _pluginConfigPropertyValue.gsp
    
    Author: Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
    Created: 7/28/11 12:03 PM
 --%>
<%@ page import="com.dtolabs.rundeck.core.plugins.configuration.StringRenderingConstants" contentType="text/html;charset=UTF-8" %>
<g:set var="propValue" value="${enc(attr:values[prop.name])}"/>
<g:if test="${includeFormFields && values[prop.name]}">
    <input type="hidden" name="${enc(attr:prefix+ 'config.'+prop.name)}" value="${propValue}"/>
</g:if>
