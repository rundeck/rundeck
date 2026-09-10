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
    title: {
      type: String,
      default: "",
    },
  },
});
</script>

<!-- eslint-disable-next-line vue/enforce-style-attribute -- .popover-content is rendered by Bootstrap's popover JS outside this component's DOM subtree, so scoped styles can't reach it -->
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
