<template>
  <div>
    <date-picker v-model="dateString" :class=dateClass :clear-btn="false" class="bs-date-picker"/>
    <time-picker v-model="time" :class=timeClass />
  </div>
</template>
<script>
import { defineComponent } from 'vue'
import moment from 'moment'

export default defineComponent({
  props:['modelValue','dateClass','timeClass'],
  emits: ['update:modelValue'],
  data(){
    return {
      dateString:'',
      time:new Date()
    }
  },
  methods:{
    recalcDate(){
      let mo=moment(this.time)
      let date=moment(this.dateString)
      mo.year(date.year())
      mo.month(date.month())
      mo.date(date.date())
      this.time=mo.toDate()
    },
    setFromValue(){
      if(this.modelValue){
        let dt=moment(this.modelValue)
        this.time=dt.toDate()
        this.dateString=dt.format('YYYY-MM-DD')
      }
    }
  },
  watch:{
    dateString:{
      handler(newVal,oldVal){
        this.recalcDate()
      }
    },
    time:{
      handler(newVal,oldVal){
        this.$emit('update:modelValue', this.datetime)
      }
    },
    modelValue:{
      handler(newVal,oldVal){
        this.setFromValue()
      }
    }
  },
  computed:{
    datetime(){
      return moment(this.time).format()
    }
  },
  mounted(){
    this.setFromValue()
  }
})
</script>
<style lang="scss" >
.bs-date-picker{
  .btn-primary{
    background: #4499ff;
    color: white;
  }
}
</style>
