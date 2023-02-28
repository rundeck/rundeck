<template>
  <div v-if="displayCard" class="px-3">
    <div class="card w-full p-6">
      
      <div class="card-header">
        <div class="current-version-number">{{provider.pluginVersion}}</div>
        <div class="text-4xl font-bold">
          <span v-if="provider.title">{{provider.title}}</span>
          <span v-else>{{provider.name}}</span>
        </div>
      </div>
      <div class="card-content flex-grow">
        <div class="flexible">
          <button
            v-if="!provider.builtin"
            style="margin-bottom:1em;"
            class="btn btn-sm btn-danger square-button"
            @click="handleUninstall(provider)"
          >Uninstall</button>

          <div v-if="provider.author" style="margin-bottom:1em;">Author: {{provider.author}}</div>
          <div class="plugin-description">{{provider.description | shorten}}</div>
          <ul class="provides">
            <li>{{provider.service | splitAtCapitalLetter}}</li>
          </ul>
          <!-- <button class="btn btn-sm btn-block square-button" @click="openInfo">More Info</button> -->
        </div>
      </div>
      <div class="card-footer">
        <span class="provider-builtin-icon" v-if="provider.builtin" v-tooltip.hover="`Built-In`">
          <i class="fa fa-briefcase" aria-hidden="true"></i>
        </span>
        <span class="provider-builtin-icon" v-else v-tooltip.hover="`Installed File`">
          <i class="fa fa-file" aria-hidden="true"></i>
        </span>
        <span class="info-icon" @click="openInfo">
          <i class="fas fa-info-circle"></i>
        </span>
      </div>
    </div>
  </div>
</template>
<script>
import axios from "axios";
import { mapActions, mapState } from "vuex";

export default {
  name: "ProviderCard",
  props: ["provider"],
  methods: {
    ...mapActions("plugins", ["getProviderInfo", "uninstallPlugin"]),
    openInfo() {
      this.getProviderInfo({
        serviceName: this.provider.service,
        providerName: this.provider.name
      });
    },
    handleUninstall(provider) {
      this.uninstallPlugin(provider);
    }
  },
  computed: {
    ...mapState("plugins", ["selectedServiceFacet"]),
    displayCard() {
      if (
        this.selectedServiceFacet === null ||
        this.selectedServiceFacet === ""
      ) {
        return true;
      } else {
        return this.selectedServiceFacet === this.provider.service;
      }
    }
  },
  filters: {
    splitAtCapitalLetter: function(value) {
      if (!value) return "";
      value = value.toString();
      if (value.match(/^[A-Z]+$/g)) return value;
      return value.match(/[A-Z][a-z]+|[0-9]+/g).join(" ");
    },
    shorten: function(value) {
      if (value.length > 200) {
        return value.substr(0, 140) + "... click to read more";
      } else {
        return value;
      }
    }
  }
};
</script>
<style lang="scss" scoped>
.px-3{
  padding-left: 0.75rem;
  padding-right: 0.75rem; 
}
.p-6{
  padding: 1.8rem;
}
.w-full{
  width: 100%;
}
.thumbnail{
  background-size: contain;
  border: 1px solid #e2e8f0;
  background-repeat: no-repeat;
  background-position: center;
  padding-bottom: 95%;
}
.font-bold{
  font-weight: 600;
}
.links{
  display: flex;
  a{
    margin-right: 10px;
  }
}
.text-4xl{
  font-size: 2.25rem;
  line-height: 2.5rem;
}
</style>