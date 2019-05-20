<template>
  <div>
    <div class="row">
      <div class="col-xs-12 col-sm-4">
        <h2 style="margin:0">Installed Plugins</h2>
      </div>
      <div class="col-xs-12 col-sm-4">
        <select class="form-control" @change="changeServiceFacet">
          <option value>View All</option>
          <option
            v-for="(service, index) in services"
            :key="index"
            :value="service.name"
          >{{service.value}}</option>
        </select>
      </div>
      <div class="col-xs-12 col-sm-4">
        <form @submit.prevent="search">
          <div class="input-group input-group-lg">
            <input
              type="text"
              class="form-control"
              placeholder="Search for..."
              v-model="searchString"
            >
            <span class="input-group-btn" v-if="searchResults.length > 0">
              <button @click="clearSearch" class="btn btn-default btn-fill" type="button">
                <i class="fas fa-times"></i>
              </button>
            </span>
            <span class="input-group-btn" v-else>
              <button @click="search" class="btn btn-default btn-fill" type="button">
                <i class="fas fa-search"></i>
              </button>
            </span>
          </div>
        </form>
      </div>
    </div>
    <div class="row">
      <div class="col-xs-12">
        <div v-if="searchResults.length" class="artifact-grid row row-flex row-flex-wrap">
          <ProviderCard
            :provider="plugin"
            class="artifact col-xs-12 col-sm-4"
            v-for="(plugin, index) in searchResults"
            :key="index"
          />
        </div>
        <div v-else class="artifact-grid row row-flex row-flex-wrap">
          <ProviderCard
            :provider="plugin"
            class="artifact col-xs-12 col-sm-4"
            v-for="(plugin, index) in plugins"
            :key="index"
          />
        </div>
      </div>
    </div>
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
const FuseSearchOptions = {
  shouldSort: true,
  threshold: 0.2,
  location: 0,
  // distance: 100,
  // maxPatternLength: 32,
  minMatchCharLength: 1,
  keys: ["display", "name"]
};

import axios from "axios";

import { mapState, mapActions, mapGetters } from "vuex";
import ProviderCard from "../components/Provider";
import ConfigureFrameworkString from "../components/ConfigureFrameworkString";

export default {
  name: "PluginConfigurationView",
  components: { ProviderCard, ConfigureFrameworkString },
  methods: {
    ...mapActions("plugins", ["initData", "setServiceFacet", "getServices"]),
    ...mapActions("modal", ["closeModal"]),
    handleModalClose() {
      this.closeModal();
    },
    changeServiceFacet(event) {
      this.setServiceFacet(event.target.value);
    },
    clearSearch() {
      this.searchResults = [];
    },
    search() {
      this.clearSearch();
      this.showWhichPlugins = null;
      if (this.searchString === "") {
        this.searchResults = [];
        return;
      }
      let theRepo = this.plugins;
      this.$search(this.searchString, theRepo, FuseSearchOptions).then(
        results => {
          this.searchResults = results;
        }
      );
    }
  },
  computed: {
    ...mapState("modal", ["modalOpen"]),
    ...mapState("plugins", ["plugins", "provider", "serviceName"]),
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
  created() {
    this.initData();
    this.getServices().then(result => {
      this.services = result;
    });
  },
  data() {
    return {
      showWhichPlugins: null,
      searchString: "",
      searchIndex: [],
      searchResults: [],
      selectedService: null,
      services: []
    };
  }
};
</script>

<style lang="scss" scoped>
.artifact {
  max-width: 33.33333333%;
}
</style>
<style lang="scss">
// Modal Styles
#provider-modal {
  .modal-dialog.modal-lg {
    width: 90%;
  }
  .provider-prop-divs > div {
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
}
</style>
