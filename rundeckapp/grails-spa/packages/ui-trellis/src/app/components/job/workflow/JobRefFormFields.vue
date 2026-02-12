<template>
  <div class="jobref-form-fields">
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
            v-model="modelValue.project"
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
        :class="{ 'has-error': showValidation && isUseName }"
      >
        <label class="col-sm-2 control-label"></label>
        <div class="col-sm-5">
          <input
            :id="`jobNameField${rkey}`"
            v-model="modelValue.name"
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
            v-model="modelValue.group"
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
        :class="{ 'has-error': showValidation && !isUseName }"
      >
        <label class="col-sm-2 control-label">
          {{ $t("Workflow.Step.uuid.label") }}
        </label>
        <div class="col-sm-10">
          <PtAutoComplete
            :id="`jobUuidField${rkey}`"
            v-model="modelValue.uuid"
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
            v-model="modelValue.args"
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
              v-model="modelValue.importOptions"
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
              v-model="modelValue.ignoreNotifications"
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
              v-model="modelValue.failOnDisable"
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
              v-model="modelValue.childNodes"
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
              v-model="modelValue.nodefilters.dispatch.nodeIntersect"
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
              v-model="modelValue.nodefilters.dispatch.nodeIntersect"
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
            :value="modelValue.nodefilters.filter"
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
            v-model="modelValue.nodefilters.dispatch.threadcount"
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
              v-model="modelValue.nodefilters.dispatch.keepgoing"
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
              v-model="modelValue.nodefilters.dispatch.keepgoing"
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
              v-model="modelValue.nodefilters.dispatch.keepgoing"
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
            v-model="modelValue.nodefilters.dispatch.rankAttribute"
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
              v-model="modelValue.nodefilters.dispatch.rankOrder"
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
              v-model="modelValue.nodefilters.dispatch.rankOrder"
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
              v-model="modelValue.nodefilters.dispatch.rankOrder"
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
              v-model="modelValue.nodeStep"
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
              v-model="modelValue.nodeStep"
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

    <Teleport to="body">
      <modal
        v-if="openJobSelectionModal"
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
            projectToDisplay: modelValue.project || selectedProject,
            key: modelValue.project,
          }"
        />
      </modal>
    </Teleport>
  </div>
</template>

<script lang="ts">
import { defineComponent, type PropType } from "vue";
import { getRundeckContext } from "@/library";
import NodeFilterInput from "@/app/components/job/resources/NodeFilterInput.vue";
import NodeListEmbed from "@/app/components/job/resources/NodeListEmbed.vue";
import PtAutoComplete from "@/library/components/primeVue/PtAutoComplete/PtAutoComplete.vue";
import UiSocket from "@/library/components/utils/UiSocket";
import { ContextVariable } from "@/library/stores/contextVariables";
import { cloneDeep, merge } from "lodash";
import { mapState, mapActions } from "pinia";
import { useNodesStore } from "@/library/stores/NodesStorePinia";
import {
  getContextVariables,
  transformVariables,
} from "@/library/components/utils/contextVariableUtils";

const rundeckContext = getRundeckContext();
const eventBus = rundeckContext.eventBus;

interface JobRefModel {
  name: string;
  uuid: string;
  project: string;
  group: string;
  args: string;
  failOnDisable: boolean;
  childNodes: boolean;
  importOptions: boolean;
  ignoreNotifications: boolean;
  nodeStep: boolean;
  nodefilters: {
    filter: string;
    dispatch: {
      threadcount: number | null;
      keepgoing: boolean | null;
      rankAttribute: string | null;
      rankOrder: string | null;
      nodeIntersect: boolean | null;
    };
  };
}

export default defineComponent({
  name: "JobRefFormFields",
  components: {
    NodeFilterInput,
    NodeListEmbed,
    PtAutoComplete,
    UiSocket,
  },
  provide() {
    return {
      showJobsAsLinks: false,
    };
  },
  props: {
    modelValue: {
      type: Object as PropType<JobRefModel>,
      required: true,
    },
    showValidation: {
      type: Boolean,
      default: false,
    },
    extraAutocompleteVars: {
      type: Array as PropType<ContextVariable[]>,
      required: false,
      default: () => [],
    },
  },
  emits: ["update:modelValue"],
  data() {
    return {
      isUseName: false,
      nodeFilterOverrideExpanded: null as boolean | null,
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
      eventBus: eventBus,
    };
  },
  computed: {
    hasFilter() {
      return Boolean(this.modelValue?.nodefilters?.filter);
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
    "nodeFilterStore.filter": {
      async handler(val) {
        if (val !== this.modelValue.nodefilters.filter) {
          this.modelValue.nodefilters.filter = val;
          await this.triggerFetchNodes();
        }
      },
    },
  },
  mounted() {
    // Set up event bus listeners
    this.eventBus.on(`browser-job-item-selection`, this.updateJobSelection);
    this.eventBus.on(`browser-jobs-empty`, this.handleFavoritesButton);

    // Set expanded state for node filter if there's a filter
    if (
      this.modelValue.nodefilters?.filter ||
      this.modelValue.nodefilters?.dispatch?.nodeIntersect
    ) {
      this.nodeFilterOverrideExpanded = true;
    }

    // Async initialization
    this.initializeAsync();
  },
  beforeUnmount() {
    // Clean up event bus listeners
    if (this.eventBus) {
      this.eventBus.off(`browser-job-item-selection`, this.updateJobSelection);
      this.eventBus.off(`browser-jobs-empty`, this.handleFavoritesButton);
    }
  },
  methods: {
    async initializeAsync() {
      // Load project store if needed
      if (!this.projectStore.loaded) {
        await this.projectStore.load();
      }

      // Initialize node filter if exists
      if (this.modelValue.nodefilters?.filter) {
        this.nodeFilterStore.setSelectedFilter(
          this.modelValue.nodefilters.filter,
        );

        if (rundeckContext.data) {
          this.queryParams = merge(
            this.queryParams,
            rundeckContext.data.nodeData,
          );
        }

        await this.triggerFetchNodes();
      }

      this.filterLoaded = true;
    },

    openJobBrowser() {
      this.openJobSelectionModal = true;
      this.showFavoritesButton = true;
    },

    updateJobSelection(job: any) {
      this.modelValue.uuid = job.id;
      this.modelValue.name = job.jobName;
      this.modelValue.group = job.groupPath;
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

<style lang="scss" scoped>
.jobref-form-fields {
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
