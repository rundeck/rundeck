<template>
  <div class="content">
    <div id="layoutBody">
      <div class="title">
        <span class="text-h3">
          <template v-if="filename === 'readme.md'">
            <i class="fas fa-file-alt"></i> {{ $t("edit.readme.label") }}
          </template>
          <template v-else>
            <i class="fas fa-comment-alt"></i> {{ $t("edit.motd.label") }}
          </template>
        </span>
      </div>
      <div class="container-fluid">
        <div class="row">
            <div class="col-xs-12">
              <div class="card" id="createform">
                <div class="card-header">
                  <h3 class="card-title">{{  $t("edit.file.project", [this.filename, this.project]) }}</h3>
                </div>
                <div class="card-content">
                  <div class="help-block">
                    <details class="details-reset more-info">
                      <summary>
                        <span v-html="$t('file.readme.help.markdown')"></span>
                        <span class="more-indicator-verbiage more-info-icon"><i class="glyphicon glyphicon-chevron-right "></i></span>
                        <span class="less-indicator-verbiage more-info-icon"><i class="glyphicon glyphicon-chevron-down "></i></span>
                      </summary>
                      <span v-html="$t('file.readme.help.html')"></span>
                    </details>
                  </div>
                  <ace-editor v-model="fileText"
                              :soft-wrap-control="true"
                              height="250"
                              width="100%"
                              lang="markdown"
                              :readOnly="false"
                  />
                </div>
                <div class="card-footer">
                  <button type="button" class="btn btn-default reset_page_confirm" @click="createProjectHomeLink">Cancel</button>
                  <button type="submit" class="btn btn-cta reset_page_confirm" @click="saveProjectFile">Save</button>
                  <template v-if="displayConfig.includes('none')">
                    <span class="text-warning text-right">
                      <template v-if="authAdmin">
                        {{ $t("file.warning.not.displayed.admin.message") }}
                        <a :href="createProjectConfigureLink"> {{ $t("project.configuration.label") }} </a>
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
import {defineComponent} from "vue";
import AceEditorVue from "@/library/components/utils/AceEditorVue.vue";
import AceEditor from "@/library/components/utils/AceEditor.vue";
import {getRundeckContext} from "@/library";
import {url} from  "@/library/rundeckService"
import {Notification} from "uiv";

const rundeckClient = getRundeckContext().rundeckClient
export default defineComponent({
  name: "EditProjectFile",
  components: {AceEditor, AceEditorVue},
  data() {
    return {
      fileText: '',
      markdownSectionOpen: false,
      errorMsg: ''
    };
  },
  props: {
    filename: '',
    displayConfig: [],
    project: '',
    authAdmin: false,
  },
  computed: {
    createProjectConfigureLink() {
      return url('project/' + this.project + '/configure').href
    },
  },
  mounted() {
    this.getFileText()
  },
  methods: {
    async saveProjectFile() {
      if(this.fileText === ''){
        this.fileText = '#This is a test'
      }
      const resp = await rundeckClient.sendRequest({
        baseUrl: `${getRundeckContext().rdBase}api/${getRundeckContext().apiVersion}`,
        pathTemplate: "/project/"+this.project+"/" + this.filename,
        method: "PUT",
        body: {
          contents: this.fileText
        }
      });
      if(resp.status === 200){
        this.notifySuccess("Success", "Saved Project File " + this.filename )
      }
      else{
        if(resp.status !== 200){
          this.errorMsg = resp.parsedBody.message
          this.notifyError(this.errorMsg)
        }
      }
    },
    createProjectHomeLink() {
      document.location = url('project/' + this.project + '/home').href
    },
    notifyError(msg) {
      Notification.notify({
        type: "danger",
        title: "An Error Occurred",
        content: msg,
        duration: 0
      });
    },

    notifySuccess(title, msg) {
      Notification.notify({
        type: "success",
        title: title,
        content: msg,
        duration: 5000
      });
    },
    getFileText(){
      rundeckClient.sendRequest({
        baseUrl: `${getRundeckContext().rdBase}api/${getRundeckContext().apiVersion}`,
        pathTemplate: "/project/"+this.project+"/" + this.filename,
        headers: {
          'Accept': 'application/json'
        },
        method: 'GET'
      }).then(response => {
        if(response.status === 200){
          this.fileText = response.parsedBody.contents
        }
        else{
          if(response.status !== 200){
            this.errorMsg = response.parsedBody.message
            this.notifyError(this.errorMsg)
          }
        }
      })
    }
  },
});
</script>