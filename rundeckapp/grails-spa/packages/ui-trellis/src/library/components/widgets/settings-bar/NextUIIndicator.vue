<template>
  <button
    v-if="nextUiCapable"
    class="settings-bar__button settings-bar__nextui-indicator"
    :title="$t('settings.nextUi.indicatorTitle')"
    @click="handleOpenModal"
  >
    <span class="settings-bar__nextui-text">
      {{
        isNextUiPage
          ? $t("settings.nextUi.enabled")
          : $t("settings.nextUi.available")
      }}
    </span>
  </button>
</template>

<script lang="ts">
import { defineComponent } from "vue";
import { getRundeckContext } from "../../../../library";
import { getPageUiMeta } from "./services/pageUiMetaService";
import type { PageUiMeta } from "./NextUIIndicatorTypes";

export default defineComponent({
  name: "NextUIIndicator",
  data(): PageUiMeta {
    return {
      nextUiCapable: false,
      isNextUiPage: false,
    };
  },
  mounted() {
    const pageUiMeta = getPageUiMeta();
    this.nextUiCapable = pageUiMeta.nextUiCapable;
    this.isNextUiPage = pageUiMeta.isNextUiPage;
  },
  methods: {
    handleOpenModal(): void {
      const eventBus = getRundeckContext()?.eventBus;
      if (eventBus) {
        eventBus.emit("settings:open-modal", "ui-early-access");
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
  color: var(--grey-900);
  cursor: pointer;
  font-size: 14px;
  font-weight: 700;

  &:hover {
    background-color: var(--background-color-accent-lvl2);
  }
}

*[data-color-theme="dark"] .settings-bar__button {
  color: var(--font-color);
}

.settings-bar__nextui-text {
  white-space: nowrap;
}
</style>
