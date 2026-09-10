<template>
  <div class="flow-h">
    <btn
      :class="{ disabled: !hasUndo }"
      size="xs"
      data-testid="undo-btn"
      @click="doUndo"
    >
      <i class="glyphicon glyphicon-step-backward"></i>
      {{ $t("util.undoredo.undo") }}
    </btn>
    <btn
      :class="{ disabled: !hasRedo }"
      size="xs"
      data-testid="redo-btn"
      @click="doRedo"
    >
      {{ $t("util.undoredo.redo") }}
      <i class="glyphicon glyphicon-step-forward"></i>
    </btn>
    <btn
      v-if="revertAllEnabled && hasUndo"
      size="xs"
      type="simple"
      class="btn-muted"
      data-testid="revertAll-btn"
      @click="doRevertAll"
    >
      <i class="glyphicon glyphicon-fast-backward"></i>
      {{ $t("util.undoredo.revertAll") }}
    </btn>
  </div>
</template>
<script lang="ts">
import { PropType, defineComponent } from "vue";
import { EventBus } from "../../../library";

export default defineComponent({
  name: "UndoRedo",
  props: {
    eventBus: Object as PropType<typeof EventBus>,
    revertAllEnabled: {
      type: Boolean,
      default: false,
    },
  },
  data() {
    return {
      stack: <any>[],
      index: 0,
      revertAllConfirm: false,
    };
  },
  computed: {
    hasUndo(): boolean {
      return this.stack.length > this.index;
    },
    hasRedo(): boolean {
      return this.index > 0;
    },
  },
  mounted() {
    this.eventBus?.on("change", this.addChange);
  },
  beforeUnmount() {
    this.eventBus?.off("change");
  },
  methods: {
    addChange(val: any) {
      if (this.index > 0) {
        this.stack.splice(0, this.index);
        this.index = 0;
      }
      this.stack.unshift(val);
    },
    doUndo() {
      if (this.index >= this.stack.length) {
        return;
      }
      const newindex = this.index + 1;
      const change = this.stack[this.index];
      this.index = newindex;
      this.eventBus?.emit("undo", change);
    },
    doRedo() {
      if (this.index < 1) {
        return;
      }
      const newindex = this.index - 1;
      const change = this.stack[newindex];
      this.index = newindex;
      this.eventBus?.emit("redo", change);
    },
    doRevertAll() {
      this.index = this.stack.length;
      this.eventBus?.emit("revertAll");
    },
  },
});
</script>
<style scoped lang="scss">
.flow-h > * + * {
  margin-left: var(--spacing-2);
}
</style>
<style lang="scss">
.edit-lock-disabled .btn.btn-xs.btn-default,
.edit-lock-disabled .btn.btn-simple.btn-xs.btn-muted {
  opacity: 0.4 !important;
  cursor: not-allowed !important;
  pointer-events: none !important;
}
</style>
