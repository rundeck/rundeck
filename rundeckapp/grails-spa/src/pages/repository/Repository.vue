<template>
  <div class="repository">
    <div class="repo-header">
      <a style="display:block;" @click="toggleVisiblity">
        <h3 class="repo-title">{{repo.repositoryName}} Repository</h3>
        <div class="visibility-toggle pull-right">
          <i v-show="visible" class="fas fa-sort-up fa-2x" title="Hide"></i>
          <i v-show="!visible" class="fas fa-sort-down fa-2x" title="Show"></i>
        </div>
      </a>
    </div>
    <div v-show="visible">
      <div class="repo-meta">{{repo.results.length}} plugins in repo</div>
      <div class="artifact-grid row row-flex row-flex-wrap">
        <PluginCard
          :result="result"
          :repo="repo"
          class="artifact col-xs-12 col-sm-4"
          v-for="result in repo.results"
          :key="result.id"
        />
      </div>
    </div>
  </div>
</template>

<script>
import PluginCard from "./PluginCard";
import { mapState } from "vuex";

export default {
  name: "RepositoryRow",
  props: ["repo"],
  components: {
    PluginCard
  },
  computed: {
    ...mapState(["repositories"])
  },
  data() {
    return {
      visible: true
    };
  },
  watch: {},
  methods: {
    toggleVisiblity() {
      this.visible = !this.visible;
    }
  }
};
</script>

<style lang="scss" scoped>
.repository {
  margin-top: 2em;
}
.repo-header {
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

    // line-height: 35px;
    // font-size: 1.2em;
    // font-weight: bold;
    // .fa-sort-up:after {
    //   content: "Hide";
    //   font-size: 12px;
    // }
    // .fa-sort-down:after {
    //   content: "Show";
    //   font-size: 12px;
    // }
    i.fa-sort-up,
    i.fa-sort-down {
      line-height: inherit;
      // margin-top: 5px;
      // vertical-align: sub;
    }
    i.fa-sort-up {
      margin-top: 8px;
    }
    i.fa-sort-down {
      margin-top: -2px;
    }
  }
  .repo-title {
    margin: 0;
    display: inline-block;
    font-weight: bold;
    text-transform: uppercase;
  }
}
.repo-meta {
  padding-left: 1em;
  margin-bottom: 1em;
}
</style>
