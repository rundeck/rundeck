<template>
  <SettingsModal
    :is-open="isModalOpen"
    :active-tab="activeTab"
    @close="handleCloseModal"
    @change-tab="handleChangeTab"
  />
</template>

<script lang="ts">
import { defineComponent } from "vue";
import SettingsModal from "./SettingsModal.vue";

export default defineComponent({
  name: "SettingsModalWrapper",
  components: {
    SettingsModal,
  },
  data() {
    return {
      isModalOpen: false,
      activeTab: "theme" as "theme" | "ui-early-access",
    };
  },
  mounted() {
    // Listen for modal open events from any button
    const eventBus = (window._rundeck as any)?.eventBus;
    if (eventBus) {
      eventBus.on("settings:open-modal", this.handleOpenModal);
    }
  },
  beforeUnmount() {
    const eventBus = (window._rundeck as any)?.eventBus;
    if (eventBus) {
      eventBus.off("settings:open-modal", this.handleOpenModal);
    }
  },
  methods: {
    handleOpenModal(tab: "theme" | "ui-early-access") {
      this.activeTab = tab;
      this.isModalOpen = true;
    },
    handleCloseModal() {
      this.isModalOpen = false;
    },
    handleChangeTab(tab: "theme" | "ui-early-access") {
      this.activeTab = tab;
    },
  },
});
</script>
