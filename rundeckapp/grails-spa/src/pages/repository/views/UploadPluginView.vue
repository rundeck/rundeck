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
    <span>form.groovyNote</span>
    <modal v-model="isModalOpen" @hide="handleModalClose" ref="modal" size="lg">
      <p>Somethign soemthing seomtthing</p>
      <div slot="footer">
        <btn @click="handleModalClose">Close</btn>
      </div>
    </modal>
  </div>
</template>
<script>
import PluginUploadForm from "../components/PluginUploadForm";
import PluginURLUploadForm from "../components/PluginURLUploadForm";
import { mapState, mapActions } from "vuex";
import axios from "axios";
export default {
  name: "UploadPluginView",
  components: { PluginUploadForm, PluginURLUploadForm },
  created() {
    this.$store.dispatch("overlay/openOverlay", false);
  },
  methods: {
    ...mapActions("modal", ["closeModal"]),
    handleModalClose() {
      this.closeModal();
    }
  },
  computed: {
    ...mapState("modal", ["modalOpen"]),
    isModalOpen: {
      get: function() {
        return this.modalOpen;
      },
      set: function() {
        this.closeModal().then(() => {
          return this.modalOpen;
        });
      }
    }
  }
};
</script>
<style lang="scss" scoped>
</style>
