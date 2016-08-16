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
  Created by IntelliJ IDEA.
  User: greg
  Date: 10/3/13
  Time: 12:19 PM
  To change this template use File | Settings | File Templates.
--%>

<%@ page contentType="text/html;charset=UTF-8" %>
<html>
<head>
    <meta name="layout" content="base"/>
    <meta name="meta.tabpage" content="configure"/>
    <title>Metrics Links</title>
</head>
<body>

<div class="row">
    <div class="col-sm-3">
        <g:render template="configNav" model="[selected: 'metrics']"/>
    </div>

    <div class="col-sm-9">
<g:markdown>
* [Metrics](${g.createLink(uri: '/metrics/metrics?pretty=true')}) (json)
* [Ping](${g.createLink(uri:'/metrics/ping')})
* [Threads](${g.createLink(uri: '/metrics/threads')})
* [Healthcheck](${g.createLink(uri: '/metrics/healthcheck')})  (json)
</g:markdown>
        </div>
    </div>

</body>
</html>
