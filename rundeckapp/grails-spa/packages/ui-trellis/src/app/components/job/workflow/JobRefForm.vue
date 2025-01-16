<template>
  <modal v-model="showModal" :title="$t('plugin.edit.title')">
    <div class="wfitemEditForm">
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
              </label>
              <span>{{
                $t("Workflow.Step.jobreference.name.description")
              }}</span>
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
              </label>
              <span>
                {{ $t("Workflow.Step.jobreference.uuid.description") }}
              </span>
            </div>
          </div>
        </div>
        <div class="form-group">
          <label class="col-sm-2 control-label" for="jobNameField">{{
            $t("Workflow.Step.jobreference.name-group.label")
          }}</label>
          <div class="col-sm-6">
            <select
              id="jobProjectField"
              v-model="editModel.jobref.project"
              name="jobProject"
              class="form-control"
            >
              <option
                v-for="project in projectStore.projects"
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
              @click="openModal"
            >
              {{ $t(!loading ? "choose.a.job..." : "Loading...") }}
            </span>
            <span id="jobChooseSpinner"></span>
          </div>
          <div class="col-sm-10 col-sm-offset-2" style="margin-top: 1em">
            <p class="help-block">
              {{ $t("Workflow.Step.jobreference.jobName.help") }}
            </p>
          </div>
        </div>
        <div class="form-group">
          <label class="col-sm-2 control-label"></label>
          <div class="col-sm-5">
            <input
              :id="`jobNameField${rkey}`"
              v-model="editModel.jobref.name"
              type="text"
              name="jobName"
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
              type="text"
              name="jobGroup"
              size="100"
              :placeholder="$t('scheduledExecution.groupPath.label')"
              class="form-control"
              :readonly="!isUseName"
            />
          </div>
        </div>

        <div class="form-group">
          <label class="col-sm-2 control-label">{{
            $t("Workflow.Step.uuid.label")
          }}</label>
          <div class="col-sm-10">
            <input
              :id="`jobUuidField${rkey}`"
              v-model="editModel.jobref.uuid"
              type="text"
              name="uuid"
              size="100"
              :readonly="!!isUseName"
              :placeholder="$t('Workflow.Step.jobreference.uuid.placeholder')"
              class="form-control context_var_autocomplete"
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
          <label class="col-sm-2 control-label">{{
            $t("Workflow.Step.argString.label")
          }}</label>
          <div class="col-sm-10">
            <input
              id="jobArgStringField"
              v-model="editModel.jobref.args"
              type="text"
              name="argString"
              size="100"
              :placeholder="
                $t('Workflow.Step.jobreference.argString.placeholder')
              "
              class="form-control"
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
              <span>
                {{ $t("Workflow.Step.jobreference.import.options.help") }}
              </span>
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
              <span>
                {{ $t("Workflow.Step.jobreference.ignore.notifications.help") }}
              </span>
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
              :class="{ active: nodeFilterOverrideExpanded }"
              @click="nodeFilterOverrideExpanded = !nodeFilterOverrideExpanded"
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
                v-model="editModel.jobref.nodeIntersect"
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
                v-model="editModel.jobref.nodeIntersect"
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
          <label class="col-sm-2 control-label" :for="`nodeFilterField${rkey}`">
            {{ $t("node.filter.prompt") }}
          </label>

          <div class="col-sm-10 vue-ui-socket">
            <ui-socket
              section="job-wfitem-edit"
              location="jobref-node-filter-input"
              :socket-data="{
                extraAttrs: {
                  class: 'nodefilters',
                  nodeFilterStore: nodeFilterStore,
                },
                filter: '',
                filterFieldName: 'nodeFilter',
                queryFieldPlaceholderText: $t('enter.a.node.filter.override'),
              }"
            />
            <!--                          socket-data="${enc(attr: [
                                 extraAttrs: [ 'class': 'nodefilters' ],
                                 filter: filtvalue?:'',
                                 filterFieldName:'nodeFilter',
                                 filterFieldId:'nodeFilterField'+rkey,
                                 koFieldName:'nodeFilterMap',
                                 koParam:'#nodeFilterOverride'+rkey,
                                 queryFieldPlaceholderText: g.message(code:'enter.a.node.filter.override')
                         ].encodeAsJSON())}"-->
          </div>
        </div>

        <div class="form-group">
          <label class="col-sm-2 control-label">
            {{ $t("matched.nodes.prompt") }}
          </label>

          <div class="col-sm-10">
            <div class="well well-sm embed matchednodes">
              <button
                type="button"
                class="pull-right btn btn-sm refresh_nodes"
                data-loading-text='$t("loading.text")'
                data-bind="click: $data.updateMatchedNodes"
                :title="$t('click.to.refresh')"
              >
                {{ $t("refresh") }}
                <i class="glyphicon glyphicon-refresh"></i>
              </button>
              <span v-if="total">
                {{ $t("count.nodes.matched", [total]) }}
              </span>
              <span v-else-if="editModel.filters.length === 0">
                {{ $t("JobExec.property.nodeFilter.null.description") }}
              </span>

              <div :id="`matchednodes${rkey}`" class="clearfix">
                <node-list-embed :nodes="[]" />
              </div>
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
              v-model="editModel.jobref.nodeThreadcount"
              type="number"
              name="nodeThreadcount"
              min="1"
              size="3"
              class="form-control"
              :disabled="editModel.filters.length === 0"
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
                v-model="editModel.jobref.nodeKeepgoing"
                type="radio"
                name="nodeKeepgoing"
                :value="null"
                :disabled="editModel.filters.length === 0"
              />
              <label for="nodeKeepgoingNull">
                {{ $t("JobExec.property.nodeKeepgoing.null.description") }}
              </label>
            </div>

            <div class="radio">
              <input
                id="nodeKeepgoingTrue"
                v-model="editModel.jobref.nodeKeepgoing"
                type="radio"
                name="nodeKeepgoing"
                :value="true"
                :disabled="editModel.filters.length === 0"
              />
              <label for="nodeKeepgoingTrue">
                {{ $t("Workflow.property.keepgoing.true.description") }}
              </label>
            </div>

            <div class="radio">
              <input
                id="nodeKeepgoingFalse"
                v-model="editModel.jobref.nodeKeepgoing"
                type="radio"
                name="nodeKeepgoing"
                :value="false"
                :disabled="editModel.filters.length === 0"
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
              v-model="editModel.jobref.nodeRankAttribute"
              :disabled="editModel.filters.length === 0"
              type="text"
              name="nodeRankAttribute"
              class="form-control"
              :placeholder="
                $t('scheduledExecution.property.nodeRankAttribute.description')
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
                v-model="editModel.jobref.nodeRankOrderAscending"
                type="radio"
                name="nodeRankOrderAscending"
                :value="null"
                :disabled="editModel.filters.length === 0"
              />
              <label for="nodeRankOrderAscendingNull">
                {{ $t("JobExec.property.nodeRankOrder.null.description") }}
              </label>
            </div>
            <div class="radio">
              <input
                id="nodeRankOrderAscending"
                v-model="editModel.jobref.nodeRankOrderAscending"
                type="radio"
                name="nodeRankOrderAscending"
                :value="true"
                :disabled="editModel.filters.length === 0"
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
                v-model="editModel.jobref.nodeRankOrderAscending"
                type="radio"
                name="nodeRankOrderAscending"
                :value="false"
                :disabled="editModel.filters.length === 0"
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
      <section>
        <div class="form-group">
          <label class="col-sm-2 control-label">{{
            $t("JobExec.nodeStep.title")
          }}</label>
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
      <hr />
      <div class="form-group">
        <label class="col-sm-2 control-label" :for="`description${rkey}`">
          {{ $t("Workflow.stepLabel") }}
        </label>

        <div class="col-sm-10">
          <input
            :id="`description${rkey}`"
            v-model="editModel.description"
            type="text"
            name="description"
            class="form-control"
            :placeholder="$t('Workflow.step.property.description.placeholder')"
            size="100"
          />
        </div>
      </div>
    </div>
    <template #footer>
      <btn data-testid="cancel-button" @click="$emit('cancel')">
        {{ $t("Cancel") }}
      </btn>
      <btn type="success" data-testid="save-button" @click="saveChanges">
        {{ $t("Save") }}
      </btn>
    </template>
  </modal>
  <modal
    v-if="openJobSelectionModal"
    v-model="openJobSelectionModal"
    :title="$t('choose.a.job...')"
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
        projectToDisplay: editModel.jobref.project,
        key: editModel.jobref.project,
      }"
    />
  </modal>
