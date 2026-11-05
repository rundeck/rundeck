<template>
  <span class="copyright">
    {{ copyright }}
  </span>
</template>

<script>
import { defineComponent } from "vue";

export default defineComponent({
  name: "RdCopyright",
  data() {
    return {
      copyright: process.env.VUE_APP_BUILD_COPYRIGHT,
    };
  },
});
</script>

<style scoped lang="scss">
.copyright {
  color: var(--font-color);
}
</style>
