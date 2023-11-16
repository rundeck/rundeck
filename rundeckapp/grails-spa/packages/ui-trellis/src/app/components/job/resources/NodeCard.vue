<template>
  <div class="card">
    <div class="card-content vue-tabs">
      <!-- NAV TABS -->
      <div class="nav-tabs-navigation">
        <div class="nav-tabs-wrapper">
          <ul class="nav nav-tabs">
            <li class="active" id="tab_link_summary">
              <a href="#summary" data-toggle="tab">
                {{ $t("browse") }}
              </a>
            </li>
            <li id="tab_link_result" v-if="allCount >=2">
              <a href="#result" data-toggle="tab" v-if="filterIsSet">
                {{ $t("result") }}
                <template v-if="allCount>=2">
                  <span class="text-info" style="margin-right: 4px">{{ total }} </span>
                  <span data-bind="text: nodesTitle">Node {{1 !== total ? 's' : ''}}</span>
                </template>
                <span v-else class="text-strong">&hellip;</span>
              </a>
              <a v-else>
                {{ $t("enter.a.filter") }}
              </a>
            </li>
          </ul>
        </div>
        <span v-if="allCount>=2" class="pull-right">
            <span class="tabs-sibling tabs-sibling-compact">
                <div class="btn-group pull-right ">
                  <button class="btn btn-default btn-sm dropdown-toggle" data-toggle="dropdown">
                      {{$t("actions")}} <span class="caret"></span>
                  </button>
                  <ul class="dropdown-menu" role="menu">
                    <li v-if="executionMode === 'active' && hasNodes" :class="{'disabled': !runAuthorized}">
                        <a href="#"
                           @click="runCommand"
                           :title="notAuthorized()"
                           :class="{'has_tooltip': !runAuthorized}"
                           data-placement="left"
                        >
                            <i class="glyphicon glyphicon-play"></i>
                            <span> {{ $t("run.a.command.on.count.nodes.ellipsis",[total, nodesTitle]) }} </span>
                      </a>
                    </li>
                    <li v-else-if="hasNodes" class="disabled">
                        <a href="#"
                           :title="$t('disabled.execution.run')"
                           class="has_tooltip"
                           data-placement="left"
                        >
                            <i class="glyphicon glyphicon-play"></i>
                            <span> {{ $t("run.a.command.on.count.nodes.ellipsis",[total, nodesTitle]) }} </span>
                        </a>
                    </li>
                    <li :class="{'disabled': !jobCreateAuthorized}">
                        <a href="#"
                           @click="saveJob"
                           :title="notAuthorized(jobCreateAuthorized)"
                           :class="{'has_tooltip': !jobCreateAuthorized}"
                           data-placement="left"
                        >
                            <i class="glyphicon glyphicon-plus"></i>
                            <span>
                              {{ $t("create.a.job.for.count.nodes.ellipsis",[total, nodesTitle]) }}
                            </span>
                        </a>
                    </li>
                  </ul>
              </div>
            </span>
        </span>
      </div>



      <!-- TABS -->
      <div class="tab-content">
        <div class="tab-pan" id="result">
          <div v-if="error" class="row row-space">
            <div class="col-sm-12">
              <span class="text-danger">
                  <i class="glyphicon glyphicon-warning-sign"></i>
                  <span>{{ error }}</span>
              </span>
            </div>
          </div>
        </div>
        <div class="tab-pane active" id="summary">
          <div class="row">
            <div class="col-xs-6">
              <h5 class="column-title text-uppercase text-strong">
                {{ $t("resource.metadata.entity.tags") }}
              </h5>
              <ul v-for="(tag,index) in nodeSummary.tags" class="list-unstyled">
                <li style="display:inline;" :key="index">
                  <node-filter-link
                    filterKey="tags"
                    :filterVal="tag"

                  />
<!--                  <node-filter-link  params="-->
<!--                                    classnames: 'label label-muted',-->
<!--                                    filterkey: 'tags',-->
<!--                                    filterval: tag,-->
<!--                                    tag: tag,-->
<!--                                    count: count-->
<!--                                ">-->
<!--                  </node-filter-link>-->
                </li>
              </ul>
              <div v-if="nodeSummary && !nodeSummary.hasOwnProperty('tags') || nodeSummary.tags && nodeSummary.tags!.length === 0">
                {{ $t("none") }}
              </div>
            </div>

            <div class="col-xs-6">
              <h5 class="column-title text-uppercase text-strong">
                {{ $t("filters") }}
              </h5>


              <ul class="list-unstyled">
                <li>
                  <a class="nodefilterlink btn btn-default btn-xs" data-node-filter=".*" data-node-filter-all="true">
                    {{ $t("all.nodes") }} {{ nodeSummary?.totalCount }}
                  </a>
                  <div class="btn-group" style="margin-left: 4px">
                    <button type="button"
                            class="btn btn-default btn-xs btn-simple dropdown-toggle"
                            style="padding: 0 5px;"
                            title="Filter Actions"
                            data-toggle="dropdown"
                            aria-expanded="false">
                      <span class="caret"></span>
                    </button>
                    <ul class="dropdown-menu" role="menu">
                      <li>
                        <a href="#" @click="isDefaultFilter? removeDefaultFilter: setDefaultFilter">
                          <i class="glyphicon" :class="[isDefaultFilter? 'glyphicon-ban-circle': 'glyphicon-filter']"></i>
                          {{
                            isDefaultFilter
                                ? $t('remove.all.nodes.as.default.filter')
                                : $t("set.all.nodes.as.default.filter")
                          }}
                        </a>
                      </li>
                    </ul>
                  </div>
                </li>
              </ul>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script lang="ts">
