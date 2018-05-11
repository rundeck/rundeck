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

<strong><g:message code="select.nodes.by.name" default="Select nodes by name"/>:</strong>
<p>
    <code><g:message code="mynode1.mynode2" default="mynode1 mynode2"/></code>
</p>
<p>
    <g:message code="this.will.select.both.nodes" default="This will select both nodes."/>
</p>

<strong><g:message code="filter.nodes.by.attribute.value" default="Filter nodes by attribute value"/>:</strong>
<ul>
    <li><g:message code="include" default="Include"/>: <code><g:message code="attribute" default="attribute"/>: <g:message code="value" default="value"/></code></li>

    <li><g:message code="exclude" default="Exclude"/>: <code>!<g:message code="attribute" default="attribute"/>: <g:message code="value" default="value"/></code></li>
</ul>


<strong><g:message code="use.regular.expressions" default="Use Regular Expressions:"/></strong>
<p>
    <code><g:message code="node.metadata.hostname" default="Hostname"/>: dev(\d+).test.com</code>.
</p>

<strong><g:message code="regex.syntax.checking" default="Regex syntax checking"/>:</strong>
<p>
    <code><g:message code="attribute" default="attribute"/>: /regex/</code>
</p>

<strong><g:message code="examples" default="Examples"/>:</strong>
<dl>
    <dt><g:message code="all.nodes" default="All Nodes"/></dt>
    <dd><g:link class="nodefilterlink" action="nodes" controller="framework" params="[filter:'.*',project:params.project?:request.project]"
        data-node-filter="name: .*"
    >name: .*</g:link> </dd>
    <dt><g:message code="nodes.tagged" default="Nodes tagged"/> "production"</dt>
    <dd><g:link class="nodefilterlink" action="nodes" controller="framework" params="[filter:'tags: production', project: params.project ?: request.project]"
        data-node-filter="tags: production"
    >tags: production</g:link> </dd>
    <dt><g:message code="unix.nodes" default="Unix nodes"/></dt>
    <dd><g:link class="nodefilterlink" action="nodes" controller="framework" params="[filter:'osFamily: unix', project: params.project ?: request.project]"
        data-node-filter="osFamily: unix"
    >osFamily: unix</g:link> </dd>
</dl>
