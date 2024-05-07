<template>
  <div v-show="overlay" class="loading">
    <div v-show="loadingSpinner" class="loading-spinner">
      <i class="fas fa-spinner fa-spin fa-5x"></i>
      <div
        v-show="loadingMessage"
        class="loading-text"
        v-html="loadingMessage"
      ></div>
    </div>
    <div v-if="errors && errors.length" v-show="errors" class="errors">
      <div>
        <a class="btn btn-default" @click="closeOverlay">Close</a>
      </div>
      <div class="error-list">
        <h3 v-html="errors.code"></h3>
        <p v-html="errors.msg"></p>
      </div>
      <!-- <ul class="error-list">
        <li v-for="(error, index) in errors" :key="index">{{error}}</li>
      </ul>-->
    </div>
  </div>
</template>

<script>
import { mapState, mapActions } from "vuex";

export default {
  name: "Overlay",
  computed: {
    ...mapState("overlay", [
      "overlay",
      "errors",
      "loadingMessage",
      "loadingSpinner",
    ]),
  },
  methods: {
    ...mapActions("overlay", ["closeOverlay"]),
  },
};
</script>
<style lang="scss" scoped>
.loading {
  position: fixed;
  height: 100%;
  background: #ffffffc9;
  width: 100%;
  z-index: 99;
  top: 0;
  left: 0;
  .loading-spinner {
    position: absolute;
    top: 50%;
    text-align: center;
    left: 40%;
    width: calc(100% - 70%);
    // position: absolute;
    // top: 50%;
    // left: 50%;
  }
  .loading-text {
    font-size: 16px;
    margin-top: 1em;
    font-weight: bold;
    // text-align: center;
    // margin-left: -20px;
  }
}
.errors {
  position: fixed;
  height: 100%;
  background: #ffffffc9;
  width: 100%;
  z-index: 99;
  top: 0;
  left: 0;
  .error-list {
    top: 20%;
    left: 20%;
    max-width: 800px;
    position: absolute;
    font-size: 24px;
  }
}
</style>
