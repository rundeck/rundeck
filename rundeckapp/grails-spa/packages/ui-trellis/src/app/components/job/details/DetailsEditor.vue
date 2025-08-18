<template>
  <div id="tab_details" class="tab-pane active">
    <section v-if="internalData && loaded" class="section-space-lg">
      <div id="schedJobNameLabel" class="form-group">
        <div :class="{ 'has-error': hasErrors('jobName') }">
          <label for="schedJobName" class="required col-sm-2 control-label">
            {{ $t("scheduledExecution.jobName.label") }}
          </label>
          <div class="col-sm-5">
            <input
              id="schedJobName"
              v-model="internalData.jobName"
              type="text"
              class="form-control"
              @blur="onBlur('jobName')"
            />
          </div>
        </div>
        <div class="col-sm-5">
          <div class="input-group">
            <span v-if="hasErrors('groupPath')" class="input-group-addon">
              <i class="glyphicon glyphicon-warning-sign"></i>
            </span>

            <input
              id="schedJobGroup"
              v-model="internalData.groupPath"
              type="text"
              class="form-control"
              :placeholder="$t('scheduledExecution.groupPath.description')"
            />
            <span class="input-group-btn">
              <span
                id="groupChooseModalBtn"
                class="btn btn-default"
                data-toggle="modal"
                data-target="#groupChooseModal"
                :title="$t('job.edit.groupPath.choose.text')"
              >
                {{ $t("choose.action.label") }}
              </span>
            </span>
          </div>
        </div>
        <common-modal
          modal-id="groupChooseModal"
          title-code="job.edit.groupPath.choose.text"
        >
          <div id="groupChooseModalContent" />
        </common-modal>
      </div>
      <div
        class="form-group"
        :class="{ 'has-error': hasErrors('description') }"
      >
        <label for="description" class="col-sm-2 control-label">
          {{ $t("scheduledExecution.property.description.label") }}
        </label>
        <div class="col-sm-10">
          <ul class="nav nav-tabs">
            <li class="active">
              <a href="#desceditor" data-toggle="tab">Edit</a>
            </li>
            <li
              v-if="showPreviewTab"
              id="previewrunbook"
              @click="generatePreview"
            >
              <a href="#descpreview" data-toggle="tab">
                {{ $t("job.editor.preview.runbook") }}
              </a>
            </li>
          </ul>
          <div class="tab-content">
            <div id="desceditor" class="tab-pane active">
              <div class="ace_text">
                <ace-editor
                  v-model="internalData.description"
                  name="description"
                  lang="markdown"
                  :read-only="false"
                  height="170"
                  width="100%"
                />
              </div>
              <textarea
                name="description"
                cols="80"
                rows="3"
                class="hidden"
                :value="internalData.description"
              />
            </div>
            <div
              id="descpreview"
              class="tab-pane panel panel-default panel-tab-content"
            >
              <div
                v-if="preview"
                id="descpreview_content"
                class="panel-body"
                v-html="preview"
              ></div>
            </div>
          </div>
          <div v-if="hasErrors(internalData.description)">
            <i class="glyphicon glyphicon-warning-sign text-warning"></i>
          </div>
          <div class="help-block">
            <plugin-details
              v-if="allowHtml"
              :description="
                $t('scheduledExecution.property.description.description')
              "
              :allow-html="allowHtml"
              extended-css=""
              inline-description
            />
            <template v-else>
              {{
                $t("scheduledExecution.property.description.plain.description")
              }}
            </template>
          </div>
        </div>
      </div>
    </section>
    <ui-socket section="job-details" location="tags" />
  </div>
</template>

<script lang="ts">
import { defineComponent, PropType } from "vue";
import UiSocket from "@/library/components/utils/UiSocket.vue";

import AceEditor from "@/library/components/utils/AceEditor.vue";
import { JobDetailsData } from "./types/detailsType";
import CommonModal from "../../common/CommonModal.vue";
import { getRundeckContext } from "@/library";
import PluginDetails from "@/library/components/plugins/PluginDetails.vue";

export default defineComponent({
  name: "DetailsEditor",
  components: {
    PluginDetails,
    CommonModal,
    UiSocket,
    AceEditor,
  },
  props: {
    allowHtml: {
      type: Boolean,
      default: false,
    },
    modelValue: {
      type: Object as PropType<JobDetailsData>,
      required: true,
    },
  },
  emits: ["update:modelValue"],
  data() {
    return {
      internalData: {
        jobName: "",
        description: "",
        groupPath: "",
      },
      errors: [],
      preview: null,
      loaded: false,
      eventBus: getRundeckContext().eventBus,
    };
  },
  computed: {
    showPreviewTab() {
      return this.internalData.description?.includes("---");
    },
  },
  watch: {
    internalData: {
      deep: true,
      handler(newVal) {
        this.$emit("update:modelValue", newVal);
      },
    },
  },
  async mounted() {
    try {
      this.internalData = Object.assign(
        {
          jobName: "",
          description: "",
          groupPath: "",
        },
        this.modelValue,
      );
    } catch (e) {
      console.log(e);
    } finally {
      this.loaded = true;
    }
    this.eventBus.on("group-selected", this.updateGroup);
  },
  methods: {
    updateGroup(path: string) {
      this.internalData.groupPath = path;
    },
    onBlur(inputName: string) {
      if (
        this.internalData[inputName].length === 0 &&
        !this.errors.includes[inputName]
      ) {
        this.errors.push(inputName);
      } else {
        this.errors = this.errors.filter((name) => name !== inputName);
      }
    },
    hasErrors(value: string) {
      return this.errors?.includes(value);
    },
    generatePreview() {
      // remove the divider
      const massagedData = (this.internalData.description || "").replace(
        /^(.|[\r\n])*?(\r\n|\n)---(\r\n|\n)/,
        "",
      );
      // then add markdown
      // @ts-ignore
      window.markdeep.format(massagedData + "\n", true, (t) => {
        this.preview = t;
      });
    },
  },
});
</script>
