<template>
  <div class="help-icon">
    <i
      :id="`help-${_uid}`"
      class="fas fa-sm fa-question-circle text-muted"
      style="cursor: pointer; margin-left: 5px"
    />
    <popover
      :title="title"
      :target="`#help-${_uid}`"
      custom-class="popover-wide"
      viewport="#section-main"
    >
      <template #popover>
        <slot></slot>
      </template>
    </popover>
  </div>
</template>

<script lang="ts">
import { defineComponent } from "vue";

export default defineComponent({
  name: "HelpIcon",
  props: {
    title: String,
  },
});
</script>

<!-- eslint-disable-next-line vue/enforce-style-attribute -- styles teleported overlay content -->
<style>
.popover-content {
  max-height: 50vh;
  overflow-y: auto;
}
</style>

<style scoped>
.help-icon {
  display: inline;
}
</style>
