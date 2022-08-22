<template>
  <div id="app" v-if="project">
    <slot :project="project"></slot>
    <activity-summary v-if="eventsAuth && project  && showSummary!=='false'" :project="project" :rdBase="rdBase" ></activity-summary>
    <project-readme v-if="project && showReadme!=='false' " :project="project"></project-readme>
    <!-- <activity-list v-if="project" :project="project" :rdBase="rdBase" :eventBus="eventBus"></activity-list> -->
  </div>
</template>

<script>
import projectDescription from '@/app/pages/project-dashboard/components/description.vue'
import projectReadme from './components/projectReadme.vue'
import activitySummary from './components/activitySummary.vue'
import activityList from '../../components/activity/activityList.vue'

import {
  getRundeckContext
} from "@/library/rundeckService"
import RundeckContext from '@/library/centralService'

export default {
  name: 'App',
  props:['eventBus', 'showDescription','showReadme','showSummary','showActivity'],
  components: {
    // motd,
    projectDescription,
    projectReadme,
    activitySummary,
    activityList
  },
  data () {
    return {
      project: null,
      rdBase: null,
      eventsAuth:false
    }
  },
  async mounted () {
    if (window._rundeck && window._rundeck.rdBase && window._rundeck.projectName) {
      this.rdBase = window._rundeck.rdBase
      this.eventsAuth=window._rundeck.data && window._rundeck.data.projectEventsAuth
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
