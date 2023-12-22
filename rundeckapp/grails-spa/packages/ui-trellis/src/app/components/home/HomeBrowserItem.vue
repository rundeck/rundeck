<template>
  <DynamicScrollerItem
    :item="project"
    :size-dependencies="[
        project.description,
        project.failedCount,
    ]"
  >

  </DynamicScrollerItem>
</template>

<script lang="ts">
import {defineComponent} from 'vue'

export default defineComponent({
  name: "HomeBrowserItem"
})
</script>