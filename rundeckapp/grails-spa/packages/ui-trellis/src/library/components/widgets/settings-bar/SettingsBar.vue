<template>
  <div class="settings-bar">
    <NextUIIndicator />
    <SettingsCogButton />
    <SettingsModalWrapper />
  </div>
</template>

<script lang="ts">
import { defineComponent } from "vue";
import NextUIIndicator from "./NextUIIndicator.vue";
import SettingsCogButton from "./SettingsCogButton.vue";
import SettingsModalWrapper from "./SettingsModalWrapper.vue";

export default defineComponent({
  name: "SettingsBar",
  components: {
    NextUIIndicator,
    SettingsCogButton,
    SettingsModalWrapper,
  },
});
</script>

<style scoped lang="scss">
.settings-bar {
  display: flex;
  align-items: center;
  gap: var(--spacing-2, 8px);
}
</style>
