<template>
  <div v-if="modelData">
    <div class="form-group">
      <div class="col-sm-2" style="text-align: right">
        <label class="control-label">
          <ui-socket section="resources-editor" location="section-title">
            {{ $t("resourcesEditor.Nodes") }}
          </ui-socket>
        </label>
        <ui-socket section="resources-editor" location="section-title-help" />
      </div>

      <div class="col-sm-10">
        <div class="radio radio-inline">
          <input
            id="doNodedispatchTrue"
            v-model="modelData.doNodedispatch"
            type="radio"
            name="doNodedispatch"
            :value="true"
            class="xnode_dispatch_radio"
          />
          <label for="doNodedispatchTrue">
            <ui-socket
              section="resources-editor"
              location="node-dispatch-true-label"
            >
              {{ $t("resourcesEditor.Dispatch to Nodes") }}
            </ui-socket>
          </label>
        </div>
        <div class="radio radio-inline">
          <input
            id="doNodedispatchFalse"
            v-model="modelData.doNodedispatch"
            type="radio"
            name="doNodedispatch"
            :value="false"
            class="xnode_dispatch_radio"
          />
          <label for="doNodedispatchFalse">
            <ui-socket
              section="resources-editor"
              location="node-dispatch-false-label"
            >
              {{ $t("execute.locally") }}
            </ui-socket>
          </label>
        </div>
      </div>
    </div>

    <template v-if="modelData.doNodedispatch">
      <div
        class="form-group"
        :class="{
          'has-error':
            modelData.filterErrors && modelData.filterErrors.length > 0,
        }"
      >
        <div class="subfields nodeFilterFields">
          <label class="col-sm-2 control-label">
            {{ $t("node.filter") }}
          </label>

          <div class="col-sm-10">
            <inline-validation-errors :errors="modelData.filterErrors" />

            <node-filter-input
              id="job_edit__node_filter_include"
              v-model="modelData.filter"
              :project="modelData.project"
              :filter-name="modelData.filterName"
              :node-summary="nodeSummary"
              :allow-filter-default="false"
              @filters-updated="loadNodeSummary"
              @filter="handleFilterClick"
            />

            <input type="hidden" name="project" :value="modelData.project" />
          </div>
        </div>
      </div>
      <div
        v-if="modelData.doNodedispatch"
        class="form-group"
        :class="{
          'has-error':
            modelData.filterExcludeErrors &&
            modelData.filterExcludeErrors.length > 0,
        }"
      >
        <div class="subfields nodeFilterFields">
          <label class="col-sm-2 control-label">
            {{ $t("node.filter.exclude") }}
          </label>

          <div class="col-sm-10">
            <inline-validation-errors :errors="modelData.filterExcludeErrors" />

            <node-filter-input
              id="job_edit__node_filter_exclude"
              v-model="modelData.filterExclude"
              :project="modelData.project"
              :filter-name="modelData.filterNameExclude"
              :node-summary="nodeSummary"
              :allow-filter-default="false"
              filter-field-name="filterExclude"
              filter-field-id="schedJobNodeFilterExclude"
              :help-button="false"
              search-btn-type="default"
              @filters-updated="loadNodeSummary"
              @filter="handleFilterExcludeClick"
            />
          </div>
        </div>
      </div>

      <div class="form-group">
        <div class="col-sm-2 control-label text-form-label">
          {{ $t("scheduledExecution.property.excludeFilterUncheck.label") }}
        </div>

        <div class="col-sm-10">
          <div class="radio radio-inline">
            <input
              id="excludeFilterTrue"
              v-model="modelData.excludeFilterUncheck"
              type="radio"
              name="excludeFilterUncheck"
              :value="true"
            />
            <label for="excludeFilterTrue">
              {{ $t("yes") }}
            </label>
          </div>

          <div class="radio radio-inline">
            <input
              id="excludeFilterFalse"
              v-model="modelData.excludeFilterUncheck"
              type="radio"
              :value="false"
              name="excludeFilterUncheck"
            />
            <label for="excludeFilterFalse">
              {{ $t("no") }}
            </label>
          </div>

          <span class="help-block">
            {{
              $t("scheduledExecution.property.excludeFilterUncheck.description")
            }}
          </span>
        </div>
      </div>

      <input type="hidden" name="nodeExcludePrecedence" value="true" />

      <div class="form-group">
        <label :class="labelColClass">
          {{ $t("matched.nodes.prompt") }}
        </label>

        <div class="container-fluid" :class="fieldColSize">
          <node-filter-results
            :node-filter="modelData.filter"
            :node-exclude-filter="modelData.filterExclude"
            :exclude-filter-uncheck="modelData.excludeFilterUncheck"
            @filter="handleFilterClick"
          />
        </div>
      </div>

      <div class="form-group">
        <div class="col-sm-2 control-label text-form-label">
          {{ $t("scheduledExecution.property.nodefiltereditable.label") }}
        </div>

        <div class="col-sm-10">
          <div class="radio radio-inline">
            <input
              id="editableFalse"
              v-model="modelData.nodeFilterEditable"
              type="radio"
              :value="false"
              name="nodeFilterEditable"
            />
            <label for="editableFalse">
              {{ $t("no") }}
            </label>
          </div>
          <div class="radio radio-inline">
            <input
              id="editableTrue"
              v-model="modelData.nodeFilterEditable"
              type="radio"
              name="nodeFilterEditable"
              :value="true"
            />
            <label for="editableTrue">
              {{ $t("yes") }}
            </label>
          </div>
        </div>
      </div>

      <div
        class="form-group"
        :class="{
          'has-errors':
            modelData.filterExcludeErrors &&
            modelData.filterExcludeErrors.length > 0,
        }"
      >
        <label for="schedJobnodeThreadcount" :class="labelColClass">
          {{ $t("scheduledExecution.property.nodeThreadcount.label") }}
        </label>

        <div :class="fieldColSize">
          <div class="row">
            <div class="col-sm-4">
              <input
                id="schedJobnodeThreadcount"
                v-model="modelData.nodeThreadcountDynamic"
                type="text"
                name="nodeThreadcountDynamic"
                size="3"
                class="form-control input-sm"
              />
            </div>
          </div>
          <inline-validation-errors
            :errors="modelData.nodeThreadcountDynamicErrors"
          />
          <span class="help-block">
            {{ $t("scheduledExecution.property.nodeThreadcount.description") }}
          </span>
        </div>
      </div>

      <div
        class="form-group"
        :class="{
          'has-errors':
            modelData.nodeRankAttributeErrors &&
            modelData.nodeRankAttributeErrors.length > 0,
        }"
      >
        <label for="schedJobnodeRankAttribute" :class="labelColClass">
          {{ $t("scheduledExecution.property.nodeRankAttribute.label") }}
        </label>

        <div :class="fieldColSize">
          <div class="row">
            <div class="col-sm-4">
              <input
                id="schedJobnodeRankAttribute"
                v-model="modelData.nodeRankAttribute"
                type="text"
                name="nodeRankAttribute"
                class="form-control input-sm"
              />
            </div>
          </div>
          <inline-validation-errors
            :errors="modelData.nodeRankAttributeErrors"
          />
          <span class="help-block">
            {{
              $t("scheduledExecution.property.nodeRankAttribute.description")
            }}
          </span>
        </div>
      </div>

      <div class="form-group">
        <label :class="labelColClass">
          {{ $t("scheduledExecution.property.nodeRankOrder.label") }}
        </label>

        <div :class="fieldColSize">
          <div class="radio radio-inline">
            <input
              id="nodeRankOrderAscending"
              v-model="modelData.nodeRankOrderAscending"
              type="radio"
              name="nodeRankOrderAscending"
              :value="true"
            />
            <label for="nodeRankOrderAscending">
              {{
                $t("scheduledExecution.property.nodeRankOrder.ascending.label")
              }}
            </label>
          </div>
          <div class="radio radio-inline">
            <input
              id="nodeRankOrderDescending"
              v-model="modelData.nodeRankOrderAscending"
              type="radio"
              name="nodeRankOrderAscending"
              :value="false"
            />
            <label for="nodeRankOrderDescending">
              {{
                $t("scheduledExecution.property.nodeRankOrder.descending.label")
              }}
            </label>
          </div>
        </div>
      </div>

      <div class="form-group">
        <label :class="labelColClass">{{
          $t("scheduledExecution.property.nodeKeepgoing.prompt")
        }}</label>

        <div :class="fieldColSize">
          <div class="radio">
            <input
              id="nodeKeepgoingFalse"
              v-model="modelData.nodeKeepgoing"
              type="radio"
              name="nodeKeepgoing"
              :value="false"
            />
            <label for="nodeKeepgoingFalse">
              {{
                $t(
                  "scheduledExecution.property.nodeKeepgoing.false.description",
                )
              }}
            </label>
          </div>

          <div class="radio">
            <input
              id="nodeKeepgoingTrue"
              v-model="modelData.nodeKeepgoing"
              type="radio"
              name="nodeKeepgoing"
              :value="true"
            />
            <label for="nodeKeepgoingTrue">
              {{
                $t("scheduledExecution.property.nodeKeepgoing.true.description")
              }}
            </label>
          </div>
        </div>
      </div>
      <div class="form-group">
        <label :class="labelColClass">{{
          $t("scheduledExecution.property.successOnEmptyNodeFilter.prompt")
        }}</label>

        <div :class="fieldColSize">
          <div class="radio">
            <input
              id="successOnEmptyNodeFilterFalse"
              v-model="modelData.successOnEmptyNodeFilter"
              type="radio"
              name="successOnEmptyNodeFilter"
              :value="false"
            />
            <label for="successOnEmptyNodeFilterFalse">
              {{
                $t(
                  "scheduledExecution.property.successOnEmptyNodeFilter.false.description",
                )
              }}
            </label>
          </div>
          <div class="radio">
            <input
              id="successOnEmptyNodeFilterTrue"
              v-model="modelData.successOnEmptyNodeFilter"
              type="radio"
              name="successOnEmptyNodeFilter"
              :value="true"
            />
            <label for="successOnEmptyNodeFilterTrue">
              {{
                $t(
                  "scheduledExecution.property.successOnEmptyNodeFilter.true.description",
                )
              }}
            </label>
          </div>
        </div>
      </div>
      <div class="form-group">
        <label :class="labelColClass">{{
          $t("scheduledExecution.property.nodesSelectedByDefault.label")
        }}</label>

        <div :class="fieldColSize">
          <div class="radio">
            <input
              id="nodesSelectedByDefaultTrue"
              v-model="modelData.nodesSelectedByDefault"
              type="radio"
              name="nodesSelectedByDefault"
              :value="true"
            />
            <label for="nodesSelectedByDefaultTrue">
              {{
                $t(
                  "scheduledExecution.property.nodesSelectedByDefault.true.description",
                )
              }}
            </label>
          </div>
          <div class="radio">
            <input
              id="nodesSelectedByDefaultFalse"
              v-model="modelData.nodesSelectedByDefault"
              type="radio"
              name="nodesSelectedByDefault"
              :value="false"
            />
            <label for="nodesSelectedByDefaultFalse">
              {{
                $t(
                  "scheduledExecution.property.nodesSelectedByDefault.false.description",
                )
              }}
            </label>
          </div>
        </div>
      </div>
      <orchestrator-editor
        v-model="modelData.orchestrator"
        :label-col-class="labelColClass"
        :field-col-size="fieldColSize"
      />
    </template>
  </div>
