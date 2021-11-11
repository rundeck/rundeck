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

<g:set var="sidebarColor" value="${g.rConfig(value: "gui.sidebarColor", type: 'string')}"/>
<g:set var="sidebarTextColor" value="${g.rConfig(value: "gui.sidebarTextColor", type: 'string')}"/>
<g:set var="sidebarTextActiveColor" value="${g.rConfig(value: "gui.sidebarTextActiveColor", type: 'string')}"/>

<g:set var="instanceNameLabelColor" value="${g.rConfig(value: "gui.instanceNameLabelColor", type: 'string')}"/>
<g:set var="instanceNameLabelTextColor" value="${g.rConfig(value: "gui.instanceNameLabelTextColor", type: 'string')}"/>

<style type="text/css">
  <g:if test="sidebarColor">
    .sidebar:after,
    .sidebar:before,
    .off-canvas-sidebar:after,
    .off-canvas-sidebar:before {
        background-color: ${sidebarColor} !important;
    }
  </g:if>
  <g:if test="sidebarTextColor">
    .sidebar .nav li:not(.active) > a,
    .off-canvas-sidebar .nav li:not(.active) > a {
      color: ${sidebarTextColor} !important;
    }
  </g:if>
  <g:if test="sidebarTextActiveColor">
    .sidebar .nav li.active > a,
    .off-canvas-sidebar .nav li.active > a,
    .off-canvas-sidebar .nav li.active > a,
    .off-canvas-sidebar .nav li.active > a {
      color: ${sidebarTextActiveColor} !important;
    }
  </g:if>

  <g:if test="instanceNameLabelColor">
    .label-default.instance-label{
      background-color: ${instanceNameLabelColor} !important;
    }
  </g:if>

  <g:if test="instanceNameLabelTextColor">
    .label-default.instance-label{
      color: ${instanceNameLabelTextColor} !important;
    }
  </g:if>
</style>
