<template>
  <div id="app">
    <h4>Jobs</h4>
    <ul>
      <li v-for="job in jobs" :key="job.id">
        name: {{job.name}}  <br>
        group: {{job.group}} <br>
        shortDescription: {{job.shortDescription}} <br>
        <button @click="runJob(job.id)">Run Job</button>
      </li>
    </ul>
    <modal v-model="openRunJobModal" title="Execute Job" size="lg">
      <div v-html="modalContent"></div>
    </modal>
  </div>
</template>

<script>
import axios from 'axios'
import _ from 'lodash'
import {getRundeckContext, getSynchronizerToken, RundeckBrowser} from '@rundeck/ui-trellis'

export default {
  name: 'ProjectJobs',
  components: {
  },
  data () {
    return {
      RundeckContext: null,
      openRunJobModal: false,
      modalContent: null,
      groups: [],
      jobs: []
    }
  },
  methods: {
    runJob (jobId) {
      this.openRunJobModal = true
      axios.get(appLinks.scheduledExecutionExecuteFragment, {
        params: {
          id: jobId
        }
      }).then(response => {
        console.log('hello', response)
        this.modalContent = response.data
      })
    }
  },
  mounted () {
    this.RundeckContext = getRundeckContext()
    if (this.RundeckContext.projectName) {
      this.RundeckContext.rundeckClient.jobList(this.RundeckContext.projectName).then(response => {
        _.each(response, (job) => {
          let shortDescription = job.description.split('---')[0];
          job.shortDescription = shortDescription
          this.jobs.push(job)
        })
      })
    }
  }
}
</script>

<style>
</style>
