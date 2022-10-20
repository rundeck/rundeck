<template>
  <div id="webhooks-app" style="height: 100%">
    <webhooks-view />
  </div>
</template>
<script>
import WebhooksView from "./views/WebhooksView.vue";

export default {
  name: "WebhooksApplication",
  components: {WebhooksView},
  computed: {},
  methods: {}
};
</script>
<style lang="scss">
</style>
