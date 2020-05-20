<template>
  <div>
    <h2 style="margin-bottom:2em;">Plugin Install</h2>
    <div class="row">
      <PluginUploadForm/>
    </div>

    <div style="margin:3em 0;">
      <div style="text-align: center;font-size: 3em; font-style: italic;">OR</div>
    </div>
    <div class="row">
      <PluginURLUploadForm/>
    </div>
  </div>
</template>
<script>
import PluginUploadForm from "../components/PluginUploadForm";
import PluginURLUploadForm from "../components/PluginURLUploadForm";
export default {
  name: "UploadPluginView",
  components: { PluginUploadForm, PluginURLUploadForm },
  created() {
    this.$store.dispatch("overlay/openOverlay", false);
  }
};
</script>
<style lang="scss" scoped>
</style>
