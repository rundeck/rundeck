<template>
  <div class="card">
    <div class="card-content vue-tabs">
      <!-- NAV TABS -->
      <div class="nav-tabs-navigation">
        <div class="nav-tabs-wrapper">
          <ul class="nav nav-tabs">
            <li
              id="tab_link_summary"
              :key="`${nodeFilterStore.filter}tab-result`"
              :class="{ active: !nodeFilterStore.filter }"
              data-testid="summary-tab"
              @click="fetchNodeSummary"
            >
              <a href="#summary1" data-toggle="tab">
                {{ $t("browse") }}
              </a>
            </li>
            <li
              v-if="allCount !== null"
              id="tab_link_result"
              :key="`${nodeFilterStore.filter}tab-summary`"
              :class="{ active: nodeFilterStore.filter }"
              data-testid="node-table-tab"
            >
              <a v-if="filterIsSet" href="#result1" data-toggle="tab">
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
                    :title="notAuthorized()"
                    :class="{ has_tooltip: !runAuthorized }"
                    data-placement="left"
                    @click="runCommand"
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
                    :title="notAuthorized(jobCreateAuthorized)"
                    :class="{ has_tooltip: !jobCreateAuthorized }"
                    data-placement="left"
                    @click="saveJob"
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
        <div v-if="error" class="row row-space">
          <div class="col-sm-12">
            <span class="text-danger">
              <i class="glyphicon glyphicon-warning-sign"></i>
              <span>{{ error }}</span>
            </span>
          </div>
        </div>

        <div
          id="summary1"
          :key="`${nodeFilterStore.filter}summary`"
          class="tab-pane"
          :class="{ active: !nodeFilterStore.filter }"
        >
          <div class="row">
            <div class="col-xs-6">
              <h5 class="column-title text-uppercase text-strong">
                {{ $t("resource.metadata.entity.tags") }}
              </h5>
              <ul class="list-unstyled">
                <li
                  v-for="(tag, index) in nodeSummary.tags"
                  :key="index"
                  style="margin: 0 2px; display: inline-block"
                >
                  <node-filter-link
                    class="label label-muted"
                    filter-key="tags"
                    :filter-val="tag.tag"
                    @nodefilterclick="saveFilter"
                  >
                    <template #suffix> ({{ tag.count }}) </template>
                  </node-filter-link>
                </li>
              </ul>
              <div
                v-if="
                  (nodeSummary && !nodeSummary.hasOwnProperty('tags')) ||
                  (nodeSummary.tags && nodeSummary.tags!.length === 0)
                "
              >
                {{ $t("none") }}
              </div>
            </div>

            <div class="col-xs-6">
              <h5 class="column-title text-uppercase text-strong">
                {{ $t("filters") }}
              </h5>
              <node-default-filter-dropdown
                :node-summary="nodeSummary"
                :is-default-filter="isDefaultFilter"
                @filter="saveFilter"
              />
            </div>
          </div>
        </div>

        <div
          id="result1"
          :key="`${nodeFilterStore.filter}result`"
          class="tab-pane"
          :class="{ active: nodeFilterStore.filter }"
        >
          <div id="nodeview" class="clear matchednodes">
            <NodeTable
              :node-set="nodeSet"
              :filter-columns="filterColumns"
              :loading="loading"
              :has-paging="hasPaging"
              :paging-max="pagingMax"
              :max-pages="maxPages"
              :page="page"
              :filter-all="filterAll"
              :filter="nodeFilterStore.filter"
              @filter="saveFilter"
              @change-page="updatePage"
              @change-paging-max="updatePagingMax"
            />
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script lang="ts">
import { defineComponent, PropType } from "vue";
import { _genUrl } from "@/app/utilities/genUrl";
import { getAppLinks, getRundeckContext } from "@/library";
import NodeFilterLink from "@/app/components/job/resources/NodeFilterLink.vue";
import NodeTable from "@/app/components/job/resources/NodeTable.vue";
import {
  getConfigMetaForExecutionMode,
  getNodes,
  getNodeSummary,
} from "@/app/components/job/resources/services/nodeServices";
import { NodeSummary } from "@/app/components/job/resources/types/nodeTypes";
import NodeDefaultFilterDropdown from "@/app/components/job/resources/NodeDefaultFilterDropdown.vue";

