<template>
  <div v-if="project.readme.readmeHTML && showReadme" class="row row-space">
    <div class="col-sm-12">
      <div class="card">
        <div class="card-header">
          <h4 class="card-title">Readme</h4>
        </div>
        <div class="card-content">
          <span v-if="project" v-html="project.readme.readmeHTML"></span>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
export default {
  name: "Readme",
  props: ["project"],
  data() {
    return {
      showReadme: false,
    };
  },
  mounted() {
    this.showReadme = this.shouldShowReadme();
  },
  methods: {
    shouldShowReadme() {
      const display = this.project.readmeDisplay;
      if (display.indexOf("projectHome") >= 0) {
        return true;
      }
      return false;
    },
  },
};
</script>
