<%@ page import="com.dtolabs.rundeck.core.common.FrameworkProject" contentType="text/html;charset=UTF-8" %>
<div class="content">
    <div id="layoutBody">
        <div class="title">
            <span class="text-h3"><i class="fas fa-sitemap"></i> ${g.message(code:"edit.nodes")}</span>
        </div>
        <div class="container-fluid">
            <div class="row">
                <div class="col-xs-12">
                    <g:render template="/common/messages" />
                </div>
            </div>

            <div class="row row-space-bottom">
                <div class="col-xs-12">
                    <div class="" id="createform">
                        <div class="card">
                            <div class="card-content vue-tabs">
                                <div class="nav-tabs-navigation">
                                    <div class="nav-tabs-wrapper">
                                        <ul class="nav nav-tabs" id="node_config_tabs">



                                            <li class="${writeableSources ? 'active' : ''}" id="tab_link_sources_writeable">
                                                <a href="#node_sources_writeable" data-toggle="tab">
                                                    <i class="fas fa-pencil-alt "></i>
                                                    <g:message code="button.Edit.label" />
                                                </a>
                                            </li>

                                            <li class="${writeableSources ? '' : 'active'}">
                                                <a href="#node_sources" data-toggle="tab" id="tab_link_sources">
                                                    <i class="fas fa-hdd"></i>
                                                    <g:message code="project.node.sources.title.short" />
                                                </a>
                                            </li>

                                            <feature:enabled name="enhancedNodes">

                                                <li id="tab_link_plugins">
                                                    <a href="#node_plugins" data-toggle="tab">
                                                        <i class="fas fa-puzzle-piece"></i>
                                                        <g:message code="framework.service.NodeEnhancer.label.short.plural" />
                                                    </a>
                                                </li>

                                            </feature:enabled>

                                            <li id="tab_link_settings">
                                                <a href="#node_settings" data-toggle="tab">
                                                    <i class="fas fa-cog"></i>
                                                    <g:message code="configuration" />
                                                </a>
                                            </li>
                                        </ul>
                                    </div>
                                </div>





                                %{--Shared page-confirm handler--}%
                                <div class="project-plugin-config-vue">
                                    <page-confirm :event-bus="EventBus" class="pull-right"
                                                  message="${enc(attr:message(code:'page.unsaved.changes'))}" :display="true"
                                                  style="display: inline-block">
                                        <template v-slot:default="{ confirm }">
                                            <div class="well well-sm">
                                                <span class="text-warning">
                                                    <g:message code="page.unsaved.changes" />:
                                                </span>
                                                <span v-if="confirm.indexOf('Node Sources')>=0">
                                                    <a href="#node_sources" onclick="jQuery('#tab_link_sources').tab('show')">
                                                        <i class="fas fa-hdd fa-edit"></i>
                                                        <g:message code="project.node.sources.title.short" />
                                                    </a>
                                                </span>
                                                <span v-if="confirm.indexOf('Node Enhancers')>=0">
                                                    <a href="#plugins" >
                                                        <i class="fas fa-puzzle-piece"></i>
                                                        <g:message code="framework.service.NodeEnhancer.label.short.plural" />
                                                    </a>
                                                </span>
                                            </div>
                                        </template>
                                    </page-confirm>
                                </div>
                                <div class="tab-content">
                                    <div class="tab-pane" id="node_settings">

                                    %{--updated UI--}%
                                        <g:form action="saveProjectNodeSources" method="post" useToken="true" class="form">


                                            <g:hiddenField name="project" value="${project}"/>
                                        <%--Render project configuration settings for 'resourceModelSource'--%>
                                            <g:render template="projectConfigurableForm"
                                                      model="${[extraConfigSet: extraConfig?.values(),
                                                                category      : 'resourceModelSource',
                                                                categoryPrefix: 'extra.category.resourceModelSource.',

                                                                helpCode      : 'project.configuration.extra.category.resourceModelSource.description'
                                                      ]}"/>




                                            <div class="card-footer">

                                                <g:submitButton name="save" value="${g.message(code: 'button.action.Save', default: 'Save')}" class="btn btn-cta reset_page_confirm"/>
                                            </div>
                                        </g:form>

                                    </div>



                                    <div class="tab-pane ${writeableSources ? 'active' : ''}"
                                         id="node_sources_writeable">

                                        <div class="help-block">
                                            <g:message code="modifiable.node.sources.will.appear.here" />
                                        </div>
                                        <div class="project-plugin-config-vue">
                                            <writeable-project-node-sources :event-bus="EventBus" class="list-group" item-css="list-group-item">
                                                <template v-slot:empty>
                                                    <div class="list-group-item">
                                                        <span class="text-info"><i class="glyphicon glyphicon-info-sign"></i> <g:message code="no.modifiable.sources.found" /></span>
                                                    </div>
                                                </template>
                                            </writeable-project-node-sources>
                                        </div>

                                        <div class="well well-sm">
                                            <g:message code="use.the.node.sources.tab.1" />
                                            <a href="#node_sources" onclick="jQuery('#tab_link_sources').tab('show')">
                                                <i class="fas fa-hdd fa-edit"></i>
                                                <g:message code="project.node.sources.title.short"/>
                                            </a>
                                            <g:message code="use.the.node.sources.tab.2" />
                                        </div>

                                    </div>


                                    <div class="tab-pane ${writeableSources ? '' : 'active'}" id="node_sources">


                                        <div class="project-plugin-config-vue">
                                            <project-node-sources-config
                                                    help="${enc(attr: g.message(code: "domain.Project.edit.ResourceModelSource.explanation"))}"
                                                    :edit-mode="true"
                                                    :mode-toggle="false"
                                                    @saved="EventBus.emit('project-node-sources-saved')"
                                                    @modified="EventBus.emit('page-modified','Node Sources')"
                                                    @reset="EventBus.emit('page-reset','Node Sources')"
                                                    :event-bus="EventBus">
                                            </project-node-sources-config>

                                            <project-node-sources-help :event-bus="EventBus">

                                            </project-node-sources-help>
                                        </div>
                                    </div>


                                    <feature:enabled name="enhancedNodes">
                                        <div class="tab-pane" id="node_plugins">
                                            <div class="project-plugin-config-vue">

                                                <project-plugin-config
                                                        config-prefix="nodes.plugin"
                                                        service-name="NodeEnhancer"
                                                        help="${enc(attr: g.message(code: 'framework.service.NodeEnhancer.explanation'))}"
                                                        edit-button-text="${enc(attr: g.message(code: 'edit.node.enhancers'))}"
                                                        :mode-toggle="false"
                                                        @modified="EventBus.emit('page-modified','Node Enhancers')"
                                                        @reset="EventBus.emit('page-reset','Node Enhancers')"
                                                        :event-bus="EventBus"
                                                        :edit-mode="true">

                                                </project-plugin-config>
                                            </div>

                                        </div>
                                    </feature:enabled>
                                </div>
                            </div>
                        </div>
                    </div>
                </div>
            </div>
        </div>
    </div>
</div>