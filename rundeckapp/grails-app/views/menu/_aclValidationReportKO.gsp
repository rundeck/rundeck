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


<script type="text/html" id="acl-policy-validation">
<ul>
    <li data-bind="foreachprop: validation">

        <code data-bind="text: key"></code>
        <i  data-bind="bootstrapTooltip: true"
            data-placement="bottom"
            data-container="body"
            class="text-info glyphicon glyphicon-question-sign"
            title="${g.message(code:'acl.validation.error.sourceIdentity.help')}"></i>
        <ol data-bind="foreach: value">
            <li><code data-bind="text: $data"></code></li>
        </ol>

    </li>
</ul>
</script>
