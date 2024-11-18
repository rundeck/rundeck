<template>
  <InfoDisplay
    :version="system.versionInfo"
    :latest="releases.releases[0]"
    :server="system.serverInfo"
    :app-info="system.appInfo"
  />
</template>

<script lang="ts">
import {defineComponent, inject, PropType} from "vue";
import InfoDisplay from "./RundeckInfo.vue";
import {SystemStore} from "../../../stores/System.ts";
import {Releases} from "../../../stores/Releases.ts";

export default defineComponent({
  name: "RundeckInfoWidget",
  components: {
    InfoDisplay,
  },
  props: {
    system: {
      type: Object as PropType<SystemStore>,
      required: true,
    },
    releases: {
      type: Object as PropType<Releases>,
      required: true,
    },
  },
  async mounted() {
    try {
      await Promise.all([this.releases.load(), this.system.load()]);
    } catch (e) {}
  },
});
</script>
