<template>
  <button
    v-if="nextUiCapable"
    class="settings-bar__button settings-bar__nextui-indicator"
    :title="$t('settings.nextUi.indicatorTitle')"
    @click="handleOpenModal"
  >
    <span class="settings-bar__nextui-text">
      {{
        nextUiEnabled
          ? $t("settings.nextUi.enabled")
          : $t("settings.nextUi.available")
      }}
    </span>
  </button>
</template>

<script lang="ts">
import { defineComponent } from "vue";
import { getRundeckContext } from "../../../../library";
import {
  getPageUiMeta,
  getNextUiCapable,
  getNextUiSystemEnabled,
  hasNextUiCookie,
  isNextUiPage,
} from "./services/pageUiMetaService";
import type { NextUIIndicatorData } from "./NextUIIndicatorTypes";

export default defineComponent({
  name: "NextUIIndicator",
  data(): NextUIIndicatorData {
    const pageUiMeta = getPageUiMeta();
    const cookieValue = hasNextUiCookie(this.$cookies);
    const isNextPage = isNextUiPage(pageUiMeta);

    return {
      nextUiCapable: getNextUiCapable(pageUiMeta),
      nextUiSystemEnabled: getNextUiSystemEnabled(
        pageUiMeta,
        cookieValue,
        isNextPage,
      ),
      nextUiEnabled: cookieValue,
    };
  },
  mounted() {
    const pageUiMeta = getPageUiMeta();
    if (pageUiMeta) {
      const cookieValue = hasNextUiCookie(this.$cookies);
      const isNextPage = isNextUiPage(pageUiMeta);

      this.nextUiCapable = getNextUiCapable(pageUiMeta);
      this.nextUiSystemEnabled = getNextUiSystemEnabled(
        pageUiMeta,
        cookieValue,
        isNextPage,
      );
      this.nextUiEnabled = cookieValue;
    }
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

.settings-bar__nextui-text {
  white-space: nowrap;
}
</style>
