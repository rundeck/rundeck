<template>
  <div>
    <input type="hidden" v-model="json" :name="fieldName"/>
  </div>
</template>
<script>
import { defineComponent } from 'vue'

export default defineComponent({
  name: 'JsonEmbed',
  props: ['fieldName','outputData'],
  computed:{
    json(){
      return JSON.stringify(this.outputData)
    }
  }
})
</script>
