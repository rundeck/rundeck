<template>
<div  style="padding:30px">
    <div class="rd-trow">
      <div class="cell cell--none">
        <input type="checkbox">
      </div>
      <div v-for="(field, index) in fields" :key="index" class="cell" :class="[
       field.type == 'input-switch' ? 'cell-none' : '' ,
       field.type == 'input-text' ? 'cell--100' : '' ,
       field.type == 'text' ? 'cell--100' : '' ,
      ]">
        <span v-if="field.type == 'text'">{{ field.value }}</span>
        <input type="text" v-if="field.type == 'input-text'">
        <rd-switch v-model="fields[index].value" contrast v-if="field.type == 'input-switch'" class="justify-end"/>
      </div>
    </div>
</div>
</template>
<script>
import RdSwitch from '../inputs/Switch.vue'
export default {
  components: {
    RdSwitch
  },
  data () {
    return {
      fields: [
        {
          type: 'text',
          value: 'Zingbats Logistics Runbook'
        },
        {
          type: 'input-text',
          name: 'filter',
          value: ''
        },
        {
          type: 'input-switch',
          name: 'enabled',
          value: false
        },
      ]
    }
  }
}
</script>
<style lang="scss" scoped>
@import '../../../theme/scss/paper/_variables.scss';
$cell-padding: 1rem;
.rd-trow{
  border-top:1px solid $grey-400;
  border-left:1px solid $grey-400;
  border-right:1px solid $grey-400;
  background-color: $white;
  display: flex;
  justify-content: space-between;
  align-items: center;
  &:last-child{
    border-bottom:1px solid $grey-400;
  }
}
.cell{
  display: flex;
  padding: $cell-padding;
  flex-grow: 1;
  &--100{
    width: 100%
  }
  &--none{
    flex: none;
  }
}
.checkbox label{
  padding: 0;
}
.justify-end{
  justify-self: end;
}
input[type="text"]{
  width: 100%;
}
</style>