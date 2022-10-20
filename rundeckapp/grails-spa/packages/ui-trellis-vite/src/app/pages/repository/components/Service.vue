<template>
  <div class="service">
    <div class="service-header">
      <a style="display:block;" @click="toggleVisiblity">
        <h3 class="service-title">{{service.service}}</h3>
        <div class="visibility-toggle pull-right">
          <i v-show="visible" class="fas fa-sort-up fa-2x" title="Hide"></i>
          <i v-show="!visible" class="fas fa-sort-down fa-2x" title="Show"></i>
        </div>
      </a>
    </div>
    <div v-show="visible">
      <div class="service-meta">{{service.desc}}</div>
      <div class="row row-flex row-flex-wrap">
        <div v-for="(provider, index) in service.providers" :key="index">
          <ProviderCard :provider="provider" :service-name="service.service" class/>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
import ProviderCard from "../components/Provider";
import { mapState } from "vuex";

export default {
  name: "ServiceRow",
  props: ["service"],
  components: {
    ProviderCard
  },
  data() {
    return {
      visible: true
    };
  },
  methods: {
    toggleVisiblity() {
      this.visible = !this.visible;
    }
  }
};
</script>

<style lang="scss" scoped>
.service {
  margin-top: 2em;
}

.service-header {
  background: #e5e2de;
  color: #5d5d5d;
  padding: 1em 2em;
  margin-bottom: 1em;
  border-radius: 6px;
  display: block;
  cursor: pointer;
  a {
    color: #5d5d5d;
  }
  .visibility-toggle {
    line-height: 35px;
    display: inline-block;
    i.fa-sort-up,
    i.fa-sort-down {
      line-height: inherit;
    }
    i.fa-sort-up {
      margin-top: 8px;
    }
    i.fa-sort-down {
      margin-top: -2px;
    }
  }
  .service-title {
    margin: 0;
    display: inline-block;
    font-weight: bold;
    text-transform: uppercase;
  }
}
.service-meta {
  padding-left: 1em;
  margin-bottom: 1em;
}
</style>
