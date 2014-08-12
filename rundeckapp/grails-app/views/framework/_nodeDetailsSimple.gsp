<%@ page import="com.dtolabs.rundeck.core.common.NodeEntryImpl" %>
    <table class="table table-condensed table-embed">
        <g:if test="${node.description}">
            <tr>
                <td class="value text-muted" colspan="4">
                    <g:enc>${node.description}</g:enc>
                </td>
            </tr>
        </g:if>
        <g:if test="${!runnable}">
        <tr>
            <td class="value text-muted" colspan="4">
                <i class="glyphicon glyphicon-ban-circle"></i>
                <g:message code="node.access.not-runnable.message" />
            </td>
        </tr>
        </g:if>
        <tr>
            <td class="key">
                <g:message code="node.metadata.os"/>
            </td>
            <td class="value">
                <g:each in="['osName','osFamily','osVersion','osArch']" var="oskey">
                    <g:if test="${node[oskey]}">
                        <g:set var="useparens" value="${oskey in ['osFamily', 'osArch']}"/>
                        <tmpl:nodeFilterLink
                            prefix="${useparens?'(':''}"
                            suffix="${useparens ? ')' : ''}"
                            key="${oskey}" value="${node[oskey]}"
                        />
                    </g:if>
                </g:each>
            </td>
            <g:if test="${(!exclude || !exclude.contains('hostname') || !exclude.contains('username'))}">
                <td class="key"><g:message code="node.metadata.username-at-hostname"/></td>
                <td>
                    <g:if test="${node.username}">
                        <tmpl:nodeFilterLink key="username" value="${node['username']}"/>
                        <span class="atsign">@</span>
                    </g:if>
                    <tmpl:nodeFilterLink key="hostname" value="${node['hostname']}"/>
                </td>
            </g:if>
        </tr>

        <g:if test="${(!exclude || !exclude.contains('tags')) && node['tags']}">
        <tr><td class="key"><i class="glyphicon glyphicon-tags text-muted"></i></td>
            <td class="" colspan="3">
                <span class="nodetags">
                    <g:each var="tag" in="${node.tags.sort()}">
                        <tmpl:nodeFilterLink key="tags" value="${tag}" linkclass="textbtn tag"/>
                    </g:each>
                </span>
            </td></tr>
        </g:if>
        <g:if test="${useNamespace}">
            <g:set var="nkey" value="${g.rkey()}"/>
            <g:set var="nodeNamespaces" value="${NodeEntryImpl.nodeNamespacedAttributes(node)}"/>
            <g:if test="${nodeNamespaces}">
                <g:each var="nsname" in="${nodeNamespaces.keySet().grep { nodeNamespaces[it] }.sort()}">
                <g:set var="nsAttrs" value="${nodeNamespaces[nsname]}"/>
                <g:if test="${nsname!=''}">
                <tr>
                    <td class="key namespace">
                        <g:expander key="${nkey}_ns_${nsname}" classnames="textbtn-muted textbtn-saturated"><g:enc>${nsname} (${nsAttrs.size()})</g:enc></g:expander>
                    </td>
                </tr>
                </g:if>
                    <tbody id="${enc(attr:nkey+'_ns_'+nsname)}"  style="${wdgt.styleVisible(if:nsname=='')}" class="${nsname!=''?'subattrs':''}">
                        <g:if test="${nsAttrs?.size()>0}">
                            <g:each var="inNsAttrName" in="${nsAttrs.keySet().findAll{nsAttrs[it]?.get(1)}.sort()}">
                                <g:set var="origAttrName" value="${nsAttrs[inNsAttrName][0]}"/>
                                <g:set var="value" value="${nsAttrs[inNsAttrName][1]}"/>
                                <tr class="hover-action-holder">
                                    <td class="key setting">
                                        <tmpl:nodeFilterLink key="${origAttrName}" value="${'.*'}" linktext="${inNsAttrName}"
                                                             titletext="(any value)"
                                                             suffix=":"/>
                                    </td>
                                    <td class="setting " colspan="3">
                                        <div class="value">
                                            <g:enc>${value}</g:enc>
                                            <tmpl:nodeFilterLink key="${origAttrName}" value="${value}"
                                                                 linkclass="textbtn textbtn-info textbtn-saturated hover-action"
                                                                 linkicon="glyphicon glyphicon-search "/>
                                        </div>
                                    </td>
                                </tr>
                            </g:each>
                        </g:if>
                        </tbody>

                </g:each>
            </g:if>
        </g:if>
        <g:else>
        <g:set var="nodeAttrs" value="${NodeEntryImpl.nodeExtendedAttributes(node)}"/>
        <g:if test="${nodeAttrs}">
            <g:each var="setting" in="${nodeAttrs.keySet().grep{nodeAttrs[it]}.sort()}">
                <tr class="hover-action-holder">
                    <td class="key setting">
                        <tmpl:nodeFilterLink key="${setting}" value="${'.*'}" linktext="${setting}" suffix=":"/>
                    </td>
                    <td class="setting" colspan="3">
                        <div class="value">
                        <g:enc>${nodeAttrs[setting]}</g:enc>
                        <tmpl:nodeFilterLink key="${setting}" value="${nodeAttrs[setting]}"
                                             linkclass="textbtn textbtn-info textbtn-saturated hover-action"
                                             linkicon="glyphicon glyphicon-search "/>
                    </div>
                    </td>
                </tr>
            </g:each>
        </g:if>
        </g:else>
    </table>
