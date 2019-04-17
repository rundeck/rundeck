<template>
  <span ><slot v-if="display && needsConfirm" :confirm="confirm" :needs-confirm="needsConfirm">{{message}}</slot></span>
</template>
<script lang="ts">
import Vue from 'vue'
export default Vue.extend({
  props:{
    eventBus:{
      type:Vue,required:true
    },
    message:{
      type:String, required:true
    },
    display:{
      type:Boolean, default: false
    }
  },
  methods:{
    setConfirm(name:string){
      const loc=this.confirm.indexOf(name)
      if(loc<0){
        this.confirm.push(name)
      }
    },
    resetConfirm(name:string){
      const loc=this.confirm.indexOf(name)
      if(loc>=0){
        this.confirm.splice(loc,1)
      }
    }
  },
  data(){
    return{
      confirm: [] as string[]
    }
  },
  computed: {
    needsConfirm: function():boolean {
      return this.confirm.length>0
    }
  },
  mounted(){
    this.eventBus.$on('page-modified',this.setConfirm)
    this.eventBus.$on('page-reset',this.resetConfirm)
    window.onbeforeunload = ()=> {
        if (this.needsConfirm) {
            return this.message||'confirm'
        }
    }
  }
})
</script>
