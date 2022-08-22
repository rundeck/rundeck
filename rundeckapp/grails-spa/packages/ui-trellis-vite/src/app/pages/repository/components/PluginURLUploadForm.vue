<template>
  <div>
    <div class="col-xs-12">
      <div class="input-group input-group-lg">
        <span class="input-group-addon">Plugin URL</span>
        <input
          type="text"
          class="form-control input-text"
          placeholder="https://someurl.com/some-path/some-plugin.jar"
          v-model="pluginURL"
        >
        <span class="input-group-btn">
          <a @click="submitUrl" class="btn btn-cta" type="button">Install</a>
        </span>
      </div>
    </div>
  </div>
</template>
<script>
import {client} from "@/library/modules/rundeckClient"
export default {
  name: "PluginUrlUploadForm",
  data() {
    return {
      pluginURL: ""
    };
  },
  methods: {
    submitUrl() {
      this.$store.dispatch("overlay/openOverlay", {
        loadingMessage: "Installing",
        loadingSpinner: true
      });
      client.sendRequest({
        baseUrl: window._rundeck.rdBase,
        pathTemplate: `/plugin/installPlugin`,
        queryParameters: {
          pluginUrl: this.pluginURL
        },
        method: 'POST'
      }).then(response => {
        if (response.status === 200) {
          this.$store.dispatch("overlay/openOverlay");
          if (response.parsedBody.err) {
            this.$alert({
              title: "Error Uploading",
              content: response.parsedBody.err
            });
          } else {
            this.$alert({
              title: "Plugin Installed",
              content: response.parsedBody.msg
            });
          }
        }else if (response.status >= 300) {
          this.$store.dispatch("overlay/openOverlay");
          let message = `Error: ${response.status}`
          if (response.parsedBody && response.parsedBody.message) {
            message = response.parsedBody.message
          }else if (response.parsedBody && response.parsedBody.error) {
            message = response.parsedBody.error
          }
          this.$alert({
            title: "Error Uploading",
            content: message
          });
        }
      });
    }
  }
};
</script>
<style lang="scss" scoped>
.input-text{
  padding: 0 var(--spacing-6);
}
</style>
