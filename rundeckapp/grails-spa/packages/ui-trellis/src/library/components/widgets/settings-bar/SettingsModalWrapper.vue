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
import { getRundeckContext } from "../../../../library";
import SettingsModal from "./SettingsModal.vue";
import type { SettingsTab } from "./SettingsModalTypes";

export default defineComponent({
  name: "SettingsModalWrapper",
  components: {
    SettingsModal,
  },
  data() {
    return {
      isModalOpen: false,
      activeTab: "theme" as SettingsTab,
    };
  },
  computed: {
    eventBus() {
      return getRundeckContext()?.eventBus;
    },
  },
  mounted() {
    if (this.eventBus) {
      this.eventBus.on("settings:open-modal", this.handleOpenModal);
    }
  },
  beforeUnmount() {
    if (this.eventBus) {
      this.eventBus.off("settings:open-modal", this.handleOpenModal);
    }
  },
  methods: {
    handleOpenModal(tab: SettingsTab): void {
      this.activeTab = tab;
      this.isModalOpen = true;
    },
    handleCloseModal(): void {
      this.isModalOpen = false;
    },
    handleChangeTab(tab: SettingsTab): void {
      this.activeTab = tab;
    },
  },
});
</script>
