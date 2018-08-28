<template>
  <div>HELLO WORLD</div>
</template>

<script>
import axios from 'axios'

export default {
  name: 'App',
  components: {
  },
  data () {
    return {
    }
  },
  mounted () {
    // if (window._rundeck && window._rundeck.rdBase && window._rundeck.projectName) {
    //   axios({
    //     method: 'get',
    //     headers: {'x-rundeck-ajax': true},
    //     url: `${window._rundeck.rdBase}menu/homeAjax`,
    //     params: {
    //       projects: `${window._rundeck.projectName}`
    //     },
    //     withCredentials: true
    //   }).then((response) => {
    //     console.log('response', response)
    //     if (response.data.projects[0]) {
    //       this.project = response.data.projects[0]
    //     }
    //   })
    // }
  }
}
</script>

<style>
</style>
