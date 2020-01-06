<template>
  <div v-if="displayCard">
    <div class="card result flex-col">
      <div class="card-header">
        <div class="current-version-number">{{provider.pluginVersion}}</div>
        <h3 class="card-title">
          <span v-if="provider.title">{{provider.title}}</span>
          <span v-else>{{provider.name}}</span>
        </h3>
      </div>
      <div class="card-content flex-grow">
        <div class="flexible">
          <button
            v-if="!provider.builtin"
            style="margin-bottom:1em;"
            class="btn btn-sm btn-block square-button"
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
.card.result {
  .card-header {
    background: #20201f;
    padding: 1em;
    border-radius: 7px 7px 0 0;
    .card-title {
      margin: 0;
      color: white;
      font-weight: bold;
      font-size: 1.4em;
      line-height: 1.1em;
    }
    .support-type {
      color: white;
      // font-weight: bold;
      margin: 0 0 0.25em;
      // text-transform: uppercase;
    }
    .current-version-number {
      font-size: 12px;
      color: white;
      margin-bottom: 5px;
    }
  }
  .card-content {
    padding: 1em;
    // min-height: 250px;
    .provides {
      list-style: none;
      margin: 2em 0 0;
      padding: 0;
      font-size: 12px;
      li {
        display: inline-block;
        margin-right: 1em;
        margin-bottom: 1em;
        background-color: #d8d8d8;
        padding: 6px 10px 5px;
        border-radius: 50px;
        color: #6e6e6e;
      }
    }
  }
  .card-footer {
    margin-bottom: 0;
    padding: 0 2em auto;
    border-radius: 0 0 7px 7px;
    .links {
      a {
        color: #20201f;
        text-decoration: none;
        margin-right: 0.6em;
      }
    }
    .provider-builtin-icon,
    .info-icon {
      i {
        font-size: 18px;
      }
    }
    .info-icon {
      float: right;
    }
  }
}
.btn.square-button {
  border-radius: 5px;
}
</style>
