%{--
  - Copyright 2019 Rundeck, Inc. (http://rundeck.com)
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

<%@ page import="com.opensymphony.module.sitemesh.RequestConstants" %>

<g:set var="buildIdent" value="${servletContextAttribute(attribute: 'app.ident')}"/>
<g:set var="appId" value="${g.appTitle()}"/>


<footer class="footer">
    <div class="footer__group footer__group--left">
        <nav>
            <ul>
                <li>
                    <a href="${enc(attr: g.helpLinkUrl())}" class="help " target="_blank">
                        <g:message code="help"/>
                    </a>
                </li>
                <li>
                    <g:ifServletContextAttributeExists attribute="CLUSTER_MODE_ENABLED">
                        <g:ifServletContextAttribute attribute="CLUSTER_MODE_ENABLED" value="true">
                            <g:set var="clusterIdentityInFooter" value="${g.rConfig(value: "gui.clusterIdentityInFooter", type: 'string')}"/>

                            <g:if test="${clusterIdentityInFooter in [true, 'true']}">
                                <span class="rundeck-server-uuid"
                                      data-server-uuid="${enc(attr: servletContextAttribute(attribute: 'SERVER_UUID'))}"
                                      data-server-name="${enc(attr: servletContextAttribute(attribute: 'FRAMEWORK_NODE'))}">
                                </span>
                            </g:if>
                        </g:ifServletContextAttribute>
                    </g:ifServletContextAttributeExists>
                </li>
            </ul>
        </nav>
    </div>
    <div class="footer__group footer__group--right">
        <div class="copyright">
            &copy; Copyright ${java.time.LocalDate.now().getYear()} <a href="http://pagerduty.com">PagerDuty, Inc.</a>

            All rights reserved.
        </div>
    </div>
  </div>
</footer>
