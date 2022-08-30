<template>
  <Drawer
    v-if="message && showMessage"
    placement="right"
    :closeable="false"
    :visible="message && showMessage"
    width="50%"
    height="auto"
    @close="dismissMessage">

    <div :class="alertStyle" :style="styleCss" style="padding: 20px;">
      <button type="button" class="btn btn-default btn-link btn-close" @click="dismissMessage">Close</button>
      <div class="text-h4" v-if="noTitle">Message of The Day</div>
      <div class="motd-content" v-bind:class="{ full: showFullMOTD}">
        <span v-html="message"></span>
      </div>
    </div>
  </Drawer>
</template>

<script>
import axios from 'axios'
// import _ from 'lodash'

import Drawer from '@/library/components/containers/drawer/Drawer.vue'

export default {
  name: 'MessageOfTheDay',
  components: {Drawer},
  props: [
    'eventBus',
    'tabPage'
  ],
  data () {
    return {
      project:null,
      message: '',
      showFullMOTD: true,
      hasNewMessage:false,
      showMessage: false,
      pageDisplayConfigVal:{
        home:'projectList',
        projectHome:'projectHome'
      }
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
      if (!messageToHash || messageToHash.length === 0) return hash
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
    },
    checkPage(){
      const display = this.project.motdDisplay
      if(display.indexOf(this.pageDisplayConfigVal[this.tabPage])>=0){
        return true
      }
      return false
    }
  },
  computed: {
    /**
     * Return true if the html does not start with a <h1/2/3/4/5> tag
     * @returns {boolean}
     */
    noTitle () {
      if(!this.message){
        return true
      }
      return this.message.indexOf("<article class=\"markdown-body\"><h") < 0
    },
    /**
     * Return true if the html does not start with a <h1/2/3/4/5> tag
     * @returns {boolean}
     */
    motdTitle () {
      if(!this.message){
        return null
      }
      const index= this.message.indexOf("<article class=\"markdown-body\"><h")
      if(index>=0 ){
        const match=this.message.match(/<article class="markdown-body"><h(\d)>([^<]+)<\/h\1>/)
        if(match && match.length>2){
          return match[2]
        }
        return null
      }
    },
    // /**
    //  * Return 'style' variant if the motd text contains a html comment starting with <!-- style:variant
    //  * for the danger, warning, primary, info, success styles
    //  * @returns {string}
    //  */
    motdStyle () {
      if(!this.project.readme.motd){
        return ''
      }
      let style = ['danger', 'warning', 'primary', 'info', 'success']
              .find((val) => this.project.readme.motd.indexOf('<!-- style:' + val) >= 0)
      if (style) {
        return style
      }
      return 'default'
    },
    // /**
    //  * Return 'alert-*' variant if the motd text contains a html comment starting with <!-- style:variant
    //  * for the danger, warning, primary, info, success styles
    //  * @returns {string}
    //  */
    alertStyle () {
      return `bg-${this.motdStyle}`
    },
    styleCss () {
      let style = {}
    //   if (!this.project.readme.motd) {
    //     return style
    //   }
    //   const keys={fgcolor:'color',bgcolor:'backgroundColor'}
    //   let regex = /<!--\s+(fgcolor|bgcolor):(#[a-fA-F0-9]{6})\s+-->/g
    //   let found = regex.exec(this.project.readme.motd)
    //   while (found) {
    //     if(found.length>2 && keys[found[1]]) {
    //       style[keys[found[1]]] = found[2]
    //     }
    //     found = regex.exec(this.project.readme.motd)
    //   }
      return style
    }
  },
  async mounted () {

    if (window._rundeck && window._rundeck.rdBase && window._rundeck.projectName) {
      const response=await axios.get(`${window._rundeck.rdBase}menu/homeAjax`,{
        headers: {'x-rundeck-ajax': true},
        params: {
          projects: `${window._rundeck.projectName}`
        },
        withCredentials: true
      })
      if (response.data.projects[0]) {
        this.project = response.data.projects[0]
      }
      this.message = this.project.readme.motdHTML
      this.hasNewMessage = this.checkMessage()
      this.showMessage = this.hasNewMessage && this.checkPage()
      this.eventBus.$on('motd-indicator-activated', () => {
        if (!this.showMessage) {
          this.showMessage = true
        } else {
          this.dismissMessage()
        }
      })
      this.eventBus.$emit('motd-message-available', {hasMessage:!!this.project.readme.motd,hasNewMessage:this.hasNewMessage,style:this.motdStyle,title:this.motdTitle,display:this.project.motdDisplay})
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

.btn-close {
  position: absolute;
  top: 10px;
  right: 10px;
}
</style>
