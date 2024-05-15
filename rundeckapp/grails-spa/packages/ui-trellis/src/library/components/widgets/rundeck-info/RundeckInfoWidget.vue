<template>
  <InfoDisplay
    :version="system.versionInfo"
    :latest="releases.releases[0]"
    :server="system.serverInfo"
    :app-info="system.appInfo"
    :hide-version="hideVersion"
  />
</template>

<script lang="ts">
import { RootStore } from "../../../stores/RootStore";
import { defineComponent, inject } from "vue";
import InfoDisplay from "./RundeckInfo.vue";
import { getFeatureEnabled } from "../../../services/featureConfig"

const HIDE_VERSION_CONFIG_KEY = "hideVersion";
export default defineComponent({
  name: "RundeckInfoWidget",
  components: {
    InfoDisplay,
  },
  data() {
      return {
          hideVersion: true
      }
  },
  setup() {
    const { system, releases } = inject("rootStore") as RootStore;
    return { system, releases };
  },
  async mounted() {
    try {
      await Promise.all([
        this.releases.load(),
        this.system.load(),
        this.loadConfigs(),
      ]);
    } catch (e) {}
  },
  methods: {
    async loadConfigs() {
      const val = await getFeatureEnabled(HIDE_VERSION_CONFIG_KEY);
      this.hideVersion = (val !== null) ? val : false;
    },
  },
});
</script>
