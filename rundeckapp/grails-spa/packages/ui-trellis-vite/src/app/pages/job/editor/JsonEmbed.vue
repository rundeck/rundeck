<template>
  <div>
    <input type="hidden" v-model="json" :name="fieldName"/>
  </div>
</template>
<script>


export default {
  name: 'JsonEmbed',
  props: ['fieldName','outputData'],
  computed:{
    json(){
      return JSON.stringify(this.outputData)
    }
  }
}
</script>
