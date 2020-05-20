<template>
  <tr v-if="displayCard">
    <td>
      <div style="width: 60px;">
        <span v-if="provider.builtin">
          <i class="fa fa-briefcase" aria-hidden="true" v-tooltip.hover="`Built-In`"></i>
        </span>
        <span v-else>
          <i class="fa fa-file" aria-hidden="true" v-tooltip.hover="`Installed File`"></i>
        </span>
        <span class="info-icon" @click="openInfo" style="margin-left:1em;">
          <i class="fas fa-info-circle"></i>
        </span>
      </div>
    </td>
    <td>
      <div>
        <span v-if="provider.title">{{provider.title}}</span>
        <span v-else>{{provider.name}}</span>
      </div>
    </td>
    <td>{{provider.service | splitAtCapitalLetter}}</td>
    <td>
      <div v-if="provider.author">Author: {{provider.author}}</div>
    </td>
    <td>
      <div class="current-version-number">{{provider.pluginVersion}}</div>
    </td>
    <td>
      <button
        v-if="!provider.builtin"
        style="margin-bottom:1em;"
        class="btn btn-sm btn-block square-button"
        @click="handleUninstall(provider)"
      >Uninstall</button>
    </td>
    <!-- <td>
      <div class="plugin-description" v-html="provider.description"></div>
    </td>-->
  </tr>
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
    }
  }
};
</script>
<style lang="scss" scoped>
.btn.square-button {
  border-radius: 5px;
}
</style>
