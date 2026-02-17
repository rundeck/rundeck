<template>
  <button
    class="settings-bar__button"
    :title="$t('settings.theme.title')"
    @click="handleOpenModal"
  >
    <i class="fas fa-cog"></i>
  </button>
</template>

<script lang="ts">
import { defineComponent } from "vue";
import { getRundeckContext } from "../../../../library";

export default defineComponent({
  name: "SettingsCogButton",
  methods: {
    handleOpenModal(): void {
      const eventBus = getRundeckContext()?.eventBus;
      if (eventBus) {
        eventBus.emit("settings:open-modal", "theme");
      }
    },
  },
});
</script>

<style scoped lang="scss">
.settings-bar__button {
  display: flex;
  align-items: center;
  gap: var(--spacing-1, 4px);
  padding: var(--spacing-1, 4px) var(--spacing-2, 8px);
  background: transparent;
  border: none;
  color: var(--font-color);
  cursor: pointer;
  font-size: 14px;

  &:hover {
    background-color: var(--background-color-accent-lvl2);
  }

  i {
    font-size: 14px;
  }
}
</style>