import {defineComponent} from 'vue'
import axios from "axios";
import {_genUrl} from "@/app/utilities/genUrl";
import {getAppLinks} from "@/library";
import NodeFilterLink from "@/app/components/job/resources/NodeFilterLink.vue";
import { NodeFilterStore } from '../../../../library/stores/NodeFilterStore'
import {getRundeckContext} from "../../../../../build/pack";

export default defineComponent({
  name: "NodeCard",
  components: {
    NodeFilterLink
  },
  props: {
    runAuthorized: {
      type: Boolean,
      required: true,
    },
    jobCreateAuthorized: {
      type: Boolean,
      required: true,
    },
  },
  data() {
    return {
      nodeSet: {},
      nodeSummary: {},
      nodeFilterStore: new NodeFilterStore(),
      allCount: 0,
      total: 1,
      hasNodes: true,
      filterIsSet: true,
      executionMode: null,
      error: null,
      loading: false,
      truncated: null,
      colKeys: null
    }
  },
  computed: {
    isDefaultFilter() {
      return this.nodeSummary.defaultFilter === '.*'
    },
    nodesTitle() {
      return this.allCount > 1 ? this.$t('nodes') : this.$t('node')
    },
    links() {
      return getAppLinks()
    }
  },
  methods: {
    notAuthorized( authorized = this.runAuthorized) {
      return !authorized ? this.$t('not.authorized'): ''
    },
    async fetchExecutionMode() {
      try {
        let ctx = getRundeckContext();
        const response = await axios.request({
          method: 'GET',
          headers: {
            'x-rundeck-ajax': 'true',
          },
          url: `${ctx.rdBase}api/${ctx.apiVersion}/system/executions/status`
        })
        if (response.status >= 200 && response.status < 300) {
          this.executionMode = response.data.executionMode
        } else {
          throw(new Error(`Error fetching execution mode: ${response.status}`))
        }

      } catch (e) {
        this.error = e.message
      }
    },
    async fetchNodeSummary() {
      let result = await axios.get(_genUrl(getAppLinks().frameworkNodeSummaryAjax, {}))
      if (result.status >= 200 && result.status < 300) {
        this.nodeSummary = result.data
      }
    },
    async fetchNodes() {
      axios.request({
        method: 'GET',
        headers: {
          'x-rundeck-ajax': 'true',
        },
        url: _genUrl(getAppLinks().frameworkNodesQueryAjax),
      }).then(result => {
        this.loading = false
        if (result.status === 403) {
          this.error = ('Not authorized')
        } else if (result.status >= 300) {
          if (result.data.message) {

            this.error = result.data.message
          } else {
            this.error = 'Error: ' + result.status
          }
        } else {
          let data = result.data
          this.nodeSet = {
            nodes: data.allnodes,
            tagsummary: data.tagsummary
          }
          this.allCount = data.allcount
          this.total = data.total
          this.truncated = data.truncated
          this.colKeys = data.colkeys
          this.maxShown = data.max
        }
      }).catch((err) => {
        this.loading = false
        console.log('Nodes Query: request failed: ' + err)
        this.error = ('Nodes Query: request failed: ' + err)
      })
    },
    async setDefaultFilter() {
      // TODO: finish this - need to find out wth is the selectedFilterName for default
      this.nodeFilterStore.setStoredDefaultFilter(this.project,this.selectedFilterName)
      this.nodeSummary.defaultFilter = this.selectedFilterName
    },
    async removeDefaultFilter() {
      this.nodeFilterStore.removeStoredDefaultFilter(this.project)
      this.nodeSummary.defaultFilter = null
    },

    runCommand() {
      if(this.runAuthorized){
        document.location = _genUrl(getAppLinks().frameworkAdhoc, {
          filter: this.filter
        });
      }
    },
    saveJob() {
      if(this.jobCreateAuthorized){
        document.location = _genUrl(getAppLinks().scheduledExecutionCreate, {
          filter: this.filter
        });
      }
    },
  },
  async mounted() {
    await this.fetchNodeSummary();
    await this.fetchExecutionMode();
    await this.fetchNodes()
  }
})
</script>