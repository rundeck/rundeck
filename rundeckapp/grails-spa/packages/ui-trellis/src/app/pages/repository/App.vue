<template>
  <div id="app" class="container-fluid">
    <Overlay/>
    <router-view></router-view>
  </div>
</template>

<script>
import Overlay from "./components/Overlay.vue";
export default {
  name: "PluginApplication",
  components: {
    Overlay
  },
  computed: {},
  methods: {}
};
</script>
<style lang="scss">
</style>
