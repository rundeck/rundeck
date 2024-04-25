<template>
  <div class="content">
    <div id="layoutBody">
      <div class="title">
        <span class="text-h3" data-test-id="title">
          <template v-if="filename === 'readme.md'">
            <i class="fas fa-file-alt"></i>
            {{ $t("edit.readme.label") }}
          </template>

          <template v-else>
            <i class="fas fa-comment-alt"></i> {{ $t("edit.motd.label") }}
          </template>
        </span>
      </div>
      <div class="container-fluid">
        <div class="row">
          <div class="col-xs-12">
            <div id="createform" class="card">
              <div class="card-header">
                <h3 class="card-title">
                  {{ $t("edit.file.project", [filename, project]) }}
                </h3>
              </div>
              <div class="card-content">
                <div class="help-block">
                  <details class="details-reset more-info">
                    <summary>
                      <span v-html="$t('file.readme.help.markdown')"></span>
                      <span class="more-indicator-verbiage more-info-icon"
                        ><i class="glyphicon glyphicon-chevron-right"></i
                      ></span>
                      <span class="less-indicator-verbiage more-info-icon"
                        ><i class="glyphicon glyphicon-chevron-down"></i
                      ></span>
                    </summary>
                    <span v-html="$t('file.readme.help.html')"></span>
                    <a
                      href="https://en.wikipedia.org/wiki/Markdown"
                      target="_blank"
                      rel="noopener noreferrer"
                    >
                      {{ $t("file.readme.help.linkText") }}
                    </a>
                  </details>
                </div>
                <ace-editor
                  v-if="authAdmin"
                  v-model="fileText"
                  :soft-wrap-control="true"
                  height="250"
                  width="100%"
                  lang="markdown"
                  :read-only="false"
                  data-test-id="ace-editor"
                />
              </div>
              <div class="card-footer">
                <button
                  type="button"
                  class="btn btn-default reset_page_confirm"
                  @click="createProjectHomeLink"
                  data-test-id="cancel"
                >
                  Cancel
                </button>
                <button
                  v-if="authAdmin"
                  type="submit"
                  class="btn btn-cta reset_page_confirm"
                  @click="saveProjectFile"
                  data-test-id="save"
                >
                  Save
                </button>
                <template v-if="displayConfig.includes('none')">
                  <span class="text-warning text-right">
                    <template v-if="authAdmin">
                      {{ $t("file.warning.not.displayed.admin.message") }}
                      <a :href="createProjectConfigureLink">
                        {{ $t("project.configuration.label") }}
                      </a>
                    </template>
                    <template v-else>
                      {{ $t("file.warning.not.displayed.nonadmin.message") }}
                    </template>
                  </span>
                </template>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
import { defineComponent } from "vue";
import AceEditorVue from "@/library/components/utils/AceEditorVue.vue";
import AceEditor from "@/library/components/utils/AceEditor.vue";
import { getRundeckContext } from "@/library";
import { url } from "@/library/rundeckService";
import { Notification } from "uiv";
import {
  saveProjectFile,
  getFileText,
} from "@/app/components/readme-motd/editProjectFileService";

const rundeckClient = getRundeckContext().rundeckClient;
export default defineComponent({
  name: "EditProjectFile",
  components: { AceEditor, AceEditorVue },
  props: {
    filename: {
      type: String,
      default: "",
    },
    displayConfig: {
      type: Array,
      default: [],
    },
    project: {
      type: String,
      default: "",
    },
    authAdmin: {
      type: Boolean,
      default: false,
    },
  },
  data() {
    return {
      fileText: "",
      markdownSectionOpen: false,
      errorMsg: "",
    };
  },
  computed: {
    createProjectConfigureLink() {
      return url("project/" + this.project + "/configure").href;
    },
  },
  mounted() {
    this.getFileText();
    this.originalFileText = this.fileText;
  },
  methods: {
    async saveProjectFile() {
      try {
        const resp = await saveProjectFile(
          this.project,
          this.filename,
          this.fileText,
        );
        if (resp.success) {
          this.notifySuccess("Success", "Saved Project File " + this.filename);
        }
      } catch (e) {
        this.notifyError(e.message);
      }
    },
    createProjectHomeLink() {
      document.location = url("project/" + this.project + "/home").href;
    },
    notifyError(msg) {
      Notification.notify({
        type: "danger",
        title: "An Error Occurred",
        content: msg,
        duration: 0,
      });
    },

    notifySuccess(title, msg) {
      Notification.notify({
        type: "success",
        title: title,
        content: msg,
        duration: 5000,
      });
    },
    notifyWarning(message) {
      Notification.notify({
        type: "info",
        title: "Warning",
        content: message,
        duration: 0,
      });
    },
    async getFileText() {
      try {
        const response = await getFileText(this.project, this.filename);

        if (response.success) {
          this.fileText = response.contents;
        }
      } catch (e) {
        if (e.warning) {
          this.notifyWarning(
            "The file " +
              this.filename +
              " does not exist in project " +
              this.project +
              " yet. Please save to create it.",
          );
        } else {
          this.notifyError(e.message);
        }
      }
    },
  },
});
</script>
