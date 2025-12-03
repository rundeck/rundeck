<template>
  <div v-if="project" id="app">
    <activity-list
      v-if="project"
      :project="project"
      :rd-base="rdBase"
      :event-bus="eventBus"
    ></activity-list>
  </div>
</template>

<script>
import activityList from "../../components/activity/activityList";

import { getRundeckContext, RundeckContext } from "../../../library";
import axios from "axios";

export default {
  name: "App",
  components: {
    // motd,
    activityList,
  },
  props: ["eventBus"],
  data() {
    return {
      project: null,
      rdBase: null,
    };
  },
  async mounted() {
    if (
      window._rundeck &&
      window._rundeck.rdBase &&
      window._rundeck.projectName
    ) {
      this.rdBase = window._rundeck.rdBase;
      const response = await axios.get(`${this.rdBase}menu/homeAjax`, {
        params: {
          projects: window._rundeck.projectName,
        },
        headers: {
          "X-Rundeck-ajax": "true",
          Accept: "application/json",
        },
      });
      if (response.data.projects) {
        this.project = response.data.projects[0];
      }
    }
  },
};
</script>

<style></style>
