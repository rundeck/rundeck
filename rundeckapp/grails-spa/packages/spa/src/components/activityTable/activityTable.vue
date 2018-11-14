<template>
  <div>HELLO WORLD 2</div>
</template>

<script>
import Trellis from '@rundeck/ui-trellis'
import {getRundeckContext, getSynchronizerToken, RundeckBrowser} from '@rundeck/ui-trellis'

export default {
  name: 'ActivityTable',
  data () {
    return {
      test: null,
      jobId: null,
      executionStatus: null,
      executionList: null,
      executionState: null

    }
  },
  mounted () {
    // console.log('mounted')
  },
  created () {
    // console.log('created', getRundeckContext, getSynchronizerToken, RundeckBrowser)
    let RundeckContext = getRundeckContext();
    // console.log('rundeck context', RundeckContext)
    let pathname = window.location.pathname.split('/')
    let jobId = pathname[pathname.length -1]
    // console.log('jobId', jobId)
    this.jobId = jobId
    RundeckContext.rundeckClient.executionStatusGet(jobId).then(executionResult => {
      this.executionStatus = executionResult
      RundeckContext.rundeckClient.jobExecutionList(executionResult.job.id).then(jobResult => {
        this.executionList = jobResult
        // console.log('executionResult', executionResult)
        // console.log('jobResult', jobResult)
        RundeckContext.rundeckClient.executionStateGet(jobId).then(executionStateGet => {
          this.executionState = executionStateGet
          // console.log('executionStateGet', executionStateGet)
        })
      })
    })
  }
}
</script>

<style lang="scss" scoped>
.motd-content {
  max-height: 200px;
  overflow-y: hidden;
  &.full {
    max-height: 100%;
    height: 100%;
    overflow: auto;
  }
}
</style>
