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
<div style="margin-top:10px">
  <table class="table table-condensed table-embed node-details-simple">
      <tbody>
      <tr data-bind="if: attributes.description">
          <td class="value text-strong" colspan="4" data-bind="text: attributes.description">

          </td>
      </tr>
      <tr data-bind="if: !authrun">
          <td class="value text-strong" colspan="4">
              <i class="glyphicon glyphicon-ban-circle"></i>
            <g:message code="node.access.not-runnable.message" />
          </td>
      </tr>
      <tr data-bind="if: attributes['ui:status:icon'] || attributes['ui:status:text']">

          <td class="key">
              <g:message code="node.metadata.status"/>
          </td>
          <td class="value">

              <span data-bind="css: $root.nodeSet().statusIconCss(attributes), style: $root.nodeSet().statusIconStyle(attributes), attr: {title: attributes['ui:status:text']}">
                  <!-- ko if: attributes['ui:status:icon'] -->
                  <!-- ko with: attributes['ui:status:icon']() -->
                  <i  data-bind="css: $root.nodeSet().glyphiconCss($data)"></i>
                  <!-- /ko -->
                  <!-- /ko -->

                  <!-- ko if: attributes['ui:status:text'] -->
                  <span  data-bind="text: attributes['ui:status:text']"></span>
                  <!-- /ko -->
              </span>
          </td>
      </tr>
      <tr>
          %{--OS details--}%
          <!-- ko if: $root.nodeSet().hasOsData(attributes) -->
          <td class="key">
              <g:message code="node.metadata.os"/>
          </td>
          <td class="value">
              <span data-bind="foreach: ['osName','osFamily','osVersion','osArch']">
                  <!-- ko if: $parent.attributes[$data] -->

                  <node-filter-link params="
                                          filterkey: $data,
                                          filterval: $parent.attributes[$data],
                                          textcss: $data=='osFamily' || $data=='osArch' ? 'text-parenthetical' : null
                                          "></node-filter-link>

                  <!-- /ko -->
              </span>

          </td>
          <!-- /ko -->

          <!-- ko if: $root.useDefaultColumns -->
          <td class="key"><g:message code="node.metadata.username-at-hostname"/></td>
          <td>
              <span data-bind="if: attributes.username">

                  <node-filter-link params="
                                              filterkey: 'username',
                                              filterval: attributes.username,
                                              "></node-filter-link>


              </span>
              <span class="atsign">@</span>
              <span data-bind="if: attributes.hostname">

                  <node-filter-link params="
                                              filterkey: 'hostname',
                                              filterval: attributes.hostname,
                                              "></node-filter-link>


              </span>
          </td>
          <!-- /ko -->


      </tr>
      %{-- unless exclude tags --}%
      <tr data-bind="if: tags().length > 0">
          <td class="key">
            <g:message code="node.metadata.tags"/>
          </td>
          <td class="" colspan="3">
              <span data-bind="if: tags">
                  <span class="nodetags">
                      <span data-bind="foreach: tags">

                          <span class="label label-muted">
                              <span data-bind="text: $data"></span>

                              <node-filter-link params="
                                              filterkey: 'tags',
                                              filterval: $data,
                                              classnames: 'textbtn textbtn-info textbtn-saturated hover-action',
                                              linkicon: 'glyphicon glyphicon-plus text-success'
                                              "></node-filter-link>
                                <g:if test="${showExcludeFilterLinks}">
                                    <node-exclude-filter-link class="text-danger" params="
                                              filterkey: 'tags',
                                              filterval: $data,
                                              classnames: 'textbtn textbtn-info textbtn-saturated hover-action',
                                              linkicon: 'glyphicon glyphicon-minus text-danger'

                                              "></node-exclude-filter-link>
                                </g:if>
                          </span>
                      </span>
                  </span>
              </span>
          </td>
      </tr>
      </tbody>
      %{-- node attributes with no namespaces--}%
      <g:set var="koattrs" value="${useNamespace?'$root.nodeSet().displayAttributes(node.attributes)':'node.attributes'}"/>
      <tbody  data-bind="foreachprop: ${koattrs}">
      <tr class="hover-action-holder" >
          <td class="key setting">
              <node-filter-link params="
                                          filterkey: key,
                                          filterval: '.*',
                                          suffix: ':',
                                          linktext: key,
                                          "></node-filter-link>
          </td>
          <td class="setting" colspan="3">
              <div class="value">
                  <span data-bind="text: value()"></span>
                  <node-filter-link params="
                                          filterkey: key,
                                          filterval: value(),
                                          classnames: 'textbtn textbtn-info textbtn-saturated hover-action',
                                          linkicon: 'glyphicon glyphicon-zoom-in'
                                          "></node-filter-link>
                  <g:if test="${showExcludeFilterLinks}">
                      <node-exclude-filter-link class="text-danger" params="
                                          filterkey: key,
                                          filterval: value(),
                                          classnames: 'textbtn textbtn-info textbtn-saturated hover-action',
                                          linkicon: 'glyphicon glyphicon-zoom-out text-danger'

                                          "></node-exclude-filter-link>
                  </g:if>
              </div>

          </td>
      </tr>
      </tbody>

      <g:if test="${useNamespace}">
      %{--node attributes with namespaces--}%

      <!-- ko foreach: { data: $root.nodeSet().attributeNamespaces(node.attributes), as: 'namespace' } -->
      <tr class="">
          <td class="key namespace">
              <a href="#"
                 data-bind="attr: { href: '#ns_${crefText}_'+$index()+'_'+$parentContext.$index()}"
                  data-toggle="collapse"
                    class="textbtn textbtn-muted textbtn-saturated ">
                  <span data-bind="text: namespace.ns"></span>
                  <i class="auto-caret "></i>
              </a>
          </td>
          <td colspan="3" class="text-muted">
              <span data-bind="text: namespace.values.length"></span>
          </td>
      </tr>
          <tbody class="subattrs collapse collapse-expandable" data-bind="attr: {id: 'ns_${crefText}_'+$index()+'_'+$parentContext.$index()}" >
          <!-- ko foreach: { data: $data.values , as: 'nsattr' } -->
                <tr  class="hover-action-holder">

                      <td class="key setting">

                          <node-filter-link params="
                                                          filterkey: $data.name,
                                                          filterval: '.*',
                                                          suffix: ':',
                                                          linktext: $data.shortname,
                                                          "></node-filter-link>
                      </td>
                      <td class="setting " colspan="3">
                          <div class="value">
                              <span data-bind="text: $data.value"></span>
                              <node-filter-link params="
                                                              filterkey: $data.name,
                                                              filterval: $data.value,
                                                              classnames: 'textbtn textbtn-info textbtn-saturated hover-action',
                                                              linkicon: 'glyphicon glyphicon-search'
                                                              "></node-filter-link>
                              <g:if test="${showExcludeFilterLinks}">
                                  <node-exclude-filter-link class="text-danger" params="
                                          filterkey: $data.name,
                                          filterval: $data.value,
                                          classnames: 'textbtn textbtn-info textbtn-saturated hover-action',
                                          linkicon: 'glyphicon glyphicon-zoom-out text-danger'

                                          "></node-exclude-filter-link>
                              </g:if>
                          </div>
                      </td>

                </tr>
          <!-- /ko -->
          </tbody>
      <!-- /ko -->


      </g:if>

  </table>
</div>
