<template>
  <div id="app">
    HELLO JESSE
  </div>
</template>

<script>
import axios from 'axios'
import {getRundeckContext, getSynchronizerToken, RundeckBrowser} from '@rundeck/ui-trellis'

export default {
  name: 'App',
  components: {
  },
  data () {
    return {

    }
  },
  mounted () {
    console.log('RundeckBrowser', RundeckBrowser.RundeckClient)
  }
}
</script>

<style>
</style>
