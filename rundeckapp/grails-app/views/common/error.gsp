<html>
<head>
    <meta http-equiv="Content-Type" content="text/html; charset=UTF-8"/>
    <meta name="layout" content="base"/>
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

<g:set var="pageTitle" value="${flash.title ?: request.title ?: request.titleCode ? g.message(code: request.titleCode) :
        g.message(code: 'request.error.title')}"/>
    <title><g:enc>${pageTitle}</g:enc></title>

</head>
<body>

<div class="row row-space">
<div class="col-sm-6 col-sm-offset-3">
    <div class="panel panel-default">
    <div class="panel-heading">
    <h2 class="panel-title "><g:enc>${pageTitle}</g:enc></h2>
    </div>
    <div class="panel-body text-danger">
        <g:render template="/common/messagesText"/>
    </div>
    </div>
</div>
</div>

</body>
</html>
