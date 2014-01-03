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
                <g:if test="${node['osName']}">
                    ${node['osName'].encodeAsHTML()}
                </g:if>
                <g:if test="${node['osFamily']}">
                    (${node['osFamily'].encodeAsHTML()})
                </g:if>
                <g:if test="${node['osVersion']}">
                    ${node['osVersion'].encodeAsHTML()}
                </g:if>
                <g:if test="${node['osArch']}">
                    (${node['osArch'].encodeAsHTML()})
                </g:if>
            </td>
        </tr>
        <g:if test="${(!exclude || !exclude.contains('hostname') || !exclude.contains('username'))}">
            <tr>
                <td class="key">
                    <g:message code="node.metadata.username-at-hostname"/>
                </td>
                <td class="value">
                    ${node['username']?.encodeAsHTML()}@${node['hostname']?.encodeAsHTML()}
                </td>
            </tr>
        </g:if>
        <g:if test="${(!exclude || !exclude.contains('tags')) && node['tags']}">
        <tr><td class="key"><i class="glyphicon glyphicon-tags text-muted"></i></td>
            <td class="">
                <span class="nodetags">
                    <g:each var="tag" in="${node.tags.sort()}">
                        <g:link class="tag textbtn" action="nodes" params="${[nodeIncludeTags: tag]}"
                                title="Filter by tag: ${tag.encodeAsHTML()}">
                            ${tag.encodeAsHTML()}</g:link>
                    %{--<span class="action textbtn" onclick="setTagFilter('${tag.encodeAsJavaScript()}');" title="Add to existing filter">+</span>--}%
                    </g:each>
                </span>
            </td></tr>
        </g:if>
        <g:set var="nodeAttrs" value="${NodeEntryImpl.nodeExtendedAttributes(node)}"/>
        <g:if test="${nodeAttrs}">
            <g:each var="setting" in="${nodeAttrs.keySet().grep{nodeAttrs[it]}.sort()}">
                <tr>
                    <td class="key setting">${setting.encodeAsHTML()}:</td>
                    <td class="setting"><div class="value">${nodeAttrs[setting]?.encodeAsHTML()}</div></td>
                </tr>
            </g:each>
        </g:if>
    </table>
