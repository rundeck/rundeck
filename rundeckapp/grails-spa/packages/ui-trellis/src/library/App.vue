<template>
  <div id="app">
    <copybox />
  </div>
</template>

<script lang="ts">
import { defineComponent } from "vue";
import copybox from "./components/containers/copybox/CopyBox.vue";

export default defineComponent({
  name: "App",
  components: {
    copybox,
  },
});
</script>

<!-- eslint-disable-next-line vue/enforce-style-attribute -- root/body-level selectors -->
<style lang="scss">
#app {
  font-family: Avenir, Helvetica, Arial, sans-serif;
  -webkit-font-smoothing: antialiased;
  -moz-osx-font-smoothing: grayscale;
  text-align: center;
  color: #2c3e50;
  margin-top: 60px;
}
</style>
