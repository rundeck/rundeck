<template>
  <div class="container-fluid">
    <div class="row">
      <div class="col-xs-12">
        <div class="alert alert-danger">
          <button type="button" class="close">×</button>
          <span>
            <b>Danger -</b> This is a regular notification made with ".alert-danger"
          </span>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
import axios from "axios";
// import motd from '@/components/motd/motd'

export default {
  name: "VersionNotification",
  components: {
    // motd
  },
  data() {
    return {
      // project: null
    };
  },
  mounted() {
    console.log("monday");
    axios({
      method: "get",
      url: `https://api.github.com/repos/rundeck/rundeck/releases`,
      params: {
        // projects: `${window._rundeck.projectName}`
      }
    }).then(
      response => {
        console.log("response", response);
      },
      error => {
        console.log("error", error);
      }
    );
  }
};
</script>

<style>
</style>
