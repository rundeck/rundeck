<template>
  <div id="app" v-if="project">
    <!-- <motd v-if="project && project.readme && project.readme.motd" :project="project"></motd> -->
    <project-description v-if="project && project.description" :project="project"></project-description>
    <project-activity v-if="project" :project="project" :rdBase="rdBase"></project-activity>
    <project-readme v-if="project" :project="project"></project-readme>
    <!-- <pre>{{project}}</pre> -->
  </div>
</template>

<script>
import axios from 'axios'
// import motd from '@/components/motd'
import projectDescription from './components/description'
import projectReadme from './components/projectReadme'
import projectActivity from './components/activity'

export default {
  name: 'App',
  components: {
    // motd,
    projectDescription,
    projectReadme,
    projectActivity
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
