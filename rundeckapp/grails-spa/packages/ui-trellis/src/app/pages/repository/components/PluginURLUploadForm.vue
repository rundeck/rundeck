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
import axios from "axios";
import { getRundeckContext } from "../../../../library";
import Tokens from "../../../../library/modules/tokens";

export default defineComponent({
  name: "PluginUrlUploadForm",
  data() {
    return {
      pluginURL: "",
    };
  },
  methods: {
    ...mapActions("overlay", ["openOverlay"]),
    async submitUrl() {
      this.openOverlay({
        loadingMessage: "Installing",
        loadingSpinner: true,
      });
      const ctx = getRundeckContext();
      const token = await Tokens.getUIAjaxTokens();
      try {
        const response = await axios.post(
          `${ctx.rdBase}plugin/installPlugin`,
          {},
          {
            params: {
              pluginUrl: this.pluginURL,
            },
            headers: {
              "X-Rundeck-ajax": "true",
              "X-RUNDECK-TOKEN-KEY": token.TOKEN,
              "X-RUNDECK-TOKEN-URI": token.URI,
            },
          },
        );
        await Tokens.setNewUIToken(response.headers);
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
      } catch (error) {
        this.openOverlay({});
        let message = "An unknown error occurred.";
        if (error.response && error.response.data && error.response.data.message) {
          message = error.response.data.message;
        } else if (error.message) {
          message = error.message;
        }
        this.$alert({
          title: "Error Uploading",
          content: message,
        });
      }
    },
  },
});
</script>
<style lang="scss" scoped>
.input-text {
  padding: 0 var(--spacing-6);
}
</style>
