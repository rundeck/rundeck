<template>
  <div id="app" v-if="project">
    <activity-list v-if="project" :project="project" :rdBase="rdBase" :eventBus="eventBus"></activity-list>

  </div>
</template>

<script>
import activityList from '../../components/activity/activityList'

import {
  getRundeckContext,
  RundeckContext
} from "@rundeck/ui-trellis"

export default {
  name: 'App',
  props:['eventBus'],
  components: {
    // motd,
    activityList
  },
  data () {
    return {
      project: null,
      rdBase: null
    }
  },
  async mounted () {
    if (window._rundeck && window._rundeck.rdBase && window._rundeck.projectName) {
      this.rdBase = window._rundeck.rdBase
      const response = await getRundeckContext().rundeckClient.sendRequest({
        method: 'get',
        pathTemplate:"/menu/homeAjax",
        baseUrl: this.rdBase,
        queryParameters: {
          projects: window._rundeck.projectName
        }
      })
      if (response.parsedBody.projects) {
        this.project = response.parsedBody.projects[0]
      }

    }
  }
}
</script>

<style>
</style>
