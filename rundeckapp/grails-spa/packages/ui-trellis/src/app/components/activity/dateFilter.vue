<template>
  <div class="form-group">
    <span class="checkbox col-sm-2">
      <input type="checkbox" :id="uid" v-model="enabled" class="checkbox">
      <label :for="uid"><slot>Enabled</slot></label>
    </span>
    <div class="col-sm-10">
    <dropdown v-if="enabled" >
      <div class="input-group"  >

        <div class="input-group-btn">
          <btn class="dropdown-toggle"><i class="glyphicon glyphicon-calendar"/></btn>
        </div>

        <input type="text" v-model=datetime class="form-control"  />
      </div>

      <template v-slot:dropdown>
        <li style="padding: 10px">
        <date-time-picker v-model="datetime" dateClass="flex-item-1" timeClass="flex-item-auto" class="flex-container"/>
        </li>
      </template>
    </dropdown>
    </div>
  </div>
</template>
<script>
import { defineComponent } from 'vue'
import _ from 'lodash'
import DateTimePicker from './dateTimePicker.vue'

export default defineComponent({
  props: ["modelValue"],
  components:{
    DateTimePicker
  },
  emits: ['update:modelValue'],
  data() {
    return {
      uid:_.uniqueId(),
      enabled: this.modelValue.enabled,
      datetime: this.modelValue.datetime,
      picker:false
    };
  },
  watch:{
    enabled:{
      handler(newVal,oldVal){
        this.$emit('update:modelValue',{enabled:this.enabled,datetime:this.datetime})
      }
    },
    datetime:{
      handler(newVal,oldVal){
        this.$emit('update:modelValue',{enabled:this.enabled,datetime:this.datetime})
      }
    },
    modelValue() {
      this.enabled= this.modelValue.enabled
      this.datetime= this.modelValue.datetime
    }
  }
})
</script>
<style lang="scss" scoped>
.label-holder{
  padding-right: 10px;
}
</style>
