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
      <div class="repo-meta">
        <span
          v-if="type === 'search'"
        >{{repo.results.length}} {{ repo.results.length | pluralize('plugin')}} found in this repo that match the search term</span>
        <span v-else>{{repo.results.length}} {{ repo.results.length | pluralize('plugin')}} in repo</span>
      </div>
      <div class="artifact-grid row row-flex row-flex-wrap " :class="repo.repositoryName">
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
import PluginCard from "./PluginCard.vue";
import { mapState } from "vuex";

export default {
  name: "RepositoryRow",
  props: ["repo", "type"],
  components: {
    PluginCard
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
// .artifact-grid.official {
//   min-height: 100px;
//   max-height: 800px;
//   overflow-y: hidden;
//   &:after {
//     position: absolute;
//     z-index: 1;
//     bottom: 0;
//     left: 0;
//     pointer-events: none;
//     background-image: linear-gradient(
//       to bottom,
//       rgba(244, 243, 239, 0),
//       #f4f3ef 90%
//     );
//     width: 100%;
//     height: 4em;
//   }
// }

.artifact-grid {
  padding: 0 0.5em 0 0.5em;
}

.repo-header {
  background: #e5e2de;
  color: #5d5d5d;
  padding: 0.5em;
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
  .repo-title {
    margin: 0;
    display: inline-block;
    font-weight: bold;
    text-transform: uppercase;
  }
}
.repo-meta {
  // padding-left: 1em;
  margin-bottom: 1em;
}
</style>
