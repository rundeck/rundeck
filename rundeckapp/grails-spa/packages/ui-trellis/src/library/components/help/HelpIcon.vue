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
import { defineComponent, getCurrentInstance } from "vue";
import { Popover } from 'uiv';

export default defineComponent({
  name: "HelpIcon",
  components: { Popover },
  computed: {
    _uid(): number {
      return getCurrentInstance()?.uid || 0;
    },
  },
  props: {
    title: String,
  },
});
</script>

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
