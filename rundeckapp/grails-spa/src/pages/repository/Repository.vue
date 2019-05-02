<template>
  <div>
    <div class>
      <h2 class="repo-name">
        <span class="capital-case">{{repo.repositoryName}}</span> Repository
        <span class="badge">{{repo.results.length}}</span>
        <a class="pull-right btn btn-default" @click="toggleVisiblity">
          <span v-show="visible">Hide</span>
          <span v-show="!visible">Show</span>
        </a>
      </h2>
    </div>

    <div class="artifact-grid row row-flex row-flex-wrap" v-show="visible">
      <PluginCard
        :result="result"
        :repo="repo"
        class="artifact col-xs-12 col-sm-4"
        v-for="result in repo.results"
        :key="result.id"
      />
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
  },
  mounted() {
    console.log("mounted RepositoryRow");
  }
};
</script>

<style lang="scss" scoped>
.capital-case {
  text-transform: capitalize;
}
</style>
