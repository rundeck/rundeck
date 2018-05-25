%{--
  - Copyright 2018 Rundeck, Inc. (http://rundeck.com)
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




<script type="text/html" id="acl-policy-ident">
<span data-bind="if: valid">
    <g:icon name="file"/>
</span>
<span data-bind="if: !valid()">

    <i class="glyphicon glyphicon-warning-sign text-warning has_tooltip"
       title="${message(code: "aclpolicy.format.validation.failed")}"></i>
</span>
<span data-bind="text: name"></span>
<span data-bind="text: resume()"></span>
</script>