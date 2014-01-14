<%@ page import="com.dtolabs.rundeck.core.common.NodeEntryImpl" %>
    <table class="table table-condensed table-embed">
        <g:if test="${node.description}">
            <tr>
                <td class="value text-muted" colspan="2">
                    ${node.description?.encodeAsHTML()}
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
        </tr>
        <g:if test="${(!exclude || !exclude.contains('hostname') || !exclude.contains('username'))}">
            <tr>
                <td class="key">
                    <g:message code="node.metadata.username-at-hostname"/>
                </td>
                <td class="value">
                    <tmpl:nodeFilterLink key="username" value="${node['username']}"
                    />@<tmpl:nodeFilterLink key="hostname" value="${node['hostname']}"/>
                </td>
            </tr>
        </g:if>
        <g:if test="${(!exclude || !exclude.contains('tags')) && node['tags']}">
        <tr><td class="key"><i class="glyphicon glyphicon-tags text-muted"></i></td>
            <td class="">
                <span class="nodetags">
                    <g:each var="tag" in="${node.tags.sort()}">
                        <tmpl:nodeFilterLink key="tags" value="${tag}" linkclass="textbtn tag"/>
                    </g:each>
                </span>
            </td></tr>
        </g:if>
        <g:set var="nodeAttrs" value="${NodeEntryImpl.nodeExtendedAttributes(node)}"/>
        <g:if test="${nodeAttrs}">
            <g:each var="setting" in="${nodeAttrs.keySet().grep{nodeAttrs[it]}.sort()}">
                <tr>
                    <td class="key setting">
                        <tmpl:nodeFilterLink key="${setting}" value="${'.*'}" linktext="${setting}" suffix=":"/>
                    </td>
                    <td class="setting"><div class="value">
                        ${nodeAttrs[setting].encodeAsHTML()}
                        <tmpl:nodeFilterLink key="${setting}" value="${nodeAttrs[setting]}"
                                             linkclass="textbtn textbtn-info"
                                             linkicon="glyphicon glyphicon-search"/>
                    </div>
                    </td>
                </tr>
            </g:each>
        </g:if>
    </table>
