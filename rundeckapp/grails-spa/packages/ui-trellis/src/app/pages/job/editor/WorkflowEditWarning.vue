<template>
  <span v-if="isEditing" class="text-warning">{{ message }}</span>
</template>

<script lang="ts">
import { defineComponent } from "vue";
import { getRundeckContext } from "@/library";

export default defineComponent({
  name: "WorkflowEditWarning",
  props: {
    itemData: {
      type: [String, Object],
      default: () => ({}),
    },
  },
  data() {
    return {
      isEditing: false,
    };
  },
  computed: {
    message(): string {
      const d = this.itemData as any;
      if (typeof d === "string") return d;
      return d?.message || "";
    },
  },
  mounted() {
    const ctx = getRundeckContext();
    if (ctx?.eventBus) {
      ctx.eventBus.on("workflow-editing-state-changed", this.onStateChange);
    }
  },
  unmounted() {
    const ctx = getRundeckContext();
    if (ctx?.eventBus) {
      ctx.eventBus.off("workflow-editing-state-changed", this.onStateChange);
    }
  },
  methods: {
    onStateChange({ isEditing }: { isEditing: boolean }) {
      this.isEditing = isEditing;
    },
  },
});
</script>
