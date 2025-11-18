<script lang="ts">
import { loadJsonData } from "../../../utilities/loadJsonData";
import { defineComponent } from "vue";
import { Link } from "../../../../library/components/mainbar/types/menuTypes";
import MainbarMenu from "../../../..//library/components/mainbar/MainbarMenu.vue";

/**
 * AppSysConfigMenu - wraps MainbarMenu and provides data from JSON script tag
 */
export default defineComponent({
  name: "AppSysConfigMenu",
  components: {
    MainbarMenu,
  },
  data() {
    return {
      links: [] as Link[],
    };
  },
  computed: {
    header() {
      return "System";
    },
  },
  mounted() {
    this.links = loadJsonData("sysConfigNavJSON") || [];
  },
});
</script>

<template>
  <mainbar-menu
    v-if="links && links.length > 0"
    :links="links"
    icon-css="fas fa-cog fa-lg"
    :header="header"
  />
</template>

<style scoped lang="scss"></style>
