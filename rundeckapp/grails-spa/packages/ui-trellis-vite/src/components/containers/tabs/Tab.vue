<template>
  <div v-show="isActive" class="tab-pane">
    <slot></slot>
  </div>
</template>

<script lang="ts">

export default {
  props:{
    title: {
      type: String,
      default: 'patabs--standard'
    },
  },
  data() {
    return {
      isActive: true
    }
  }
}
</script>