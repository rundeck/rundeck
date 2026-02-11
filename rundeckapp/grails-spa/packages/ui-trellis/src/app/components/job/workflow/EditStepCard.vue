<template>
  <BaseStepCard
    :config="stepConfig"
    :plugin-details="provider || {}"
    :expanded="contentExpanded"
    card-class="edit-step-card"
  >
    <template #header>
      <StepCardHeader
        v-if="provider"
        :plugin-details="provider"
        :config="stepConfig"
        :hide-step-type="true"
        :show-toggle="depth > 0"
        :expanded="contentExpanded"
        @delete="handleCancel"
        @toggle="contentExpanded = !contentExpanded"
      />
      <div v-else-if="loading" class="loading-container">
        <i class="fas fa-spinner fa-spin"></i>
        <span>{{ $t("loading.text") }}</span>
      </div>
    </template>
    <template #content>
      <div v-if="isJobRef" class="jobref-form-content">
        <div v-if="validationError" class="alert alert-danger">
          {{ validationError }}
        </div>

        <div class="step-name-section">
          <div class="form-group">
            <label
              class="col-sm-2 control-label input-sm"
              for="stepDescription"
            >
              {{ $t("Workflow.step.property.description.label") }}
            </label>
            <div class="col-sm-10">
              <input
                id="stepDescription"
                data-testid="step-description"
                v-model="stepDescription"
                type="text"
                name="stepDescription"
                size="100"
                class="form-control input-sm"
              />
            </div>
            <div class="col-sm-10 col-sm-offset-2 help-block">
              {{ $t("Workflow.step.property.description.help") }}
            </div>
          </div>
        </div>

        <section>
          <div class="form-group">
            <label class="col-sm-2 control-label">
              {{ $t("Workflow.Step.jobreference.title") }}
            </label>
            <div class="col-sm-10">
              <div class="radio">
                <input
                  id="useNameTrue"
                  v-model="isUseName"
                  type="radio"
                  name="useName"
                  :value="true"
                />
                <label for="useNameTrue">
                  {{ $t("Workflow.Step.jobreference.name.label") }}
                  <span>{{
                    $t("Workflow.Step.jobreference.name.description")
                  }}</span>
                </label>
              </div>
              <div class="radio">
                <input
                  id="useNameFalse"
                  v-model="isUseName"
                  type="radio"
                  name="useName"
                  :value="false"
                />
                <label for="useNameFalse">
                  {{ $t("Workflow.Step.jobreference.uuid.label") }}
                  <span>{{
                    $t("Workflow.Step.jobreference.uuid.description")
                  }}</span>
                </label>
              </div>
            </div>
          </div>

          <div class="form-group">
            <label class="col-sm-2 control-label" for="jobNameField">
              {{ $t("Workflow.Step.jobreference.name-group.label") }}
            </label>
            <div class="col-sm-6">
              <select
                id="jobProjectField"
                v-model="editModel.jobref.project"
                data-testid="jobProjectField"
                name="jobProject"
                class="form-control"
              >
                <option
                  v-for="project in projectStore.projects"
                  :key="project.name"
                  :value="project.name"
                >
                  {{
                    project.name === selectedProject
                      ? $t("step.type.jobreference.project.label", [
                          selectedProject,
                        ])
                      : project.name
                  }}
                </option>
              </select>
            </div>
            <div class="col-sm-2">
              <span
                :id="`jobChooseBtn${rkey}`"
                class="btn btn-sm btn-default act_choose_job"
                :title="$t('select.an.existing.job.to.use')"
                @click="openJobBrowser"
              >
                {{
                  $t(!jobBrowserLoading ? "choose.a.job..." : "loading.text")
                }}
              </span>
            </div>
            <div class="col-sm-10 col-sm-offset-2" style="margin-top: 1em">
              <p class="help-block">
                {{ $t("Workflow.Step.jobreference.jobName.help") }}
              </p>
            </div>
          </div>

          <div
            class="form-group"
            :class="{ 'has-error': showRequired && isUseName }"
          >
            <label class="col-sm-2 control-label"></label>
            <div class="col-sm-5">
              <input
                :id="`jobNameField${rkey}`"
                v-model="editModel.jobref.name"
                type="text"
                name="jobName"
                data-testid="jobNameField"
                :placeholder="$t('scheduledExecution.jobName.label')"
                class="form-control"
                size="100"
                :readonly="!isUseName"
              />
            </div>
            <div class="col-sm-5">
              <input
                :id="`jobGroupField${rkey}`"
                v-model="editModel.jobref.group"
                data-testid="jobGroupField"
                type="text"
                name="jobGroup"
                size="100"
                :placeholder="$t('scheduledExecution.groupPath.label')"
                class="form-control"
                :readonly="!isUseName"
              />
            </div>
          </div>

          <div
            class="form-group"
            :class="{ 'has-error': showRequired && !isUseName }"
          >
            <label class="col-sm-2 control-label">
              {{ $t("Workflow.Step.uuid.label") }}
            </label>
            <div class="col-sm-10">
              <PtAutoComplete
                :id="`jobUuidField${rkey}`"
                v-model="editModel.jobref.uuid"
                class="context_var_autocomplete"
                name="uuid"
                data-testid="jobUuidField"
                :suggestions="inputTypeContextVariables"
                :read-only="!!isUseName"
                :placeholder="$t('Workflow.Step.jobreference.uuid.placeholder')"
              />
            </div>
          </div>
          <div class="form-group" style="margin-top: 1em">
            <div class="col-sm-10 col-sm-offset-2">
              <div class="text-info">
                {{ $t("Workflow.Step.jobreference.uuid.help") }}
              </div>
            </div>
          </div>

          <div class="form-group">
            <label class="col-sm-2 control-label">
              {{ $t("Workflow.Step.argString.label") }}
            </label>
            <div class="col-sm-10">
              <PtAutoComplete
                id="jobArgStringField"
                v-model="editModel.jobref.args"
                name="argString"
                :suggestions="inputTypeContextVariables"
                :placeholder="
                  $t('Workflow.Step.jobreference.argString.placeholder')
                "
              />
            </div>
          </div>

          <div class="form-group">
            <label class="col-sm-2 control-label"></label>
            <div class="col-sm-10">
              <div class="checkbox">
                <input
                  id="importOptionsCheck"
                  v-model="editModel.jobref.importOptions"
                  type="checkbox"
                  name="importOptions"
                  :value="true"
                />
                <label for="importOptionsCheck">
                  {{ $t("Workflow.Step.jobreference.import.options.label") }}
                </label>
                <span>{{
                  $t("Workflow.Step.jobreference.import.options.help")
                }}</span>
              </div>
            </div>
          </div>

          <div class="form-group">
            <label class="col-sm-2 control-label"></label>
            <div class="col-sm-10">
              <div class="checkbox">
                <input
                  id="ignoreNotificationsCheck"
                  v-model="editModel.jobref.ignoreNotifications"
                  type="checkbox"
                  name="ignoreNotifications"
                  :value="true"
                />
                <label for="ignoreNotificationsCheck">
                  {{
                    $t("Workflow.Step.jobreference.ignore.notifications.label")
                  }}
                </label>
                <span>{{
                  $t("Workflow.Step.jobreference.ignore.notifications.help")
                }}</span>
              </div>
            </div>
          </div>

          <div class="form-group">
            <label class="col-sm-2 control-label"></label>
            <div class="col-sm-10">
              <div class="checkbox">
                <input
                  id="failOnDisableCheck"
                  v-model="editModel.jobref.failOnDisable"
                  type="checkbox"
                  name="failOnDisable"
                  :value="true"
                />
                <label for="failOnDisableCheck">
                  {{ $t("Workflow.Step.jobreference.fail.on.disabled.label") }}
                </label>
                <span>{{
                  $t("Workflow.Step.jobreference.fail.on.disabled.help")
                }}</span>
              </div>
            </div>
          </div>

          <div class="form-group">
            <label class="col-sm-2 control-label"></label>
            <div class="col-sm-10">
              <div class="checkbox">
                <input
                  id="childNodesCheck"
                  v-model="editModel.jobref.childNodes"
                  type="checkbox"
                  name="childNodes"
                  :value="true"
                />
                <label for="childNodesCheck">
                  {{ $t("Workflow.Step.jobreference.child.nodes.label") }}
                </label>
                <span>{{
                  $t("Workflow.Step.jobreference.child.nodes.help")
                }}</span>
              </div>
            </div>
          </div>

          <div class="row">
            <div class="col-sm-2 control-label">
              <span
                class="btn btn-sm"
                data-testid="expandNodeFilterBtn"
                :class="{ active: nodeFilterOverrideExpanded && filterLoaded }"
                @click="
                  nodeFilterOverrideExpanded = !nodeFilterOverrideExpanded
                "
              >
                {{ $t("override.node.filters") }}
                <i
                  class="glyphicon"
                  :class="`glyphicon-chevron-${nodeFilterOverrideExpanded ? 'down' : 'right'}`"
                ></i>
              </span>
            </div>
          </div>
        </section>

        <section
          :id="`nodeFilterOverride${rkey}`"
          data-testid="nodeFilterContainer"
          class="collapse-expandable collapse node_filter_link_holder section-separator-solo"
          :class="{ in: nodeFilterOverrideExpanded }"
        >
          <div class="form-group" style="margin-top: 1em">
            <div class="col-sm-12">
              <div class="text-info">
                {{ $t("JobExec.property.nodeFilter.help.description") }}
              </div>
            </div>
          </div>

          <div class="form-group">
            <label class="col-sm-2 control-label">
              {{ $t("scheduledExecution.property.nodeIntersect.label") }}
            </label>
            <div class="col-sm-10">
              <div class="radio">
                <input
                  id="nodeIntersectFalse"
                  v-model="editModel.jobref.nodefilters.dispatch.nodeIntersect"
                  type="radio"
                  name="nodeIntersect"
                  :value="null"
                />
                <label for="nodeIntersectFalse">
                  {{ $t("scheduledExecution.property.nodeIntersect.false") }}
                </label>
              </div>
              <div class="radio">
                <input
                  id="nodeIntersectTrue"
                  v-model="editModel.jobref.nodefilters.dispatch.nodeIntersect"
                  type="radio"
                  name="nodeIntersect"
                  :value="true"
                />
                <label for="nodeIntersectTrue">
                  {{ $t("scheduledExecution.property.nodeIntersect.true") }}
                </label>
              </div>
            </div>
          </div>

          <div class="form-group">
            <label
              class="col-sm-2 control-label"
              :for="`nodeFilterField${rkey}`"
            >
              {{ $t("node.filter.prompt") }}
            </label>
            <div class="col-sm-10 vue-ui-socket">
              <node-filter-input
                :value="editModel.jobref.nodefilters.filter"
                :project="currentProject"
                filter-field-name="nodeFilter"
                :filter-field-id="`nodeFilterField${rkey}`"
                :query-field-placeholder-text="
                  $t('enter.a.node.filter.override')
                "
                emit-filter-on-blur
                search-btn-type="cta"
                class="nodefilters"
                @update:value="updatedValue"
                @filter="filterClicked"
              />
            </div>
          </div>

          <div class="form-group">
            <label class="col-sm-2 control-label">
              {{ $t("matched.nodes.prompt") }}
            </label>
            <div class="col-sm-10">
              <div class="well well-sm embed matchednodes">
                <template v-if="filterLoaded">
                  <button
                    type="button"
                    class="pull-right btn btn-sm refresh_nodes"
                    :title="$t('click.to.refresh')"
                    @click="triggerFetchNodes"
                  >
                    {{ $t(nodeFilterLoading ? "loading.text" : "refresh") }}
                    <i class="glyphicon glyphicon-refresh"></i>
                  </button>
                  <span v-if="hasFilter && total">
                    {{
                      $t("count.nodes.matched", [
                        total,
                        $t("Node.count.vue", total),
                      ])
                    }}
                  </span>
                  <span v-else-if="!hasFilter || total === 0">
                    {{ $t("JobExec.property.nodeFilter.null.description") }}
                  </span>
                  <div
                    v-if="hasFilter && currentNodes.length"
                    :id="`matchednodes${rkey}`"
                    class="clearfix"
                  >
                    <node-list-embed
                      :nodes="currentNodes"
                      @filter="filterClicked"
                    >
                      <p v-if="isResultsTruncated" class="text-info mb-0">
                        {{
                          $t("results.truncated.count.results.shown", [total])
                        }}
                      </p>
                    </node-list-embed>
                  </div>
                </template>
                <i v-else class="fas fa-spinner fa-pulse"></i>
              </div>
            </div>
          </div>

          <div class="form-group">
            <label
              class="col-sm-2 control-label"
              :for="`nodeThreadcountField${rkey}`"
            >
              {{ $t("scheduledExecution.property.nodeThreadcount.label") }}
            </label>
            <div class="col-sm-2">
              <input
                :id="`nodeThreadcountField${rkey}`"
                v-model="editModel.jobref.nodefilters.dispatch.threadcount"
                data-testid="nodeThreadcountField"
                type="number"
                name="nodeThreadcount"
                min="1"
                size="3"
                class="form-control"
                :disabled="!hasFilter"
              />
            </div>
            <div class="col-sm-8 help-block">
              {{ $t("JobExec.property.nodeThreadcount.null.description") }}
            </div>
          </div>

          <div class="form-group">
            <label class="col-sm-2 control-label">
              {{ $t("scheduledExecution.property.nodeKeepgoing.prompt") }}
            </label>
            <div class="col-sm-10">
              <div class="radio">
                <input
                  id="nodeKeepgoingNull"
                  v-model="editModel.jobref.nodefilters.dispatch.keepgoing"
                  type="radio"
                  name="nodeKeepgoing"
                  :value="null"
                  :disabled="!hasFilter"
                />
                <label for="nodeKeepgoingNull">
                  {{ $t("JobExec.property.nodeKeepgoing.null.description") }}
                </label>
              </div>
              <div class="radio">
                <input
                  id="nodeKeepgoingTrue"
                  v-model="editModel.jobref.nodefilters.dispatch.keepgoing"
                  data-testid="nodeKeepgoingTrue"
                  type="radio"
                  name="nodeKeepgoing"
                  :value="true"
                  :disabled="!hasFilter"
                />
                <label for="nodeKeepgoingTrue">
                  {{ $t("Workflow.property.keepgoing.true.description") }}
                </label>
              </div>
              <div class="radio">
                <input
                  id="nodeKeepgoingFalse"
                  v-model="editModel.jobref.nodefilters.dispatch.keepgoing"
                  type="radio"
                  name="nodeKeepgoing"
                  :value="false"
                  :disabled="!hasFilter"
                />
                <label for="nodeKeepgoingFalse">
                  {{ $t("Workflow.property.keepgoing.false.description") }}
                </label>
              </div>
            </div>
          </div>

          <div class="form-group">
            <label
              class="col-sm-2 control-label"
              :for="`nodeRankAttributeField${rkey}`"
            >
              {{ $t("scheduledExecution.property.nodeRankAttribute.label") }}
            </label>
            <div class="col-sm-10">
              <input
                :id="`nodeRankAttributeField${rkey}`"
                v-model="editModel.jobref.nodefilters.dispatch.rankAttribute"
                :disabled="!hasFilter"
                type="text"
                name="nodeRankAttribute"
                class="form-control"
                :placeholder="
                  $t(
                    'scheduledExecution.property.nodeRankAttribute.description',
                  )
                "
              />
            </div>
          </div>

          <div class="form-group">
            <label
              class="col-sm-2 control-label"
              :for="`nodeRankOrderField${rkey}`"
            >
              {{ $t("scheduledExecution.property.nodeRankOrder.label") }}
            </label>
            <div class="col-sm-10">
              <div class="radio">
                <input
                  id="nodeRankOrderAscendingNull"
                  v-model="editModel.jobref.nodefilters.dispatch.rankOrder"
                  type="radio"
                  name="nodeRankOrderAscending"
                  :value="null"
                  :disabled="!hasFilter"
                />
                <label for="nodeRankOrderAscendingNull">
                  {{ $t("JobExec.property.nodeRankOrder.null.description") }}
                </label>
              </div>
              <div class="radio">
                <input
                  id="nodeRankOrderAscending"
                  v-model="editModel.jobref.nodefilters.dispatch.rankOrder"
                  type="radio"
                  name="nodeRankOrderAscending"
                  value="ascending"
                  :disabled="!hasFilter"
                />
                <label for="nodeRankOrderAscending">
                  {{
                    $t(
                      "scheduledExecution.property.nodeRankOrder.ascending.label",
                    )
                  }}
                </label>
              </div>
              <div class="radio">
                <input
                  id="nodeRankOrderDescending"
                  v-model="editModel.jobref.nodefilters.dispatch.rankOrder"
                  type="radio"
                  name="nodeRankOrderAscending"
                  value="descending"
                  :disabled="!hasFilter"
                />
                <label for="nodeRankOrderDescending">
                  {{
                    $t(
                      "scheduledExecution.property.nodeRankOrder.descending.label",
                    )
                  }}
                </label>
              </div>
            </div>
          </div>
        </section>

        <!-- Node Step Toggle -->
        <section>
          <div class="form-group">
            <label class="col-sm-2 control-label">
              {{ $t("JobExec.nodeStep.title") }}
            </label>
            <div class="col-sm-10">
              <div class="radio">
                <input
                  id="jobNodeStepFieldTrue"
                  v-model="editModel.jobref.nodeStep"
                  type="radio"
                  name="nodeStep"
                  :value="true"
                />
                <label for="jobNodeStepFieldTrue">
                  {{ $t("JobExec.nodeStep.true.label") }}
                </label>
                <span>{{ $t("JobExec.nodeStep.true.description") }}</span>
              </div>
              <div class="radio">
                <input
                  id="jobNodeStepFieldFalse"
                  v-model="editModel.jobref.nodeStep"
                  type="radio"
                  name="nodeStep"
                  :value="false"
                />
                <label for="jobNodeStepFieldFalse">
                  {{ $t("JobExec.nodeStep.false.label") }}
                </label>
                <span>{{ $t("JobExec.nodeStep.false.description") }}</span>
              </div>
            </div>
          </div>
        </section>
      </div>

      <div v-else-if="isConditionalLogic" class="conditional-logic-content">
        <div class="step-name-section">
          <div class="form-group">
            <label
              class="col-sm-2 control-label input-sm"
              for="stepDescription"
            >
              {{ $t("Workflow.step.property.description.label") }}
            </label>
            <div class="col-sm-10">
              <input
                id="stepDescription"
                data-testid="step-description"
                v-model="stepDescription"
                type="text"
                name="stepDescription"
                size="100"
                class="form-control input-sm"
              />
            </div>
            <div class="col-sm-10 col-sm-offset-2 help-block">
              {{ $t("Workflow.step.property.description.help") }}
            </div>
          </div>
        </div>

        <div class="condition-section">
          <ConditionsEditor
            ref="conditionsEditor"
            v-model="conditionSets"
            :service-name="serviceName"
            :extra-autocomplete-vars="extraAutocompleteVars"
          />
        </div>

        <div class="conditional-divider"></div>

        <div class="steps-section">
          <InnerStepList
            v-model="innerCommands"
            :target-service="serviceName"
            :depth="depth + 1"
            :extra-autocomplete-vars="extraAutocompleteVars"
          />
        </div>
      </div>

      <div v-else-if="provider">
        <div class="step-name-section">
          <div class="form-group">
            <label
              class="col-sm-2 control-label input-sm"
              for="stepDescription"
              >{{ $t("Workflow.step.property.description.label") }}</label
            >
            <div class="col-sm-10">
              <input
                id="stepDescription"
                data-testid="step-description"
                v-model="stepDescription"
                type="text"
                name="stepDescription"
                size="100"
                class="form-control input-sm"
              />
            </div>
            <div class="col-sm-10 col-sm-offset-2 help-block">
              {{ $t("Workflow.step.property.description.help") }}
            </div>
          </div>
        </div>
        <plugin-config
          v-model="editModel"
          :mode="pluginConfigMode"
          :plugin-config="provider"
          :show-title="false"
          :show-description="false"
          :context-autocomplete="true"
          :validation="validation"
          scope="Instance"
          default-scope="Instance"
          group-css=""
          description-css="ml-5"
          data-testid="plugin-info"
          :service-name="serviceName"
          :extra-autocomplete-vars="extraAutocompleteVars"
        />
      </div>

      <div v-else-if="loading" class="loading-container">
        <i class="fas fa-spinner fa-spin"></i>
        <span>{{ $t("loading.text") }}</span>
      </div>
    </template>
    <template #footer>
      <div
        v-show="depth === 0 || contentExpanded"
        class="edit-step-card-footer"
      >
        <PtButton
          outlined
          severity="secondary"
          :label="$t('Cancel')"
          data-testid="cancel-button"
          @click="handleCancel"
        />
        <PtButton
          severity="primary"
          :label="$t('Save')"
          data-testid="save-button"
          @click="handleSave"
        />
      </div>
    </template>
  </BaseStepCard>

  <modal
    v-if="isJobRef && openJobSelectionModal"
    v-model="openJobSelectionModal"
    :title="$t('choose.a.job...')"
    size="lg"
  >
    <ui-socket
      v-if="showFavoritesButton"
      section="job-list-page"
      location="card-header"
    />
    <ui-socket
      section="job-list-page"
      location="tree-browser"
      :socket-data="{
        showBulkEdit: false,
        showCreateButton: false,
        allowFolderNavigation: false,
        projectToDisplay: editModel.jobref.project || selectedProject,
        key: editModel.jobref.project,
      }"
    />
  </modal>
