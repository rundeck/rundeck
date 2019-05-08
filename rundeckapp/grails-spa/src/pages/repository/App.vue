<template>
  <div id="app">
    <Overlay/>
    <router-view></router-view>
  </div>
</template>

<script>
import Overlay from "./components/Overlay";

export default {
  name: "PluginApplication",
  components: {
    Overlay
  }
};
</script>
