<template>
  <div>
    <div class="col-xs-12">
      <div class="input-group input-group-lg">
        <span class="input-group-addon">Plugin URL</span>
        <input
          style="border: 1px solid #d6d7d6;background: #fff; border-right:0; padding-left:1em;"
          type="text"
          class="form-control"
          placeholder="https://someurl.com/some-path/some-plugin.jar"
          v-model="pluginURL"
        >
        <span class="input-group-btn">
          <a @click="submitUrl" class="btn btn-default" type="button">Install</a>
        </span>
      </div>
    </div>
  </div>
</template>
<script>
import axios from "axios";
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
      axios({
        method: "post",
        headers: {
          "x-rundeck-ajax": true
        },
        url: `${window._rundeck.rdBase}plugin/installPlugin?pluginUrl=${
          this.pluginURL
        }`,
        withCredentials: true
      }).then(response => {
        this.$store.dispatch("overlay/openOverlay");
        if (response.data.err) {
          this.$alert({
            title: "Error Uploading",
            content: response.data.err
          });
        } else {
          this.$alert({
            title: "Plugin Installed",
            content: response.data.msg
          });
        }
      });
    }
  }
};
</script>
<style lang="scss" scoped>
</style>
