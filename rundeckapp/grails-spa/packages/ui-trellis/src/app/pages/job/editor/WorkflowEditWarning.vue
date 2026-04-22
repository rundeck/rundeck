<template>
  <span v-if="isEditing" data-testid="workflow-edit-warning" class="text-warning">
    {{ $t('job.editor.workflow.unsavedchanges.warning') }}
  </span>
</template>

<script lang="ts">
import { defineComponent } from "vue";
import { getRundeckContext } from "@/library";

const eventBus = getRundeckContext().eventBus;

export default defineComponent({
  name: "WorkflowEditWarning",
  data() {
    return {
      isEditing: false,
    };
  },
  mounted() {
    eventBus.on("workflow-editing-state-changed", this.onStateChange);
  },
  unmounted() {
    eventBus.off("workflow-editing-state-changed", this.onStateChange);
  },
  methods: {
    onStateChange({ isEditing }: { isEditing: boolean }) {
      this.isEditing = isEditing;
    },
  },
});
</script>
