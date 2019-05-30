<template>
  <div v-if="displayCard">
    <div class="card result flex-col" @click="openInfo" style="cursor:pointer;">
      <div class="card-header">
        <h3 class="card-title">
          <span v-if="provider.builtin">
            <i class="fa fa-briefcase" aria-hidden="true" v-tooltip.hover="`Built-In`"></i>
          </span>
          <span v-else>
            <i class="fa fa-file" aria-hidden="true" v-tooltip.hover="`Installed File`"></i>
          </span>
          <span v-if="provider.title">{{provider.title}}</span>
          <span v-else>{{provider.name}}</span>
          <span class="current-version-number label label-default">{{provider.pluginVersion}}</span>
          <span class="pull-right">
            <ul class="provides">
              <li>{{provider.service | splitAtCapitalLetter}}</li>
            </ul>
          </span>
        </h3>
      </div>
      <div class="card-content flex-grow">
        <div class="flexible">
          <div v-if="provider.author">Author: {{provider.author}}</div>
          <div class="plugin-description" v-html="provider.description"></div>
        </div>
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
    ...mapActions("plugins", ["getProviderInfo"]),
    openInfo() {
      this.getProviderInfo({
        serviceName: this.provider.service,
        providerName: this.provider.name
      });
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
      return value.match(/[A-Z][a-z]+|[0-9]+/g).join(" ");
    }
  }
};
</script>
<style lang="scss" scoped>
.card.result {
  .card-header {
    background: #20201f;
    padding: 1em 2em;
    border-radius: 7px 7px 0 0;
    .card-title {
      margin: 0;
      color: white;
      font-weight: bold;
      font-size: 1.2em;
      i {
        font-size: 1.2em;
        margin-right: 1em;
      }
    }
    .support-type {
      color: white;
      // font-weight: bold;
      margin: 0 0 0.25em;
      // text-transform: uppercase;
    }
    .current-version-number {
      // position: absolute;
      // right: 1em;
      margin-left: 2em;
      padding: 0.2em 1em;
      font-size: 14px;
      border-radius: 20px;
    }
    .requires-rundeck-version {
      color: #f7403a;
      // text-transform: capitalize;
      // font-weight: bold;
      margin: 0.7em 0 0;
      height: 20px;
    }
    .provides {
      list-style: none;
      margin: 0;
      padding: 0;
      display: inline;
      li {
        display: inline-block;
        // margin-right: 1em;
        // margin-bottom: 1em;
        background-color: #d8d8d8;
        // padding: 6px 10px 5px;
        padding: 0.2em 1em;
        border-radius: 50px;
        color: #6e6e6e;
      }
    }
  }
  .card-content {
    padding: 1em 1em 1em 2em;
    // min-height: 250px;
  }
  .card-footer {
    margin-bottom: 1em;
    padding: 0 2em auto;
    border-radius: 0 0 7px 7px;
    .links {
      a {
        color: #20201f;
        text-decoration: none;
        margin-right: 0.6em;
      }
    }
    .btn {
      border-radius: 6px;
      font-weight: bold;
      padding: 5px 30px;
    }
    .button-group {
      text-align: right;
    }
  }
}
</style>
<style lang="scss">
.card.result .card-content p {
  font-size: 1.3em;
  line-height: 1.2em;
  // font-weight: bold;
}
</style>
