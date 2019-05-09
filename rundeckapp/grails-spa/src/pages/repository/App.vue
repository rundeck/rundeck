<template>
  <div id="app">
    <Overlay/>
    <router-view></router-view>
    <modal
      v-if="provider"
      v-model="isModalOpen"
      :title="provider.title"
      @hide="handleModalClose"
      ref="modal"
      id="modal-demo"
      size="lg"
    >
      <p>
        Provider Name:
        <code>{{provider.name}}</code>
      </p>
      <p>{{provider.desc}}</p>
      <ul>
        <li v-for="(prop, index) in provider.props" :key="index">
          <dt>{{prop.title}}</dt>
          <dd>
            <p>{{prop.desc}}</p>
            <p>
              configure project:
              <code>project.plugin.{{serviceName}}.{{provider.name}}.{{prop.name}}={{prop.defaultValue}}</code>
            </p>
            <p></p>
          </dd>
          <dt>Milk</dt>
          <dd>White cold drink</dd>
        </li>
      </ul>
    </modal>
  </div>
</template>

<script>
import Overlay from "./components/Overlay";
import { mapActions, mapState } from "vuex";
export default {
  name: "PluginApplication",
  components: {
    Overlay
  },
  computed: {
    ...mapState("modal", ["isModalOpen"]),
    ...mapState("plugins", ["provider", "serviceName"])
  },
  methods: {
    ...mapActions("modal", ["closeModal"]),
    handleModalClose() {
      this.closeModal();
    }
  }
};
</script>
