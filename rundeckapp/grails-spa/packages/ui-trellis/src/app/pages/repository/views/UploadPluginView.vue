<template>
  <div class="card">
    <div class="card-content">
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
  </div>
</template>
<script>
import { defineComponent } from 'vue';
import { mapActions } from 'vuex';

import PluginUploadForm from "../components/PluginUploadForm.vue";
import PluginURLUploadForm from "../components/PluginURLUploadForm.vue";
export default defineComponent({
  name: "UploadPluginView",
  components: { PluginUploadForm, PluginURLUploadForm },
  methods: {
    ...mapActions('overlay', ['openOverlay']),
  },
  created() {
    this.openOverlay(false);
  }
})
</script>
<style lang="scss" scoped>
</style>