</template>

<script lang="ts">
import { defineComponent, defineAsyncComponent, type PropType } from "vue";
import BaseStepCard from "@/library/components/primeVue/StepCards/BaseStepCard.vue";
import pluginConfig from "@/library/components/plugins/pluginConfig.vue";
import PtButton from "@/library/components/primeVue/PtButton/PtButton.vue";
import StepCardHeader from "@/library/components/primeVue/StepCards/StepCardHeader.vue";
import { PluginConfig } from "@/library/interfaces/PluginConfig";
import { getServiceProviderDescription } from "@/library/modules/pluginService";
import { ContextVariable } from "@/library/stores/contextVariables";
import { cloneDeep, merge } from "lodash";
import { getRundeckContext } from "@/library";
import { mapState, mapActions } from "pinia";
import { useNodesStore } from "@/library/stores/NodesStorePinia";
import NodeFilterInput from "@/app/components/job/resources/NodeFilterInput.vue";
import NodeListEmbed from "@/app/components/job/resources/NodeListEmbed.vue";
import PtAutoComplete from "@/library/components/primeVue/PtAutoComplete/PtAutoComplete.vue";
import UiSocket from "@/library/components/utils/UiSocket";
import {
  getContextVariables,
  transformVariables,
} from "@/library/components/utils/contextVariableUtils";
import ConditionsEditor from "./ConditionsEditor.vue";
import type { ConditionSet } from "./types/conditionalStepTypes";
import { createEmptyConditionSet } from "./types/conditionalStepTypes";
import type { EditStepData } from "./types/workflowTypes";