</template>
<script lang="ts">
import OrchestratorEditor from "../../../components/job/resources/OrchestratorEditor.vue";
import { _genUrl } from "../../../utilities/genUrl";
import axios from "axios";
import InlineValidationErrors from "../../form/InlineValidationErrors.vue";
import { defineComponent, ref } from "vue";
import type { PropType } from "vue";
import NodeFilterInput from "./NodeFilterInput.vue";
import NodeFilterResults from "./NodeFilterResults.vue";

import { getAppLinks } from "../../../../library";
import UiSocket from "../../../../library/components/utils/UiSocket.vue";

export default defineComponent({
  name: "ResourcesEditor",
  components: {
    OrchestratorEditor,
    InlineValidationErrors,
    NodeFilterInput,
    NodeFilterResults,
    UiSocket,
  },
  props: {
    modelValue: {
      type: Object as PropType<any>,
      required: true,
    },
    eventBus: {
      type: Object as PropType<any>,
      required: true,
    },
  },
  emits: ["update:modelValue"],
  setup() {
    const modelData = ref<any>({
      doNodedispatch: false,
      orchestrator: { type: null, config: {} },
    });
    const nodeSummary = ref<any>({});
    return {
      modelData,
      nodeSummary,
    };
  },
  computed: {
    labelColClass() {
      return "col-sm-2 control-label";
    },
    fieldColSize() {
      return "col-sm-10";
    },
  },
  watch: {
    modelData: {
      handler() {
        this.$emit("update:modelValue", this.modelData);
      },
      deep: true,
    },
  },
  mounted() {
    this.onMount();
  },
  methods: {
    async loadNodeSummary() {
      const result = await axios.get(
        _genUrl(getAppLinks().frameworkNodeSummaryAjax, {}),
      );
      if (result.status >= 200 && result.status < 300) {
        this.nodeSummary = result.data;
      }
    },
    handleFilterExcludeClick(val: any) {
      this.handleFilterClick({
        filterExclude: val.filter,
        filterNameExclude: val.filterName,
      });
    },
    handleFilterClick(val: any) {
      if (val.filter) {
        this.modelData.filter = val.filter;
      }
      if (val.filterName) {
        this.modelData.filterName = val.filterName;
      }
      if (val.filterExclude) {
        this.modelData.filterExclude = val.filterExclude;
      }
      if (val.filterNameExclude) {
        this.modelData.filterNameExclude = val.filterNameExclude;
      }
    },
    async onMount() {
      this.modelData = {
        ...this.modelValue,
        filter: this.modelValue.filter || "",
        filterExclude: this.modelValue.filterExclude || "",
      };
      await this.loadNodeSummary();
    },
  },
});
</script>
