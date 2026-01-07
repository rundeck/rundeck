<template>
  <div class="card">
    <div class="card-content">
      <div class="row">
        <div class="col-xs-12">
          <div
            v-if="showEmptyState"
            class="spacing text-warning"
            id="emptyerror"
          >
            <span class="errormessage">
              {{ $t("no.nodes.selected.match.nodes.by.selecting.or.entering.a.filter") }}
            </span>
          </div>

          <div
            v-if="error"
            class="spacing text-danger"
            id="loaderror2"
          >
            <i class="glyphicon glyphicon-warning-sign"></i>
            <span class="errormessage">{{ error }}</span>
          </div>

          <node-filter-results
            v-if="hasFilter"
            :node-filter="nodeFilterStore.selectedFilter"
            :max-shown="maxShown"
            empty-mode="blank"
            view="embed"
            :paging="false"
            @filter="handleFilter"
            @update:total="handleTotalUpdate"
            @update:loading="handleLoadingUpdate"
            @update:error="handleErrorUpdate"
            ref="nodeFilterResultsRef"
          />
        </div>
      </div>
    </div>
  </div>
</template>

<script lang="ts">
import { defineComponent } from "vue";
import NodeFilterResults from "../job/resources/NodeFilterResults.vue";
import { NodeFilterStore } from "../../../library/stores/NodeFilterLocalstore";
import { _genUrl } from "../../../library/utilities/genUrl";
import { url } from "../../../library";
import type { PropType } from "vue";

export default defineComponent({
  name: "NodeFilterSection",
  components: {
    NodeFilterResults,
  },
  props: {
    nodeFilterStore: {
      type: Object as PropType<NodeFilterStore>,
      required: true,
    },
    maxShown: {
      type: Number,
      default: 50,
    },
    project: {
      type: String,
      required: true,
    },
  },
  emits: ["node-total-changed", "node-error-changed"],
  data() {
    return {
      loading: false,
      error: null as string | null,
      total: 0,
    };
  },
  computed: {
    hasFilter(): boolean {
      return !!(
        this.nodeFilterStore && this.nodeFilterStore.selectedFilter
      );
    },
    nodesTitle(): string {
      return this.total === 1
        ? this.$t("Node")
        : this.$t("Node.plural");
    },
    showEmptyState(): boolean {
      // Show warning when no nodes matched (either no filter entered, or filter returned 0 results)
      return (
        !this.loading &&
        !this.error &&
        (this.total === 0 || !this.total)
      );
    },
  },
  watch: {
    "nodeFilterStore.selectedFilter"() {
      // Reset state when filter changes
      this.total = 0;
      this.loading = false;
      this.error = null;
    },
  },
  methods: {
    handleTotalUpdate(total: number) {
      // Always update and emit, even if total is 0, to ensure App.vue receives the update
      // This is critical for disabling the Run button when no nodes are matched
      this.total = total;
      this.$emit("node-total-changed", total);
    },
    handleLoadingUpdate(loading: boolean) {
      this.loading = loading;
    },
    handleErrorUpdate(error: string | null) {
      const errorValue = error || null;
      if (errorValue !== this.error) {
        this.error = errorValue;
        this.$emit("node-error-changed", errorValue);
      }
    },
    viewInNodesPage() {
      const filter = this.nodeFilterStore.selectedFilter;
      if (filter) {
        const nodesUrl = _genUrl(`/project/${this.project}/nodes`, {
          filter,
        });
        document.location.href = url(nodesUrl).href;
      }
    },
    handleFilter(filterData: any) {
      // Handle filter changes from NodeFilterResults
      if (filterData && filterData.filter) {
        this.nodeFilterStore.setSelectedFilter(filterData.filter);
      }
    },
  },
});
</script>

<style scoped>
/* Styles inherited from existing CSS */
</style>

