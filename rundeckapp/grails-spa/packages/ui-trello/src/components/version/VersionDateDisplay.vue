<template>
    <span :class="css">{{date | moment(format)}}</span>
</template>
<script lang="ts">

import Vue from 'vue'

export default Vue.extend({

  props: {
    format: {
      type: String, required: false, default:'YYYY-MM-DD'
    },
    css: {
      type: String, required: false, default:'rundeck-version-date'
    },
    date: {
      type: String, required: true
    },
  }
})
</script>
