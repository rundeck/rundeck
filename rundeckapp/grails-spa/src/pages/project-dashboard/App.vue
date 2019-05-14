<template>
  <div id="app" v-if="project">
    <project-readme v-if="project" :project="project"></project-readme>
    <project-description v-if="project && project.description" :project="project"></project-description>
    <!-- <activity-summary v-if="project" :project="project" :rdBase="rdBase" :eventBus="eventBus"></activity-summary> -->
    <activity-list v-if="project" :project="project" :rdBase="rdBase" :eventBus="eventBus"></activity-list>
  </div>
</template>

<script>
import axios from 'axios'
import projectDescription from './components/description'
import projectReadme from './components/projectReadme'
import activitySummary from './components/activitySummary'
import activityList from '../../components/activity/activityList'

export default {
  name: 'App',
  props:['eventBus'],
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
      rdBase: null
    }
  },
  mounted () {
    if (window._rundeck && window._rundeck.rdBase && window._rundeck.projectName) {
      this.rdBase = window._rundeck.rdBase
      axios({
        method: 'get',
        headers: {'x-rundeck-ajax': true},
        url: `${this.rdBase}menu/homeAjax`,
        params: {
          projects: `${window._rundeck.projectName}`
        },
        withCredentials: true
      }).then((response) => {
        if (response.data.projects[0]) {
          this.project = response.data.projects[0]
        }
      })
    }
  }
}
</script>

<style>
</style>
