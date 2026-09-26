<template>
  <tr v-if="displayCard">
    <td>
      <div style="width: 60px">
        <span v-if="provider.builtin">
          <i
            v-tooltip.hover="`Built-In`"
            class="fa fa-briefcase"
            aria-hidden="true"
          ></i>
        </span>
        <span v-else>
          <i
            v-tooltip.hover="`Installed File`"
            class="fa fa-file"
            aria-hidden="true"
          ></i>
        </span>
        <span class="info-icon" style="margin-left: 1em" @click="openInfo">
          <i class="fas fa-info-circle"></i>
        </span>
      </div>
    </td>
    <td>
      <div>
        <span v-if="provider.title">{{ provider.title }}</span>
        <span v-else>{{ provider.name }}</span>
      </div>
    </td>
    <td>{{ StringFormatters.splitAtCapitalLetter(provider.service) }}</td>
    <td>
      <div v-if="provider.author">Author: {{ provider.author }}</div>
    </td>
    <td>
      <div class="current-version-number">{{ provider.pluginVersion }}</div>
    </td>
    <td>
      <button
        v-if="!provider.builtin"
        style="margin-bottom: 1em"
        class="btn btn-sm btn-block square-button"
        @click="handleUninstall(provider)"
      >
        Uninstall
      </button>
    </td>
    <!-- <td>
      <div class="plugin-description" v-html="provider.description"></div>
    </td>-->
  </tr>
</template>
<script>
import axios from "axios";
import { mapActions, mapState } from "vuex";
import * as StringFormatters from "../../../utilities/StringFormatters";

export default {
  name: "ProviderCard",
  props: ["provider"],
  methods: {
    ...mapActions("plugins", ["getProviderInfo", "uninstallPlugin"]),
    openInfo() {
      this.getProviderInfo({
        serviceName: this.provider.service,
        providerName: this.provider.name,
      });
    },
    handleUninstall(provider) {
      this.uninstallPlugin(provider);
    },
  },
  computed: {
    StringFormatters() {
      return StringFormatters;
    },
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
    },
  },
};
</script>
<style lang="scss" scoped>
.btn.square-button {
  border-radius: 5px;
}
</style>
