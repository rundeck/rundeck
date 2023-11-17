<template>
  <div class="content">
    <div id="layoutBody">
      <div class="title">
        <span class="text-h3">
          <template v-if="filename === 'readme.md'">
            <i class="fas fa-file-alt"></i> {{ $t("edit.readme.label") }}
          </template>
          <template v-else>
            <i class="fas fa-comment-alt"></i> {{ $t("edit.readme.label") }}
          </template>
        </span>
      </div>
      <div class="container-fluid">
        <div class="row">
            <div class="col-xs-12">
              <div class="card" id="createform">
                <div class="card-header">
                  <h3 class="card-title">{{ `Edit ${filename} for ${project}` }}</h3>
                </div>
                <div class="card-content">
                  <div class="help-block">
                    <details class="details-reset more-info">
                      <summary>
                        {{ $t("file.readme.help.markdown") }}
                        <span class="more-indicator-verbiage more-info-icon"><i class="glyphicon glyphicon-chevron-right" /></span>
                        <span class="more-indicator-verbiage more-info-icon"><i class="glyphicon glyphicon-chevron-down" /></span>
                      </summary>
                    </details>
                  </div>
                  <ace-editor v-model="fileText"
                              height="200"
                              width="100%"
                              lang="md"
                              :readOnly="false"
                  />
                </div>
                <div class="card-footer">
                  <button type="button" class="btn btn-default reset_page_confirm" @click="cancel">Cancel</button>
                  <button type="submit" class="btn btn-cta reset_page_confirm" @click="saveProjectFile">Save</button>
                  <template v-if="displayConfig.includes('none')">
                    <span class="text-warning text-right">
                      <div v-if="authAdmin">
                        <!-- Admin message -->
                      </div>
                      <div v-else>
                        <!-- Non-admin message -->
                      </div>
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
import {defineComponent} from "vue";
import AceEditorVue from "@/library/components/utils/AceEditorVue.vue";
import AceEditor from "@/library/components/utils/AceEditor.vue";

export default defineComponent({
  name: "EditProjectFile",
  components: {AceEditor, AceEditorVue},
  data() {
    return {
      editReadmeMessage: 'Your Edit Readme Message',
      editMessageOfTheDay: 'Your Edit Message of the Day',
      fileText: '', // pull value for fileText from API
    };
  },
  props: {
    filename: '',
    displayConfig: [],
    project: '',
    authAdmin: false,
  },
  computed: {
    projectFileEditMessage() {
      return `Edit ${this.filename}`;
    },
    markdownSummary() {
      // Return the markdown summary based on the filename -- not exactly sure what this is yet
    }
  },
  mounted() {
    console.log("made it into new component")
  },
  methods: {
    saveProjectFile() {
      // Implement form submission logic using API
    },
    cancel() {
      // Implement cancel logic
    },
    getFileText(){
      // Implement logic to get the project file text using the API
    }
  },
});
</script>