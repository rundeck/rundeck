<template>
  <span :class="clsStyle" class="btn btn-simple btn-xs"  @click=activate v-if="hasMessage && display"  v-tooltip.bottom="!hasNewMessage &&showTitle?motdTitle:''" >
    <i class=" fas " :class=iconStyle></i>
    <span class="motd__title" v-if="hasNewMessage && showTitle" v-html=motdTitle></span>
  </span>
</template>
<script>
/**
 * This indicator gets configured from the motd component via the event bus
 */
export default {
  name: "Motd-Indicator",
  props: ["eventBus"],
  data(){
    return {
      hasMessage:false,
      hasNewMessage:false,
      display:false,
      style:'default',
      motdTitle:null,
      styleIcons:{
        info:'fa-envelope',
        success:'fa-comment-alt',
        warning:'fa-bell',
        danger:'fa-exclamation',
        default: 'fa-comment-alt'
      }
    }
  },

  computed:{
    clsStyle(){
      return this.hasNewMessage?`btn-${this.style}`:'btn-default'
    },
    iconStyle(){
      return this.styleIcons[this.style]||this.styleIcons.default
    },
    showTitle(){
      return true
    }
  },
  methods:{
    activate(){
      this.eventBus.$emit('motd-indicator-activated')
      this.hasNewMessage=false
    }
  },
  mounted(){
      this.eventBus.$on('motd-message-available',(val)=>{
        this.hasMessage=val.hasMessage
        this.hasNewMessage=val.hasNewMessage
        this.style=val.style
        this.motdTitle=val.title
        this.display = val.display && val.display.indexOf('navbar')>=0
      })
  }
};
</script>
<style lang="scss" scoped>
.label{
  cursor: pointer
}

.motd__title {
  margin-left: 5px;
}
</style>
