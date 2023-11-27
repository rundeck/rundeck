<template>
  <div class="card">
    <div class="card-content vue-tabs">
      <!-- NAV TABS -->
      <div class="nav-tabs-navigation">
        <div class="nav-tabs-wrapper">
          <ul class="nav nav-tabs">
            <li :class="{'active': !nodeFilterStore.filter}" id="tab_link_summary" @click="fetchNodeSummary" :key="`${nodeFilterStore.filter}tab-result`">
              <a href="#summary1" data-toggle="tab">
                {{ $t("browse") }}
              </a>
            </li>
            <li :class="{'active': nodeFilterStore.filter}" id="tab_link_result" :key="`${nodeFilterStore.filter}tab-summary`" v-if="allCount !== null">
              <a href="#result1" data-toggle="tab" v-if="filterIsSet">
                {{ $t("result") }}
                <template v-if="allCount >= 0">
                  <span class="text-info" style="margin-right: 4px">
                    {{ total }}
                  </span>
                  <span>
                    {{ nodesTitle }}
                  </span>
                </template>
                <span v-else class="text-strong">&hellip;</span>
              </a>
              <a v-else data-toggle="tab">
                {{ $t("enter.a.filter") }}
              </a>
            </li>
          </ul>
        </div>
        <span v-if="allCount !== null" class="pull-right">
          <span class="tabs-sibling tabs-sibling-compact">
            <div class="btn-group pull-right">
              <button
                  class="btn btn-default btn-sm dropdown-toggle"
                  data-toggle="dropdown"
              >
                {{ $t("actions") }} <span class="caret"></span>
              </button>
              <ul class="dropdown-menu" role="menu">
                <li
                    v-if="executionMode === 'active' && hasNodes"
                    :class="{ disabled: !runAuthorized }"
                >
                  <a
                      href="#"
                      @click="runCommand"
                      :title="notAuthorized()"
                      :class="{ has_tooltip: !runAuthorized }"
                      data-placement="left"
                  >
                    <i class="glyphicon glyphicon-play"></i>
                    <span>
                      {{
                        $t("run.a.command.on.count.nodes.ellipsis", [
                          total,
                          nodesTitle,
                        ])
                      }}
                    </span>
                  </a>
                </li>
                <li v-else-if="hasNodes" class="disabled">
                  <a
                      href="#"
                      :title="$t('disabled.execution.run')"
                      class="has_tooltip"
                      data-placement="left"
                  >
                    <i class="glyphicon glyphicon-play"></i>
                    <span>
                      {{
                        $t("run.a.command.on.count.nodes.ellipsis", [
                          total,
                          nodesTitle,
                        ])
                      }}
                    </span>
                  </a>
                </li>
                <li :class="{ disabled: !jobCreateAuthorized }">
                  <a
                      href="#"
                      @click="saveJob"
                      :title="notAuthorized(jobCreateAuthorized)"
                      :class="{ has_tooltip: !jobCreateAuthorized }"
                      data-placement="left"
                  >
                    <i class="glyphicon glyphicon-plus"></i>
                    <span>
                      {{
                        $t("create.a.job.for.count.nodes.ellipsis", [
                          total,
                          nodesTitle,
                        ])
                      }}
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
        <div class="tab-pane" :class="{'active': !nodeFilterStore.filter}" id="summary1" :key="`${nodeFilterStore.filter}summary`">
          <div class="row">
            <div class="col-xs-6">
              <h5 class="column-title text-uppercase text-strong">
                {{ $t("resource.metadata.entity.tags") }}
              </h5>
              <ul
                  class="list-unstyled"
              >
                <li v-for="(tag, index) in nodeSummary.tags" style="margin: 0 2px; display: inline-block;" :key="index">
                  <node-filter-link
                      class="label label-muted"
                      filterKey="tags"
                      :filterVal="tag.tag"
                      @nodefilterclick="saveFilter"
                  >
                    <template #suffix>
                      ({{ tag.count }})
                    </template>
                  </node-filter-link>
                </li>
              </ul>
              <div
                  v-if="nodeSummary && !nodeSummary.hasOwnProperty('tags') || nodeSummary.tags && nodeSummary.tags!.length === 0"
              >
                {{ $t("none") }}
              </div>
            </div>

            <div class="col-xs-6">
              <h5 class="column-title text-uppercase text-strong">
                {{ $t("filters") }}
              </h5>

              <ul class="list-unstyled">
                <li>
                  <a
                      class="nodefilterlink btn btn-default btn-xs"
                      @click.prevent="saveFilter({filter: '.*'})"
                  >
                    {{ $t("all.nodes") }} {{ nodeSummary? nodeSummary.totalCount! : 0 }}
                  </a>
                  <div class="btn-group" style="margin-left: 4px">
                    <button
                        type="button"
                        class="btn btn-default btn-xs btn-simple dropdown-toggle"
                        style="padding: 0 5px"
                        title="Filter Actions"
                        data-toggle="dropdown"
                        aria-expanded="false"
                    >
                      <span class="caret"></span>
                    </button>
                    <ul class="dropdown-menu" role="menu">
                      <li>
                        <a
                            href="#"
                            @click="
                            isDefaultFilter
                              ? removeDefaultFilter
                              : setDefaultFilter
                          "
                        >
                          <i
                              class="glyphicon"
                              :class="[
                              isDefaultFilter
                                ? 'glyphicon-ban-circle'
                                : 'glyphicon-filter',
                            ]"
                          ></i>
                          {{
                            isDefaultFilter
                                ? $t("remove.all.nodes.as.default.filter")
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

        <div class="tab-pane" :class="{'active': nodeFilterStore.filter}" id="result1" :key="`${nodeFilterStore.filter}result`">
          <div v-if="error" class="row row-space">
            <div class="col-sm-12">
              <span class="text-danger">
                <i class="glyphicon glyphicon-warning-sign"></i>
                <span>{{ error }}</span>
              </span>
            </div>
          </div>
          <div class="clear matchednodes" id="nodeview">
            <NodeTable
                :node-set="nodeSet"
                :filter-columns="filterColumns"
                :loading="loading"
                :has-paging="hasPaging"
                :paging-max="pagingMax"
                :maxPages="maxPages"
                :page="page"
                :filterAll="filterAll"
                @filter="saveFilter"
                @changePage="updatePage"
                @changePagingMax="updatePagingMax"
                :filter="nodeFilterStore.filter"
            />
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script lang="ts">
import {defineComponent, PropType} from "vue";
import axios from "axios";
import { _genUrl } from "@/app/utilities/genUrl";
import { getAppLinks } from "@/library";
import NodeFilterLink from "@/app/components/job/resources/NodeFilterLink.vue";
import { getRundeckContext } from "../../../../../build/pack";
import NodeTable from "@/app/components/job/resources/NodeTable.vue";
import {getExecutionMode, getNodes, getNodeSummary} from "@/app/components/job/resources/services/nodeServices";

export default defineComponent({
  name: "NodeCard",
  components: {
    NodeTable,
    NodeFilterLink,
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
    nodeFilterStore: {
      type: Object as PropType<any>,
      required: true,
    },
  },
  data() {
    return {
      nodeSet: {},
      nodeSummary: {},
      allCount: null as number | null,
      total: 0,
      executionMode: null,
      error: null,
      loading: false,
      truncated: null,
      colKeys: null,
      filterAll: false,
      hideAll: false,
      filter: null,
      page: 0,
      pagingMax: 20,
    };
  },
  computed: {
    isDefaultFilter(): boolean {
      return this.nodeSummary.selectedFilter === ".*";
    },
    hasNodes(): boolean {
      return !!this.allCount;
    },
    nodesTitle(): string {
      return this.total >= 1 ? this.$t("nodes") : this.$t("node");
    },
    filterColumns(): string[] {
      if(!this.colKeys) {
        return [];
      }
      return this.colKeys.filter((colKey: string) => !colKey.startsWith("ui:"));
    },
    filterIsSet(): boolean {
      return !(this.filterAll && this.isDefaultFilter);
    },
    hasPaging(): boolean {
      return this.total > this.pagingMax;
    },
    maxPages(): number {
      return Math.ceil(this.total/this.pagingMax);
    }
  },
  emits: ['filter'],
  methods: {
    notAuthorized(authorized = this.runAuthorized): string {
      return !authorized ? this.$t("not.authorized") : "";
    },
    async fetchExecutionMode() {
      try {
        const data = await getExecutionMode();
        this.executionMode = data.executionMode;
      } catch (e) {
        this.error = e.message;
      }
    },
    async fetchNodeSummary() {
      try {
        this.nodeSummary = await getNodeSummary();
      } catch(e) {
        this.error = "Node Summary: request failed: " + e.message;
      }
    },
    async fetchNodes() {
      // filter parameters
      const filterdata =  this.nodeFilterStore.filter ? {filter: this.nodeFilterStore.filter} : {};

      const page = this.page;
      const basedata: any = {
        view: 'table',
        declarenone: true,
        fullresults: true,
        expanddetail: true,
        page: this.page,
        max: this.pagingMax,
        inlinepaging: true,
      }

      if (this.hasPaging && page !== 0) {
        basedata.view = 'tableContent';
      }

      let params = Object.assign({}, basedata, filterdata);
      if (!this.nodeFilterStore.filter) {
        params.localNodeOnly = 'true';
      }
      params.nodeExcludePrecedence = 'true';


      try {
        this.loading = true;
        const data = await getNodes(params, getAppLinks().frameworkNodesQueryAjax);

        this.nodeSet = {
          nodes: data.allnodes,
          tagsummary: data.tagsummary,
        };
        this.allCount = data.allcount;
        this.total = data.total;
        this.truncated = data.truncated;
        this.colKeys = data.colkeys;
        this.maxShown = data.max;

      } catch(e) {
        this.error = "Nodes Query: request failed: " + e.message;
      } finally {
        this.loading = false;
      }
    },
    async setDefaultFilter() {
      // TODO: finish this - need to find out wth is the selectedFilterName for default
      // this.nodeFilterStore.setStoredDefaultFilter(
      //   this.project,
      //   this.selectedFilterName
      // );
      // this.nodeSummary.defaultFilter = this.selectedFilterName;
    },
    async removeDefaultFilter() {
      // this.nodeFilterStore.removeStoredDefaultFilter(this.project);
      // this.nodeSummary.defaultFilter = null;
    },

    runCommand() {
      if (this.runAuthorized) {
        document.location = _genUrl(getAppLinks().frameworkAdhoc, {
          filter: this.nodeFilterStore.selectedFilter,
        });
      }
    },
    saveJob() {
      if (this.jobCreateAuthorized) {
        document.location = _genUrl(getAppLinks().scheduledExecutionCreate, {
          filter: this.nodeFilterStore.selectedFilter,
        });
      }
    },

    saveFilter(selectedFilter: object) {
      this.$emit('filter',selectedFilter);
    },
    async updatePage(page: number) {
      this.page = page;
      await this.fetchNodes();
    },
    async updatePagingMax(pagingMax: number) {
      this.pagingMax = pagingMax;
      await this.fetchNodes();
    }
  },
  async mounted() {
    await this.fetchNodeSummary();
    await this.fetchExecutionMode();
    await this.fetchNodes();
  },
  watch: {
    nodeFilterStore: {
      async handler(newValue) {

        if (newValue.selectedFilter === '' && this.hideAll) {
          this.filterAll = true;
        }else if(newValue.selectedFilter ===".*"){
          this.filterAll = true;
        }

        await this.fetchNodes();

      },
      deep: true,
    },
  }
});
</script>