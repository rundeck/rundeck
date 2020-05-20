<template>
  <div v-if="project">
    <motd v-if="project && project.readme && project.readme.motd" :project="project"></motd>
  </div>
</template>

<script>
import axios from 'axios'
import motd from '@/components/motd/motd'

export default {
  name: 'App',
  components: {
    motd
  },
  data () {
    return {
      project: null
    }
  },
  mounted () {
    if (window._rundeck && window._rundeck.rdBase && window._rundeck.projectName) {
      axios({
        method: 'get',
        headers: {'x-rundeck-ajax': true},
        url: `${window._rundeck.rdBase}menu/homeAjax`,
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
