<template>
  <div class="col-xs-12">
    <div class="support-filters row row-flex row-flex-wrap">
      <div class="col-md-2">
        <div class="flex-col">
          <span class="title">Support</span>
        </div>
      </div>
      <div class="col-md-10">
        <label>
          Community
          <input type="checkbox" value="Community" v-model="supportType">
        </label>

        <label>
          Rundeck Supported
          <input type="checkbox" value="Rundeck Supported" v-model="supportType">
        </label>

        <label>
          Enterprise Exclusive
          <input
            type="checkbox"
            value="Enterprise Exclusive"
            v-model="supportType"
          >
        </label>
      </div>
    </div>
  </div>
</template>

<script>
import { mapActions } from "vuex";

export default {
  name: "FacetFilters",

  data() {
    return {
      supportType: []
    };
  },
  watch: {
    supportType: function(newVal, oldVal) {
      this.setSupportTypeFilter(newVal);
    }
  },
  methods: {
    ...mapActions(["setSupportTypeFilter"])
  }
};
</script>
<style lang="scss" scoped>
.support-filters {
  background: black;
  color: white;
  padding: 2em 1em;
  font-size: 20px;
  .title {
    color: #cdcdcd;
    display: flex;
    // align-items: center;
    font-size: 1.8rem;
    padding: 1em 2em;
    text-transform: uppercase;
    letter-spacing: 3.44px;
  }
  label {
    border: 1px solid blue;
    padding: 1em 2em;
    input[type="checkbox"] {
      display: none;
    }
  }
  :checked + label {
    font-weight: bold;
    border: 1px solid red;
  }
}
</style>
