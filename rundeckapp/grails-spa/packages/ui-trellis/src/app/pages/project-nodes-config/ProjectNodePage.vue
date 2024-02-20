<template>
  <div class="content">
    <div id="layoutBody">
      <div class="title">
        <span class="text-h3">
          <i class="fas fa-sitemap"></i> {{ $t("edit.nodes.header") }}
        </span>
      </div>
      <div class="container-fluid">
        <div class="row row-space-bottom">
          <div class="col-xs-12">
            <div class="card">
              <div class="card-content">
                <div class="vue-tabs">
                  <page-confirm
                    :event-bus="rundeckContext.eventBus"
                    class="pull-right"
                    :message="$t('page.unsaved.changes')"
                    :display="true"
                    style="display: inline-block"
                  >
                    <template v-slot:default="{ confirm }">
                      <div class="well well-sm">
                        <span class="text-warning">
                          {{ $t("page.unsaved.changes") }}
                        </span>
                        <span v-if="confirm.indexOf('Node Sources') >= 0">
                          <a
                            href="#node_sources"
                            @click.prevent="navigateTabs(1)"
                          >
                            <i class="fas fa-hdd fa-edit"></i>
                            {{ $t("project.node.sources.title.short") }}
                          </a>
                        </span>
                        <span v-if="confirm.indexOf('Node Enhancers') >= 0">
                          <a href="#plugins">
                            <i class="fas fa-puzzle-piece"></i>
                            {{
                              $t(
                                "framework.service.NodeEnhancer.label.short.plural",
                              )
                            }}
                          </a>
                        </span>
                      </div>
                    </template>
                  </page-confirm>
                  <tabs
                    v-model="activeTabKey"
                    custom-nav-class="nav-tabs-navigation"
                  >
                    <tab>
                      <template #title>
                        <div><i class="fas fa-pencil-alt"></i> Edit</div>
                      </template>
                      <div class="help-block">
                        {{ $t("modifiable.node.sources.will.appear.here") }}
                      </div>
                      <div class="project-plugin-config-vue">
                        <writeable-project-node-sources
                          :event-bus="rundeckContext.eventBus"
                          class="list-group"
                          item-css="list-group-item"
                        >
                          <template v-slot:empty>
                            <div class="list-group-item">
                              <span class="text-info"
                                ><i class="glyphicon glyphicon-info-sign"></i>
                                {{ $t("no.modifiable.sources.found") }}</span
                              >
                            </div>
                          </template>
                        </writeable-project-node-sources>
                      </div>

                      <div class="well well-sm">
                        <span>{{ $t("use.the.node.sources.tab.1") }}</span>
                        <a href="#" @click.prevent="navigateTabs(1)">
                          <i class="fas fa-hdd fa-edit"></i>
                          <span>{{
                            $t("project.node.sources.title.short")
                          }}</span>
                        </a>
                        <span>{{ $t("use.the.node.sources.tab.2") }}</span>
                      </div>
                    </tab>
                    <tab>
                      <template #title>
                        <div>
                          <i class="fas fa-hdd"></i>{{ $t("nodes.title") }}
                        </div>
                      </template>
                      <project-node-sources-config
                        :help="
                          $t('project.edit.ResourceModelSource.explanation')
                        "
                        :edit-mode="true"
                        :add-button-text="$t('add.node.source')"
                        :mode-toggle="false"
                        @saved="
                          rundeckContext.eventBus.emit(
                            'project-node-sources-saved',
                          )
                        "
                        @modified="
                          rundeckContext.eventBus.emit(
                            'page-modified',
                            'Node Sources',
                          )
                        "
                        @reset="
                          rundeckContext.eventBus.emit(
                            'page-reset',
                            'Node Sources',
                          )
                        "
                        :event-bus="rundeckContext.eventBus"
                      >
                      </project-node-sources-config>

                      <project-node-sources-help
                        :event-bus="rundeckContext.eventBus"
                      >
                      </project-node-sources-help>
                    </tab>
                    <tab>
                      <template #title>
                        <div>
                          <i class="fas fa-puzzle-piece"></i
                          >{{
                            $t(
                              "framework.service.NodeEnhancer.label.short.plural",
                            )
                          }}
                        </div>
                      </template>
                      <project-plugin-config
                        config-prefix="nodes.plugin"
                        service-name="NodeEnhancer"
                        :help="$t('framework.service.NodeEnhancer.explanation')"
                        :add-button-text="$t('add.node.enhancer')"
                        :edit-button-text="$t('edit.node.enhancers')"
                        :mode-toggle="false"
                        @modified="
                          rundeckContext.eventBus.emit(
                            'page-modified',
                            'Node Enhancers',
                          )
                        "
                        @reset="
                          rundeckContext.eventBus.emit(
                            'page-reset',
                            'Node Enhancers',
                          )
                        "
                        :event-bus="rundeckContext.eventBus"
                        :edit-mode="true"
                      >
                      </project-plugin-config>
                    </tab>
                    <tab>
                      <template #title>
                        <div><i class="fas fa-cog"></i>Configuration</div>
                      </template>
                      <project-configurable-form
                        :category="'resourceModelSource'"
                      >
                      </project-configurable-form>
                    </tab>
                  </tabs>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script lang="ts">
import { Tabs, Tab } from "uiv";
import ProjectConfigurableForm from "./ProjectConfigurableForm.vue";
import ProjectNodeSourcesConfig from "./ProjectNodeSourcesConfig.vue";
import ProjectNodeSourcesHelp from "./ProjectNodeSourcesHelp.vue";
import WriteableProjectNodeSources from "./WriteableProjectNodeSources.vue";
import ProjectPluginConfig from "./ProjectPluginConfig.vue";
import { getRundeckContext, RundeckContext } from "../../../library";
import PageConfirm from "../../../library/components/utils/PageConfirm.vue";

export default {
  name: "ProjectNodePage",
  components: {
    Tabs,
    Tab,
    ProjectConfigurableForm,
    WriteableProjectNodeSources,
    ProjectNodeSourcesConfig,
    ProjectNodeSourcesHelp,
    ProjectPluginConfig,
    PageConfirm,
  },
  data() {
    return {
      rundeckContext: getRundeckContext() as RundeckContext,
      activeTabKey: 1,
    };
  },
  methods: {
    navigateTabs(tabKey: number) {
      this.activeTabKey = tabKey;
    },
  },
};
</script>

<style></style>
