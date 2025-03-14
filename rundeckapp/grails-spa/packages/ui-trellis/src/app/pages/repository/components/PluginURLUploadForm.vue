<template>
  <div>
    <div class="col-xs-12">
      <div class="input-group input-group-lg">
        <span class="input-group-addon">Plugin URL</span>
        <input
          v-model="pluginURL"
          type="text"
          class="form-control input-text"
          placeholder="https://someurl.com/some-path/some-plugin.jar"
        />
        <span class="input-group-btn">
          <a class="btn btn-cta" type="button" @click="submitUrl">Install</a>
        </span>
      </div>
    </div>
  </div>
</template>
<script>
import { defineComponent } from "vue";
import { mapActions } from "vuex";

import { api } from "../../../../library/services/api";
export default defineComponent({
  name: "PluginUrlUploadForm",
  data() {
    return {
      pluginURL: "",
    };
  },
  methods: {
    ...mapActions("overlay", ["openOverlay"]),
    submitUrl() {
      this.openOverlay({
        loadingMessage: "Installing",
        loadingSpinner: true,
      });
      api
        .post(`/plugin/installPlugin`, null, {
          params: { pluginUrl: this.pluginURL },
        })
        .then((response) => {
          if (response.status === 200) {
            this.openOverlay({});
            if (response.data.err) {
              this.$alert({
                title: "Error Uploading",
                content: response.data.err,
              });
            } else {
              this.$alert({
                title: "Plugin Installed",
                content: response.data.msg,
              });
            }
          } else if (response.status >= 300) {
            this.openOverlay({});
            let message = `Error: ${response.status}`;
            if (response.data && response.data.message) {
              message = response.data.message;
            } else if (response.data && response.data.error) {
              message = response.data.error;
            }
            this.$alert({
              title: "Error Uploading",
              content: message,
            });
          }
        });
    },
  },
});
</script>
<style lang="scss" scoped>
.input-text {
  padding: 0 var(--spacing-6);
}
</style>
