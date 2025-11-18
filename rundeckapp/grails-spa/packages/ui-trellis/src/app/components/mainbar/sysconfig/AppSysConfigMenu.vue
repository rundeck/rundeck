<script lang="ts">
import { loadJsonData } from "../../../utilities/loadJsonData";
import { defineComponent } from "vue";
import SysConfigMenu from "./SysConfigMenu.vue";
import { Link } from "../types/menuTypes";

/**
 * AppSysConfigMenu - wraps SysConfigMenu and provides data from JSON script tag
 */
export default defineComponent({
  name: "AppSysConfigMenu",
  components: {
    SysConfigMenu,
  },
  data() {
    return {
      links: [] as Link[],
    };
  },
  mounted() {
    this.links = loadJsonData("sysConfigNavJSON") || [];
  },
});
</script>

<template>
  <sys-config-menu v-if="links && links.length > 0" :links="links" />
</template>

<style scoped lang="scss"></style>
