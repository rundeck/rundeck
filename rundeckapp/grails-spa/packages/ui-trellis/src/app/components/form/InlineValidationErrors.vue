<template>
  <div class="text-warning" v-if="errors  && errors.length>0">
    <ul>
      <li v-for="err in errors">{{ err }}</li>
    </ul>
    <i class="glyphicon glyphicon-warning-sign"></i>
  </div>
</template>
<script lang="ts">

import { defineComponent } from 'vue'
import type { PropType } from 'vue'

export default defineComponent({
  name: 'InlineValidationErrors',
  props: {
    errors: {
      type: Array as PropType<string[]>,
      required: true,
    }
  }
})
</script>