const rundeckContext = getRundeckContext();
const eventBus = rundeckContext.eventBus;

export default defineComponent({
  name: "EditStepCard",
  components: {
    BaseStepCard,
    pluginConfig,
    PtButton,
    StepCardHeader,
    NodeFilterInput,
    NodeListEmbed,
    PtAutoComplete,
    UiSocket,
    ConditionsEditor,
    InnerStepList: defineAsyncComponent(() => import("./InnerStepList.vue")),
  },
  provide() {
    return {
      showJobsAsLinks: false,
    };
  },
  props: {
    modelValue: {
      type: Object,
      required: true,
      default: () => ({}) as PluginConfig,
    },
    serviceName: {
      type: String,
      required: true,
    },
    pluginDetails: {
      type: Object,
      required: false,
      default: null,
    },
    validation: {
      type: Object,
      required: false,
      default: () => ({}),
    },
    extraAutocompleteVars: {
      type: Array as PropType<ContextVariable[]>,
      required: false,
      default: () => [],
    },
    showNavigation: {
      type: Boolean,
      default: false,
    },
    depth: {
      type: Number,
      default: 0,
    },
  },
  emits: ["cancel", "save", "update:modelValue"],
  data() {
    return {
      contentExpanded: true,
      editModel: {} as any,
      stepDescription: "",
      provider: null as any,
      loading: false,
      pluginConfigMode: "edit",
      // Job reference specific data
      isUseName: false,
      nodeFilterOverrideExpanded: null,
      projectStore: rundeckContext.rootStore.projects,
      selectedProject: rundeckContext.projectName,
      currentProject: rundeckContext.projectName,
      filterLoaded: false,
      nodeFilterLoading: false,
      jobBrowserLoading: false,
      openJobSelectionModal: false,
      showFavoritesButton: true,
      queryParams: {
        view: "embed",
        declarenone: true,
        fullresults: true,
        expanddetail: true,
        inlinepaging: false,
        maxShown: 20,
      },
      rkey:
        "r_" + Math.floor(Math.random() * Math.floor(1024)).toString(16) + "_",
      validationError: "",
      showRequired: false,
      eventBus: eventBus,
      // Conditional logic specific data
      conditionSets: [createEmptyConditionSet()] as ConditionSet[],
      innerCommands: [] as EditStepData[],
      // Job reference defaults (matching JobRefForm)
      jobRefDefaults: {
        description: "",
        jobref: {
          nodeStep: false,
          name: "",
          uuid: "",
          project: rundeckContext.projectName,
          group: "",
          args: "",
          failOnDisable: false,
          childNodes: false,
          importOptions: false,
          ignoreNotifications: false,
          nodefilters: {
            filter: "",
            dispatch: {
              threadcount: null,
              keepgoing: null,
              rankAttribute: null,
              rankOrder: null,
              nodeIntersect: null,
            },
          },
        },
      },
    };
  },
  computed: {
    isJobRef() {
      return Boolean(this.editModel?.jobref);
    },
    isConditionalLogic() {
      return this.editModel?.type === "conditional.logic";
    },
    stepConfig() {
      // Computed config that includes description for StepCardHeader
      return {
        ...this.editModel,
        description: this.stepDescription,
      };
    },
    hasFilter() {
      return Boolean(this.editModel?.jobref?.nodefilters?.filter);
    },
    inputTypeContextVariables() {
      return [
        ...getContextVariables("input", "WorkflowStep"),
        ...transformVariables("input", this.extraAutocompleteVars),
      ];
    },
    ...mapState(useNodesStore, [
      "total",
      "nodeFilterStore",
      "currentNodes",
      "lastCountFetched",
      "isResultsTruncated",
    ]),
  },
  watch: {
    modelValue: {
      handler(val) {
        if (val && Object.keys(val).length > 0) {
          // Extract description separately
          this.stepDescription = val.description || "";

          // For jobref: merge with defaults (like JobRefForm does)
          // For conditional.logic: extract conditionSets and commands
          // For regular steps: clone as-is
          if (val.jobref) {
            this.editModel = merge(cloneDeep(this.jobRefDefaults), val);
          } else {
            const { description, ...rest } = val;
            this.editModel = cloneDeep(rest);
          }

          // Initialize conditional logic data
          if (this.editModel.type === "conditional.logic") {
            this.conditionSets = this.editModel.config?.conditionSets || [
              createEmptyConditionSet(),
            ];
            this.innerCommands = this.editModel.config?.commands || [];
          }

          this.loadProvider();

          // Job reference specific initialization
          if (this.isJobRef) {
            this.initializeJobRefState();
          }
        }
      },
      immediate: true,
      deep: true,
    },
    "nodeFilterStore.filter": {
      async handler(val) {
        if (this.isJobRef && val !== this.editModel.jobref.nodefilters.filter) {
          this.editModel.jobref.nodefilters.filter = val;
          await this.triggerFetchNodes();
        }
      },
    },
  },
  async mounted() {
    this.stepDescription = this.modelValue.description || "";

    // For jobref: merge with defaults (like JobRefForm does)
    // For regular steps: clone as-is
    if (this.modelValue.jobref) {
      this.editModel = merge(cloneDeep(this.jobRefDefaults), this.modelValue);
    } else {
      const { description, ...rest } = this.modelValue;
      this.editModel = cloneDeep(rest);
    }

    // Initialize conditional logic data
    if (this.editModel.type === "conditional.logic") {
      this.conditionSets = this.editModel.config?.conditionSets || [
        createEmptyConditionSet(),
      ];
      this.innerCommands = this.editModel.config?.commands || [];
    }

    if (
      this.modelValue.config &&
      Object.keys(this.modelValue.config).length === 0
    ) {
      this.pluginConfigMode = "create";
    }

    await this.loadProvider();

    // Job reference specific mounting
    if (this.isJobRef) {
      await this.mountJobRef();
    }
  },
  beforeUnmount() {
    // Clean up event bus listeners for job browser
    if (this.isJobRef && this.eventBus) {
      this.eventBus.off(`browser-job-item-selection`, this.updateJobSelection);
      this.eventBus.off(`browser-jobs-empty`, this.handleFavoritesButton);
    }
  },
  methods: {
    handleSave() {
      // Validate job reference before saving
      if (this.isJobRef) {
        if (!this.editModel.jobref.name && !this.editModel.jobref.uuid) {
          this.validationError = this.$t("commandExec.jobName.blank.message");
          this.showRequired = true;
          return;
        }
        this.validationError = "";
        this.showRequired = false;
      }

      // Validate conditional logic before saving
      if (this.isConditionalLogic) {
        const conditionsEditor = this.$refs.conditionsEditor as InstanceType<
          typeof ConditionsEditor
        >;
        if (conditionsEditor && !conditionsEditor.validate()) {
          return;
        }

        const saveData = {
          ...this.editModel,
          description: this.stepDescription,
          config: {
            ...this.editModel.config,
            conditionSets: this.conditionSets,
            commands: this.innerCommands,
          },
        };
        this.$emit("update:modelValue", saveData);
        this.$emit("save");
        return;
      }

      // Merge description back into editModel before emitting
      const saveData = {
        ...this.editModel,
        description: this.stepDescription,
      };
      this.$emit("update:modelValue", saveData);
      this.$emit("save");
    },
    handleCancel() {
      this.$emit("cancel");
    },
    async loadProvider() {
      // If plugin-details prop is provided, use it (no need to fetch)
      if (this.pluginDetails) {
        this.provider = this.pluginDetails;
        this.loading = false;
        return;
      }

      // Otherwise, fetch from API (existing behavior for backwards compatibility)
      if (this.editModel.type) {
        try {
          this.loading = true;
          this.provider = await getServiceProviderDescription(
            this.serviceName,
            this.editModel.type,
          );
        } catch (e) {
          console.log(e);
        } finally {
          this.loading = false;
        }
      } else {
        this.loading = false;
        this.provider = null;
      }
    },

    // Job Reference specific methods
    initializeJobRefState() {
      // Initialize project store if needed
      if (!this.projectStore.loaded) {
        this.projectStore.load();
      }

      // Set expanded state for node filter if there's a filter
      if (
        this.editModel.jobref.nodefilters?.filter ||
        this.editModel.jobref.nodefilters?.dispatch?.nodeIntersect
      ) {
        this.nodeFilterOverrideExpanded = true;
      }
    },

    async mountJobRef() {
      if (!this.projectStore.loaded) {
        await this.projectStore.load();
      }

      // Initialize node filter if exists
      if (this.editModel.jobref.nodefilters.filter) {
        this.nodeFilterStore.setSelectedFilter(
          this.editModel.jobref.nodefilters.filter,
        );

        if (rundeckContext.data) {
          this.queryParams = merge(
            this.queryParams,
            rundeckContext.data.nodeData,
          );
        }

        this.nodeFilterOverrideExpanded = Boolean(
          this.editModel.jobref.nodefilters.filter.length ||
            this.editModel.jobref.nodefilters.dispatch.nodeIntersect,
        );

        await this.triggerFetchNodes();
      }

      this.filterLoaded = true;

      // Set up event bus listeners for job browser
      this.eventBus.on(`browser-job-item-selection`, this.updateJobSelection);
      this.eventBus.on(`browser-jobs-empty`, this.handleFavoritesButton);
    },

    openJobBrowser() {
      this.openJobSelectionModal = true;
      this.showFavoritesButton = true;
    },

    updateJobSelection(job: any) {
      this.editModel.jobref.uuid = job.id;
      this.editModel.jobref.name = job.jobName;
      this.editModel.jobref.group = job.groupPath;
      this.jobBrowserLoading = false;
      this.openJobSelectionModal = false;
    },

    handleFavoritesButton() {
      this.showFavoritesButton = false;
    },

    updatedValue(val: string) {
      this.nodeFilterStore.setSelectedFilter(val);
    },

    filterClicked(filter: any) {
      this.nodeFilterStore.setSelectedFilter(filter.filter);
    },

    async triggerFetchNodes() {
      if (this.hasFilter) {
        try {
          this.nodeFilterLoading = true;
          await this.fetchNodes(this.queryParams);
        } catch (e) {
          console.error("Error fetching nodes:", e);
        } finally {
          this.nodeFilterLoading = false;
        }
      }
    },

    ...mapActions(useNodesStore, ["fetchNodes"]),
  },
});
</script>

