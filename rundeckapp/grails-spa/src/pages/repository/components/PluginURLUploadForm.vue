<template>
  <div>
    <div class="col-xs-12">
      <div class="input-group input-group-lg">
        <span class="input-group-addon" id="sizing-addon1">Plugin URL</span>
        <input
          style="border: 1px solid #d6d7d6;background: #fff; border-right:0; padding-left:1em;"
          type="text"
          class="form-control"
          aria-describedby="sizing-addon1"
        >
        <span class="input-group-btn">
          <button class="btn btn-default" type="button">Install</button>
        </span>
      </div>
    </div>
    <!--
    <form
      controller="plugin"
      action="installPlugin"
      id="installPlugin"
      useToken="true"
      class="form-horizontal"
      role="form"
    >
      <div class="form-group">
        <div class="col-sm-4">
          <label for="pluginUrl" class="input-sm">Plugin URL</label>
        </div>
        <div class="col-sm-8">
          <input type="submit" value="Install" class="pull-right">
          <input type="text" name="pluginUrl" id="pluginUrl" class="col-sm-8">
        </div>
      </div>
    </form>-->
  </div>
</template>
<script>
export default {
  name: "PluginUrlUploadForm",
  methods: {
    postURL() {
      this.$store.dispatch("overlay/openOverlay", {
        loadingMessage: "Installing",
        loadingSpinner: true
      });
      axios
        .post(`${window._rundeck.rdBase}plugin/installPlugin`, formData, {
          headers: {
            "Content-Type": "multipart/form-data"
          }
        })
        .then(response => {
          console.log("SUCCESS!!", response);
          this.$store.dispatch("overlay/openOverlay");
          this.$store.dispatch("modal/openModal", true);
        })
        .catch(function() {
          this.$store.dispatch("overlay/openOverlay");
          console.log("FAILURE!!");
        });
    },
    handleFilesUploads() {
      this.files = this.$refs.files.files;
    }
  }
};
</script>
<style lang="scss" scoped>
</style>
