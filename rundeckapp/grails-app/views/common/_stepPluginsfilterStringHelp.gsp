%{--
  Copyright 2018 Rundeck, Inc. (http://rundeck.com)

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

<strong>Basic search:</strong>
<p>
    <code>mystep1</code>
</p>
<p>
    This will show steps that contains "mystep1" in the title, description, or name.
</p>

<strong>Filter by matching a specific field:</strong>

<ul>
    <li>description: <code>description=value</code></li>

    <li>name: <code>name=value</code></li>

    <li>title: <code>title=value</code></li>
</ul>

<p>Filter by matching an input property value:</p>
<ul>
    <li>property description: <code>property:description=value</code></li>

    <li>property title: <code>property:title=value</code></li>

    <li>property name: <code>property:name=value</code></li>
</ul>
