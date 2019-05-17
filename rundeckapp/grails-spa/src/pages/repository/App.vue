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
      id="provider-modal"
      size="lg"
    >
      <p>
        Provider Name:
        <code>{{provider.name}}</code>
      </p>
      <p>{{provider.desc}}</p>
      <ul class="provider-props">
        <li v-for="(prop, index) in provider.props" :key="index">
          <div class="row">
            <div class="col-xs-12 col-sm-3">
              <strong>{{prop.title}}</strong>
            </div>
            <div class="col-xs-12 col-sm-9 provider-prop-divs">
              <div>{{prop.desc}}</div>
              <div>
                <strong>Configure Project:</strong>
                <code>project.plugin.{{serviceName}}.{{provider.name}}.{{prop.name}}={{prop.defaultValue}}</code>
              </div>
              <div>
                <strong>Configure Framework:</strong>
                <ConfigureFrameworkString
                  :serviceName="serviceName"
                  :provider="provider"
                  :prop="prop"
                />
              </div>
              <div class="row">
                <div class="col-xs-12 col-sm-3" v-if="prop.defaultValue">
                  <strong>Default value:</strong>
                  <code>{{prop.defaultValue}}</code>
                </div>
                <div class="col-xs-12 col-sm-9" v-if="prop.allowed && prop.allowed.length">
                  <strong>Allowed values:</strong>
                  <ul class="values">
                    <li v-for="(allowedItem, index) in prop.allowed" :key="index">
                      <code>{{allowedItem}}</code>
                    </li>
                  </ul>
                </div>
              </div>
            </div>
          </div>
        </li>
      </ul>
      <div slot="footer">
        <btn @click="handleModalClose">Close</btn>
      </div>
    </modal>
  </div>
</template>

<script>
import Overlay from "./components/Overlay";
import ConfigureFrameworkString from "./components/ConfigureFrameworkString";
import { mapActions, mapState } from "vuex";
export default {
  name: "PluginApplication",
  components: {
    Overlay,
    ConfigureFrameworkString
  },
  computed: {
    ...mapState("modal", ["modalOpen"]),
    ...mapState("plugins", ["provider", "serviceName"]),
    isModalOpen: {
      get: function() {
        return this.modalOpen;
      },
      set: function() {
        this.closeModal().then(() => {
          return this.modalOpen;
        });
      }
    }
  },
  methods: {
    ...mapActions("modal", ["closeModal"]),
    handleModalClose() {
      this.closeModal();
    }
  }
};
</script>
<style lang="scss">
// Modal Styles
#provider-modal .modal-dialog.modal-lg {
  width: 90%;
}
#provider-modal .provider-prop-divs > div {
  margin-bottom: 1em;
}
.values,
.provider-props {
  list-style: none;
  display: inline;
}
.provider-props > li {
  margin-top: 1em;
  border-top: 1px solid #ebebeb;
  padding-top: 1em;
}
.values li {
  display: inline;
  margin-right: 1em;
  // &:after {
  //   content: ",";
  // }
}
</style>