export default defineComponent({
  name: "NodeCard",
  components: {
    NodeDefaultFilterDropdown,
    NodeTable,
    NodeFilterLink,
  },
  props: {
    project: {
      type: String,
      required: true,
    },
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
  emits: ["filter"],
  data() {
    return {
      nodeSet: {},
      nodeSummary: {} as NodeSummary,
      eventBus: getRundeckContext().eventBus,
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
      if (!this.colKeys) {
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
      return Math.ceil(this.total / this.pagingMax);
    },
  },
  watch: {
    nodeFilterStore: {
      async handler(newValue) {
        if (newValue.selectedFilter === "" && this.hideAll) {
          this.filterAll = true;
        } else if (newValue.selectedFilter === ".*") {
          this.filterAll = true;
        }

        await this.fetchNodes();
      },
      deep: true,
    },
  },
  async mounted() {
    await this.fetchNodeSummary();
    await this.fetchExecutionMode();
    await this.fetchNodes();

    if (this.nodeSummary.defaultFilter) {
      let filterToEmit = this.nodeSummary.defaultFilter;
      if (filterToEmit !== ".*") {
        filterToEmit = this.nodeSummary.filters.filter(
          (f) => f.filterName === this.nodeSummary.defaultFilter
        )[0];
      }
      this.saveFilter(filterToEmit);
    }
    this.eventBus.on("nodefilter:savedFilters:changed", this.fetchNodeSummary);
  },
  methods: {
    notAuthorized(authorized = this.runAuthorized): string {
      return !authorized ? this.$t("not.authorized") : "";
    },
    async fetchExecutionMode() {
      try {
        const data = await getConfigMetaForExecutionMode(this.project);

        // not taking scheduleEnabled into account as commands executed from here are adhoc
        this.executionMode = data[0].data!.executionsEnabled
          ? "active"
          : "passive";
      } catch (e) {
        this.error = e.message;
      }
    },
    async fetchNodeSummary() {
      try {
        this.nodeSummary = await getNodeSummary();
        this.nodeSummary = {
          ...this.nodeSummary,
          ...this.nodeFilterStore.loadStoredProjectNodeFilters(this.project),
        };
      } catch (e) {
        this.error = "Node Summary: request failed: " + e.message;
      }
    },
    async fetchNodes() {
      // filter parameters
      const filterdata = this.nodeFilterStore.filter
        ? { filter: this.nodeFilterStore.filter }
        : {};

      const page = this.page;
      const basedata: any = {
        view: "table",
        declarenone: true,
        fullresults: true,
        expanddetail: true,
        page: this.page,
        max: this.pagingMax,
        inlinepaging: true,
      };

      if (this.hasPaging && page !== 0) {
        basedata.view = "tableContent";
      }

      const params = Object.assign({}, basedata, filterdata);
      if (!this.nodeFilterStore.filter) {
        params.localNodeOnly = "true";
      }
      params.nodeExcludePrecedence = "true";

      try {
        this.loading = true;
        const data = await getNodes(
          params,
          getAppLinks().frameworkNodesQueryAjax
        );

        this.nodeSet = {
          nodes: data.allnodes,
          tagsummary: data.tagsummary,
        };
        this.allCount = data.allcount;
        this.total = data.total;
        this.truncated = data.truncated;
        this.colKeys = data.colkeys;
        this.maxShown = data.max;
      } catch (e) {
        this.error = e.message;
      } finally {
        this.loading = false;
      }
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
      this.$emit("filter", selectedFilter);
    },
    async updatePage(page: number) {
      this.page = page;
      await this.fetchNodes();
    },
    async updatePagingMax(pagingMax: number) {
      this.pagingMax = pagingMax;
      await this.fetchNodes();
    },
  },
});
</script>
