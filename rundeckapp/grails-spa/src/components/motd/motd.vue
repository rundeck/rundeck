<template>
  <div class="row" v-if="message && showMessage">
    <div class="col-xs-12">
      <div :class="'alert '+alertStyle">
        <button type="button" class="close" @click="dismissMessage" data-dismiss="alert">×</button>
        <h4 v-if="noTitle">Message of The Day</h4>
        <div class="motd-content" v-bind:class="{ full: showFullMOTD}">
          <span v-html="message"></span>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
// import _ from 'lodash'

export default {
  name: 'MessageOfTheDay',
  props: [
    'project'
  ],
  data () {
    return {
      message: '',
      showFullMOTD: true,
      showMessage: false,
    }
  },
  methods: {
    dismissMessage () {
      let cookieKey = this.hashMessage(this.project.readme.motd)
      let midnight = new Date()
      midnight.setHours(23, 59, 59, 0)
      this.$cookies.set(cookieKey, 'true', midnight)
      this.showMessage = false
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
      return !this.$cookies.get(messageToCheck)
    }
  },
  computed: {
    /**
     * Return true if the html does not start with a <h1/2/3/4/5> tag
     * @returns {boolean}
     */
    noTitle () {
      return this.message.indexOf("<article class=\"markdown-body\"><h") < 0
    },
    /**
     * Return 'alert-*' variant if the motd text contains a html comment starting with <!-- style:variant
     * for the danger, warning, primary, info, success styles
     * @returns {string}
     */
    alertStyle () {
      let style = ['danger', 'warning', 'primary', 'info', 'success']
              .find((val) => this.project.readme.motd.indexOf('<!-- style:' + val) >= 0)
      if (style) {
        return `alert-${style}`
      }
      return 'alert-default'
    }
  },
  mounted () {
    this.showMessage = this.checkMessage()

    this.message = this.project.readme.motdHTML
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
