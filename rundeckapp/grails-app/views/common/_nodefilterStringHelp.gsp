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

<strong>Select nodes by name:</strong>
<p>
    <code>mynode1 mynode2</code>
</p>
<p>
    This will select both nodes.
</p>

<strong>Filter nodes by attribute value:</strong>
<ul>
    <li>Include: <code>attribute: value</code></li>

    <li>Exclude: <code>!attribute: value</code></li>
</ul>


<strong>Use Regular Expressions:</strong>
<p>
    <code>hostname: dev(\d+).test.com</code>.
</p>

<strong>Regex syntax checking:</strong>
<p>
    <code>attribute: /regex/</code>
</p>
<p>
    <code>attribute: /regex/</code>
</p>

<strong>Examples:</strong>
<dl>
    <dt>All nodes</dt>
    <dd><g:link class="nodefilterlink" action="nodes" controller="framework" params="[filter:'.*',project:params.project?:request.project]"
        data-node-filter="name: .*"
    >name: .*</g:link> </dd>
    <dt>Nodes tagged "production"</dt>
    <dd><g:link class="nodefilterlink" action="nodes" controller="framework" params="[filter:'tags: production', project: params.project ?: request.project]"
        data-node-filter="tags: production"
    >tags: production</g:link> </dd>
    <dt>Unix nodes</dt>
    <dd><g:link class="nodefilterlink" action="nodes" controller="framework" params="[filter:'osFamily: unix', project: params.project ?: request.project]"
        data-node-filter="osFamily: unix"
    >osFamily: unix</g:link> </dd>
</dl>