</template>

<script lang="ts">
import { defineComponent } from "vue";
import { PluginConfig } from "@/library/interfaces/PluginConfig";
import UiSocket from "@/library/components/utils/UiSocket";
import { NodeFilterStore } from "@/library/stores/NodeFilterLocalstore";
import { getRundeckContext } from "@/library";
import NodeListEmbed from "@/app/components/job/resources/NodeListEmbed.vue";
import { merge } from "lodash";
// import NodeFilterInput from "@/app/components/job/resources/NodeFilterInput.vue";
const eventBus = getRundeckContext().eventBus;

export default defineComponent({
  name: "JobRefForm",
  components: { NodeListEmbed, UiSocket },
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
    modalActive: {
      type: Boolean,
      default: true,
    },
  },
  emits: ["update:modelValue", "update:modalActive", "save", "cancel"],
  data() {
    return {
      isUseName: false,
      showModal: this.modalActive,
      nodeFilterOverrideExpanded: false,
      projectStore: getRundeckContext().rootStore.projects,
      nodeFilterStore: new NodeFilterStore(),
      selectedProject: getRundeckContext().projectName,
      editModel: {
        description: "",
        filters: [],
        jobref: {
          name: "",
          group: "",
          args: "",
          nodeIntersect: null,
          nodeThreadcount: null,
          nodeStep: false,
          nodeRankOrderAscending: null,
          nodeRankAttribute: null,
          importOptions: false,
          ignoreNotifications: false,
          failOnDisable: false,
          childNodes: false,
          nodeKeepgoing: null,
          project: "",
          uuid: "",
        },
      },
      filterValue: null,
      loading: false,
      openJobSelectionModal: false,
      showFavoritesButton: true,
      rkey:
        "r_" + Math.floor(Math.random() * Math.floor(1024)).toString(16) + "_",
      total: null,
    };
  },
  watch: {
    modalActive(val) {
      this.showModal = val;
    },
    showModal(val) {
      this.$emit("update:modalActive", val);
    },
    modelValue(val) {
      this.editModel = merge(this.editModel, val);
    },
    "nodeFilterStore.filter": {
      handler(val) {
        this.editModel.filters = [val];
      },
    },
  },
  async mounted() {
    if (!this.projectStore.loaded) {
      await this.projectStore.load();
    }

    this.editModel = merge(this.editModel, this.modelValue);

    eventBus.on(`browser-job-item-selection`, this.updateJobSelection);
    eventBus.on(`browser-jobs-empty`, this.handleFavoritesButton);
  },
  methods: {
    async saveChanges() {
      this.$emit("update:modelValue", this.editModel);
      this.$emit("save");
    },
    openModal() {
      this.openJobSelectionModal = true;
      this.showFavoritesButton = true;
    },
    updateJobSelection(job: any) {
      this.editModel.jobref.uuid = job.id;
      this.editModel.jobref.name = job.jobName;
      this.editModel.jobref.group = job.groupPath;
      this.openJobSelectionModal = false;
    },
    handleFavoritesButton() {
      this.showFavoritesButton = false;
    },
  },
});
</script>

<style scoped lang="scss">
.checkbox,
.radio {
  display: flex;
  align-items: center;
  gap: 5px;

  &:first-child {
    margin-top: 10px;
  }
}
</style>
