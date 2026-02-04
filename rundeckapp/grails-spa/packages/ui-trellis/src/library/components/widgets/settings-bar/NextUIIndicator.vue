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
import { loadJsonData } from "../../../../app/utilities/loadJsonData";

const COOKIE_NAME = "nextUi";

export default defineComponent({
  name: "NextUIIndicator",
  data() {
    const pageUiMeta = loadJsonData("pageUiMeta") || {};

    // Ensure we have boolean values
    const nextUiCapable =
      pageUiMeta.nextUiCapable === true || pageUiMeta.nextUiCapable === "true";

    // Check multiple sources for nextUiSystemEnabled
    let systemEnabled =
      pageUiMeta.nextUiSystemEnabled === true ||
      pageUiMeta.nextUiSystemEnabled === "true";
    const hasNextUiCookie = this.$cookies?.get(COOKIE_NAME) === "true";
    const isNextUiPage = pageUiMeta.uiType === "next";

    // If user has the cookie set or we're on a nextUi page, show the indicator
    if (hasNextUiCookie || isNextUiPage) {
      systemEnabled = true;
    }

    return {
      nextUiCapable,
      nextUiSystemEnabled: systemEnabled,
      nextUiEnabled: hasNextUiCookie,
    };
  },
  computed: {},
  mounted() {
    // Re-check pageUiMeta after mount in case data wasn't available during data()
    const pageUiMeta = loadJsonData("pageUiMeta");
    if (pageUiMeta) {
      this.nextUiCapable =
        pageUiMeta.nextUiCapable === true ||
        pageUiMeta.nextUiCapable === "true";

      // Check multiple sources for nextUiSystemEnabled
      let systemEnabled =
        pageUiMeta.nextUiSystemEnabled === true ||
        pageUiMeta.nextUiSystemEnabled === "true";
      const hasNextUiCookie = this.$cookies?.get(COOKIE_NAME) === "true";
      const isNextUiPage = pageUiMeta.uiType === "next";

      if (hasNextUiCookie || isNextUiPage) {
        systemEnabled = true;
      }

      this.nextUiSystemEnabled = systemEnabled;

      // Debug: log the values
      console.log("[NextUIIndicator] pageUiMeta:", pageUiMeta);
      console.log("[NextUIIndicator] hasNextUiCookie:", hasNextUiCookie);
      console.log("[NextUIIndicator] isNextUiPage:", isNextUiPage);
      console.log("[NextUIIndicator] nextUiCapable:", this.nextUiCapable);
      console.log(
        "[NextUIIndicator] nextUiSystemEnabled:",
        this.nextUiSystemEnabled,
      );
      console.log(
        "[NextUIIndicator] showNextUiIndicator:",
        this.showNextUiIndicator,
      );
    } else {
      console.warn("[NextUIIndicator] pageUiMeta not found");
    }
  },
  methods: {
    handleOpenModal() {
      const eventBus = (window._rundeck as any)?.eventBus;
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
  color: var(--font-color);
  cursor: pointer;
  font-size: 14px;

  &:hover {
    background-color: var(--background-color-accent-lvl2);
  }
}

.settings-bar__nextui-indicator {
  color: var(--colors-blue-600, #2563eb);
  font-size: 12px;
}

.settings-bar__nextui-text {
  white-space: nowrap;
}
</style>