<style lang="scss">
.edit-step-card {
  margin-bottom: var(--sizes-4);

  &-footer {
    display: flex;
    justify-content: flex-end;
    gap: var(--sizes-2);
    padding: var(--sizes-4);
    border-top: 1px solid var(--colors-gray-300-original);
  }
}

.loading-container {
  display: flex;
  align-items: center;
  gap: var(--sizes-2);
  padding: var(--sizes-4);
}

.step-name-section {
  display: flex;
  flex-direction: column;
  gap: var(--sizes-1);
  margin-bottom: var(--sizes-4);
}

// Conditional logic specific styles
.conditional-logic-content {
  display: flex;
  flex-direction: column;
  gap: var(--sizes-6);

  .condition-section {
    display: flex;
    flex-direction: column;
    gap: var(--sizes-4);
  }

  .conditional-divider {
    height: 1px;
    background: var(--colors-gray-200);
    margin: var(--sizes-2) 0;
  }

  .steps-section {
    display: flex;
    flex-direction: column;
    gap: var(--sizes-4);
  }
}

// Job Reference specific styles
.jobref-form-content {
  .mb-0 {
    margin-bottom: 0;
  }

  .matchednodes {
    overflow: hidden;
  }

  .checkbox,
  .radio {
    display: flex;
    align-items: center;
    gap: 5px;

    &:first-child {
      margin-top: 10px;
    }
  }

  .collapse-expandable {
    &.collapse {
      display: none;

      &.in {
        display: block;
      }
    }
  }

  .section-separator-solo {
    margin-top: 1em;
  }
}
</style>
