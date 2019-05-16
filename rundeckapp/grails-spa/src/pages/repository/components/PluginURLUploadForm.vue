<template>
  <div>
    <div class="col-xs-12">
      <div class="input-group input-group-lg">
        <span class="input-group-addon">Plugin URL</span>
        <input
          style="border: 1px solid #d6d7d6;background: #fff; border-right:0; padding-left:1em;"
          type="text"
          class="form-control"
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
        method: "get",
        headers: {
          "x-rundeck-ajax": true
        },
        url: `${window._rundeck.rdBase}plugin/installPlugin?pluginUrl=${
          this.pluginUrl
        }`,
        withCredentials: true
      })
        .then(response => {
          console.log("SUCCESS!!", response);
          this.$store.dispatch("overlay/openOverlay");
        })
        .catch(function() {
          this.$store.dispatch("overlay/openOverlay");
          console.log("FAILURE!!");
        });
    }
  }
};
</script>
<style lang="scss" scoped>
</style>
