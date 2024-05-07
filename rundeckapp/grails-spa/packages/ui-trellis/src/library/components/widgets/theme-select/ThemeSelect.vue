<template>
  <div style="padding: 20px">
    <div class="form-group">
      <label>Theme</label>
      <select v-model="theme" class="form-control select">
        <option v-for="themeOpt in themes" :key="themeOpt">
          {{ themeOpt }}
        </option>
      </select>
    </div>
    <ui-socket location="after" section="theme-select" />
  </div>
</template>

<script lang="ts">
import UiSocket from "../../utils/UiSocket.vue";
import { defineComponent } from "vue";

export default defineComponent({
  name: "ThemeSelect",
  components: { UiSocket },
  data() {
    return {
      themes: ["system", "light", "dark"],
      theme: "",
      themeStore: window._rundeck.rootStore.theme,
    };
  },
  watch: {
    theme(newVal: any) {
      this.themeStore.setUserTheme(newVal);
    },
  },
  beforeMount() {
    this.theme = this.themeStore.userPreferences.theme!;
  },
});
</script>
