<template>
  <span ><slot v-if="display && needsConfirm" :confirm="confirmData" :needs-confirm="needsConfirm">{{message}}</slot></span>
</template>
<script lang="ts">
import Vue from 'vue'
import {Component, Prop} from 'vue-property-decorator'

@Component
export default class PageConfirm extends Vue{
  confirmData:  string[] = []

  @Prop({required:true})
  eventBus!:Vue

  @Prop({required:true})
  message!:String

  @Prop({required:true})
  display!:Boolean

  setConfirm(name:string){
    const loc=this.confirmData.indexOf(name)
    if(loc<0){
      this.confirmData.push(name)
    }
  }

  resetConfirm(name:string){
    const loc=this.confirmData.indexOf(name)
    if (loc >= 0) {
        this.confirmData.splice(loc, 1)
    } else if (name === '*') {
        this.confirmData.splice(0, this.confirmData.length)
    }
  }

  get needsConfirm():boolean {
    return this.confirmData.length>0
  }

  mounted() {
      this.eventBus.$on('page-modified', this.setConfirm)
      this.eventBus.$on('page-reset', this.resetConfirm)

      const orighandler = window.onbeforeunload
      const self = this
      window.onbeforeunload = (ev: BeforeUnloadEvent) => {
          if (this.needsConfirm) {
              return self.message || 'confirm'
          }
          if (typeof (orighandler) === 'function') {
              //@ts-ignore
              return orighandler(ev)
          }
      }
  }
}
</script>
