<table class="table table-condensed table-embed">
    <tbody>
    <tr data-bind="if: attributes.description">
        <td class="value text-muted" colspan="4" data-bind="text: attributes.description">

        </td>
    </tr>
    <tr data-bind="if: !authrun">
        <td class="value text-muted" colspan="4">
            <i class="glyphicon glyphicon-ban-circle"></i>
            <g:message code="node.access.not-runnable.message" />
        </td>
    </tr>
    <tr>
        %{--OS details--}%
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
    <tr>
        <td class="key">
            <i class="" data-bind="css: {'glyphicon glyphicon-tags text-muted': tags().size()>0}"></i>
        </td>
        <td class="" colspan="3">
            <span data-bind="if: tags">
                <span class="nodetags">
                    <span data-bind="foreach: tags">

                        <node-filter-link params="
                                                    filterkey: 'tags',
                                                    filterval: $data,
                                                    tag: $data
                                                    "></node-filter-link>


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
                                        linkicon: 'glyphicon glyphicon-search'
                                        "></node-filter-link>
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
               data-bind="attr: { href: '#ns_'+$index()+'_'+$parentContext.$index()}"
                data-toggle="collapse"
                  class="textbtn textbtn-muted textbtn-saturated ">
                <span data-bind="text: namespace.ns"></span>
                (<span data-bind="text: namespace.values.size()"></span>)
                <i class="auto-caret "></i>
            </a>
        </td>
        <td colspan="3"></td>
    </tr>
        <tbody class="subattrs collapse collapse-expandable" data-bind="attr: {id: 'ns_'+$index()+'_'+$parentContext.$index()}" >
    <tr >
        <td colspan="4">
            <table class="table table-condensed table-embed">
                <tbody data-bind="foreach: { data: $data.values , as: 'nsattr' }" >

                <tr class="hover-action-holder ">
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
                        </div>
                    </td>
                </tr>

                </tbody>
            </table>
        </td>
    </tr>
        </tbody>
    <!-- /ko -->


    </g:if>

</table>