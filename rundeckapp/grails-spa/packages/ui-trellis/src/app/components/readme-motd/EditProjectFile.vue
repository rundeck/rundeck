<template>
  <div class="content">
<!--    <div id="layoutBody">-->
<!--      <div class="title">-->
<!--        <span class="text-h3">-->
<!--          <template v-if="filename === 'readme.md'">-->
<!--            <i class="fas fa-file-alt"></i> {{ $t("edit.readme.label") }}-->
<!--          </template>-->
<!--          <template v-else>-->
<!--            <i class="fas fa-comment-alt"></i> {{ $t("edit.readme.label") }}-->
<!--          </template>-->
<!--        </span>-->
<!--      </div>-->
<!--      <div class="container-fluid">-->
<!--        <div class="row">-->
<!--            <div class="col-xs-12">-->
<!--              <div class="card" id="createform">-->
<!--                <div class="card-header">-->
<!--                  <h3 class="card-title">{{ `Edit ${filename} for ${project}` }}</h3>-->
<!--                </div>-->
<!--                <div class="card-content">-->
<!--                  <div class="help-block">-->
<!--                    <details class="details-reset more-info">-->
<!--                      <summary>-->
<!--                        {{ markdownSummary }}-->
<!--                        &lt;!&ndash; figure out how to add icons in vue &ndash;&gt;-->
<!--                      </summary>-->
<!--                      &lt;!&ndash; Markdown content here &ndash;&gt;-->
<!--                    </details>-->
<!--                  </div>-->
<!--                  <ace-editor :model-value="fileText"></ace-editor>-->
<!--                </div>-->
<!--                <div class="card-footer">-->
<!--                  <button type="button" class="btn btn-default reset_page_confirm" @click="cancel">Cancel</button>-->
<!--                  <button type="submit" class="btn btn-cta reset_page_confirm" @click="saveProjectFile">Save</button>-->
<!--                  &lt;!&ndash; Conditional Rendering for Warning Message &ndash;&gt;-->
<!--                  <template v-if="displayConfig.includes('none')">-->
<!--                    <span class="text-warning text-right">-->
<!--                      <div v-if="authAdmin">-->
<!--                        &lt;!&ndash; Admin message &ndash;&gt;-->
<!--                      </div>-->
<!--                      <div v-else>-->
<!--                        &lt;!&ndash; Non-admin message &ndash;&gt;-->
<!--                      </div>-->
<!--                    </span>-->
<!--                  </template>-->
<!--                </div>-->
<!--              </div>-->
<!--            </div>-->
<!--        </div>-->
<!--      </div>-->
<!--    </div>-->
    {{ "This is a test" }}
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
    console.log("never made it into new component")
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