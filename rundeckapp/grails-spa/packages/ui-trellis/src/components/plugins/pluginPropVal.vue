<template>
  <span>

    <template  v-if="prop.type==='Boolean'">
        <template v-if="value==='true'||value===true">
          {{prop.options&&prop.options['booleanTrueDisplayValue']?prop.options['booleanTrueDisplayValue']:$t('yes')}}
        </template>
        <template v-if="value==='false'||value===false">
          {{prop.options&&prop.options['booleanFalseDisplayValue']?prop.options['booleanFalseDisplayValue']:$t('no')}}
        </template>
    </template>
    <template v-else-if="['Options', 'Select','FreeSelect'].indexOf(prop.type)>=0">
      {{ prop.selectLabels && prop.selectLabels[value] || value }}
    </template>
    <template v-else-if="prop.options && prop.options['displayType']==='PASSWORD'">&bull;&bull;&bull;&bull;&bull;&bull;&bull;&bull;&bull;&bull;&bull;&bull;</template>
    <template v-else>{{ value }}</template>
  </span>
</template>

<script lang="ts">
import Vue from "vue"
export default Vue.extend({
  props: {
    'prop': {
      type: Object,
      required: true
    },
    'value': {
      required: true,
      default: ''
    }
  }
})
</script>
