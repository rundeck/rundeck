<template>
  <div>
    <h1>HELLO WORLD</h1>
    <div v-for="(service, index) in services" :key="index">
      <ServiceRow :service="service"/>
    </div>
  </div>
</template>

<script>
import axios from "axios";
import { mapState, mapActions } from "vuex";
import ServiceRow from "../components/Service";
export default {
  name: "PluginConfigurationView",
  components: {
    ServiceRow
  },
  methods: {
    ...mapActions("plugins", ["initData"])
  },
  computed: {
    ...mapState("plugins", ["services"])
  },
  mounted() {
    this.initData();
  }
};
</script>

<style lang="scss" scoped>
</style>
