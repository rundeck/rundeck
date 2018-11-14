<template>
  <div class="row" v-if="message">
    <div class="col-xs-12">
      <div class="alert alert-info">
        <button type="button" class="close" @click="dismissMessage">×</button>
        <h4>Message of The Day</h4>
        <div v-show="showMessageTeaser">
          <span v-html="message.shortMessage"></span>
          <a v-show="message.fullMessage" class="btn btn-sm btn-simple" style="margin: 0; padding: 0 0 0 .5em;" @click="showMessageTeaser = !showMessageTeaser">[read more]</a>
        </div>

        <div v-show="!showMessageTeaser">
          <span v-html="message.fullMessage"></span>
          <p class="text-center">
            <a class="btn btn-sm btn-simple text-center" @click="showMessageTeaser = !showMessageTeaser">[show less]</a>
          </p>
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
      message: false,
      showMessageTeaser: true
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
      this.showMessageTeaser = true
      this.message = {
        shortMessage: _.truncate(this.project.readme.motd, {
          'length': 250
        }),
        fullMessage: (this.project.readme.motd.length <= 250 ? false : this.project.readme.motdHTML)
      }
    }
  }
}
</script>

<style scoped>

</style>
