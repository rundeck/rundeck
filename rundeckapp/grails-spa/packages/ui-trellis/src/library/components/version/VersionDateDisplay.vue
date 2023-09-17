<template>
    <span :class="css">{{formattedDate}}</span>
</template>
<script lang="ts">

import {defineComponent} from 'vue'
import {formatDate} from "../../utilities/DateTimeFormatters";

export default defineComponent({
  name:"VersionDateDisplay",
  computed: {
    formattedDate() {
        return formatDate(this.date, this.format)
    }
  },
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
  },
})
</script>
