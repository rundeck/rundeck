<template>
  <div class="row" v-if="message">
    <div class="col-xs-12">
      <div class="alert alert-info">
        <button type="button" class="close" @click="dismissMessage">×</button>
        <h4>Message of The Day</h4>
        <div class="motd-content" v-bind:class="{ full: showFullMOTD}">
          <span v-html="message"></span>
        </div>
        <div style="margin-top:1em;">
          <a v-show="!showFullMOTD" class="btn btn-sm btn-default text-center" @click="showFullMOTD = !showFullMOTD">show more</a>
          <a v-show="showFullMOTD" class="btn btn-sm btn-default text-center" @click="showFullMOTD = !showFullMOTD">show less</a>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
import _ from 'lodash'

export default {
  name: 'MessageOfTheDay',
  props: [
    'project'
  ],
  data () {
    return {
      message: '',
      showFullMOTD: false
    }
  },
  methods: {
    dismissMessage () {
      let cookieKey = this.hashMessage(this.project.readme.motd)
      let midnight = new Date()
      midnight.setHours(23, 59, 59, 0)
      this.$cookies.set(cookieKey, 'true', midnight)
      this.message = false
    },
    hashMessage (messageToHash) {
      let hash = 0
      let chr
      if (messageToHash.length === 0) return hash
      for (let i = 0; i < messageToHash.length; i++) {
        chr = messageToHash.charCodeAt(i)
        hash = ((hash << 5) - hash) + chr
        hash |= 0 // Convert to 32bit integer
      }
      return hash
    },
    checkMessage () {
      let messageToCheck = this.hashMessage(this.project.readme.motd)
      if (this.$cookies.get(messageToCheck)) {
        // the message has been previously dismissed
        return false
      } else {
        return true
      }
    }
  },
  mounted () {
    let message = this.checkMessage()

    if (message) {
      this.message =  this.project.readme.motdHTML
    }
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
