<template>
  <div id="adhoc-app">
                        <node-filter-section
                          v-if="executionModeActive && nodeFilterStore"
                          :node-filter-store="nodeFilterStore"
                          :max-shown="filterParams.matchedNodesMaxCount"
                          :project="projectName"
                          @node-total-changed="handleNodeTotalChanged"
                          @node-error-changed="handleNodeErrorChanged"
                        />

                        <adhoc-command-form
                          v-if="executionModeActive && adhocCommandStore && nodeFilterStore"
                          :adhoc-command-store="adhocCommandStore"
                          :node-filter-store="nodeFilterStore"
                          :project="projectName"
                          :event-bus="EventBus"
                          :page-params="pageParams"
                          :node-total="nodeTotal"
                          :node-error="nodeError"
                          @node-total-changed="handleNodeTotalChanged"
                        />

            <execution-output
              v-if="adhocCommandStore"
              :event-bus="EventBus"
              :page-params="pageParams"
              :adhoc-command-store="adhocCommandStore"
            />

    <activity-section
      v-if="eventReadAuth"
      :event-bus="EventBus"
    />
  </div>
</template>

<script lang="ts">
import { defineComponent } from "vue";
import { NodeFilterStore } from "../../../library/stores/NodeFilterLocalstore";
import { AdhocCommandStore } from "../../../library/stores/AdhocCommandStore";
import { loadJsonData } from "../../utilities/loadJsonData";
import { getRundeckContext } from "../../../library";
import NodeFilterSection from "../../components/adhoc/NodeFilterSection.vue";
import AdhocCommandForm from "../../components/adhoc/AdhocCommandForm.vue";
import ExecutionOutput from "../../components/adhoc/ExecutionOutput.vue";
import ActivitySection from "../../components/adhoc/ActivitySection.vue";

export default defineComponent({
  name: "AdhocApp",
  components: {
    NodeFilterSection,
    AdhocCommandForm,
    ExecutionOutput,
    ActivitySection,
  },
  props: {
    eventBus: {
      type: Object,
      required: true,
    },
  },
  data() {
    return {
      EventBus: this.eventBus,
      projectName: "",
      executionModeActive: true, // Set from window._rundeck.data.executionModeActive in initializeData()
      eventReadAuth: false,
      emptyQuery: false,
      filterParams: {
        matchedNodesMaxCount: 50,
        filter: "",
        filterAll: false,
      },
      pageParams: {
        disableMarkdown: "",
        smallIconUrl: "",
        iconUrl: "",
        lastlines: 20,
        maxLastLines: 20,
        emptyQuery: null,
        ukey: "",
        project: "",
        runCommand: "",
        adhocKillAllowed: false,
      },
      nodeFilterStore: null as NodeFilterStore | null,
      adhocCommandStore: null as AdhocCommandStore | null,
      nodeTotal: 0,
      nodeError: null as string | null,
    };
  },
  mounted() {
    this.initializeData();
    this.initializeStores();
    // Event listeners are set up via component props and event handlers
    // Node filter events are handled by handleNodeTotalChanged() and handleNodeErrorChanged()
  },
  beforeUnmount() {
    // Cleanup if needed
    if (this.adhocCommandStore) {
      this.adhocCommandStore.stopFollowing();
    }
  },
  methods: {
    initializeData() {
      const rundeckContext = getRundeckContext();
      this.projectName = rundeckContext.projectName || "";

      // Load embedded JSON data
      const filterParamsData = loadJsonData("filterParamsJSON");
      if (filterParamsData) {
        this.filterParams = {
          matchedNodesMaxCount:
            filterParamsData.matchedNodesMaxCount || 50,
          filter: filterParamsData.filter || "",
          filterAll: filterParamsData.filterAll || false,
        };
        this.emptyQuery = !filterParamsData.filter;
      }

      const pageParamsData = loadJsonData("pageParams");
      if (pageParamsData) {
        this.pageParams = {
          disableMarkdown: pageParamsData.disableMarkdown || "",
          smallIconUrl: pageParamsData.smallIconUrl || "",
          iconUrl: pageParamsData.iconUrl || "",
          lastlines: pageParamsData.lastlines || 20,
          maxLastLines: pageParamsData.maxLastLines || 20,
          emptyQuery: pageParamsData.emptyQuery,
          ukey: pageParamsData.ukey || "",
          project: pageParamsData.project || this.projectName,
          runCommand: pageParamsData.runCommand || "",
          adhocKillAllowed: pageParamsData.adhocKillAllowed || false,
        };
      }

      // Get auth and execution mode from window._rundeck.data
      if (window._rundeck && window._rundeck.data) {
        this.eventReadAuth =
          window._rundeck.data.eventReadAuth !== undefined
            ? window._rundeck.data.eventReadAuth
            : false;

        // Execution mode is set from GSP via window._rundeck.data.executionModeActive
        // The GSP uses g.executionMode() to determine if execution is active
        if (window._rundeck.data.executionModeActive !== undefined) {
          this.executionModeActive = window._rundeck.data.executionModeActive;
        } else {
          // Default to active if not specified
          this.executionModeActive = true;
        }
      }
    },

    initializeStores() {
      // Initialize NodeFilterStore
      this.nodeFilterStore = new NodeFilterStore({
        filter: this.filterParams.filter,
      });

      // Initialize AdhocCommandStore
      this.adhocCommandStore = new AdhocCommandStore({
        commandString: this.pageParams.runCommand || "",
      });

      // Node filter total changes are handled via events from NodeFilterSection
      // which calls handleNodeTotalChanged() to update canRun state
    },

    handleNodeTotalChanged(total: number) {
      this.nodeTotal = total;
      // Update adhocCommandStore canRun state
      if (this.adhocCommandStore) {
        this.adhocCommandStore.updateCanRunFromNodeTotal(
          total,
          this.adhocCommandStore.running
        );
      }
    },
    handleNodeErrorChanged(error: string | null) {
      this.nodeError = error;
      // Update adhocCommandStore canRun state when error changes
      if (this.adhocCommandStore && this.nodeFilterStore) {
        this.adhocCommandStore.updateCanRunFromNodeTotal(
          this.nodeTotal,
          this.adhocCommandStore.running
        );
      }
    },
  },
});
</script>

<style scoped>
/* Styles will be inherited from existing CSS */
</style>

