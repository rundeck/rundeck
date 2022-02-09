<template>
  <div id="webhooks-app" style="height: 100%">
    <span class="badge-page-title"><i class="fas fa-plug"></i> Webhooks</span>
    <webhooks-view />
  </div>
</template>
<script>
import WebhooksView from "./views/WebhooksView";

export default {
  name: "WebhooksApplication",
  components: {WebhooksView},
  computed: {},
  methods: {}
};
</script>
<style lang="scss">
</style>
