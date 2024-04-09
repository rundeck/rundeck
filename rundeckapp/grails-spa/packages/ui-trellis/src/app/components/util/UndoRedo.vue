<template>
  <span class="flow-h">
    <btn
      :class="{ disabled: !hasUndo }"
      @click="doUndo"
      size="xs"
      data-test="undo-btn"
    >
      <i class="glyphicon glyphicon-step-backward"></i>
      {{ $t("util.undoredo.undo") }}
    </btn>
    <btn
      :class="{ disabled: !hasRedo }"
      @click="doRedo"
      size="xs"
      data-test="redo-btn"
    >
      {{ $t("util.undoredo.redo") }}
      <i class="glyphicon glyphicon-step-forward"></i>
    </btn>
    <btn
      size="xs"
      type="simple"
      class="btn-muted"
      v-if="revertAllEnabled && hasUndo"
      @click="doRevertAll"
      data-test="revertAll-btn"
    >
      <i class="glyphicon glyphicon-fast-backward"></i>
      {{ $t("util.undoredo.revertAll") }}
    </btn>
  </span>
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
      let newindex = this.index + 1;
      let change = this.stack[this.index];
      this.index = newindex;
      this.eventBus.emit("undo", change);
    },
    doRedo() {
      if (this.index < 1) {
        return;
      }
      let newindex = this.index - 1;
      let change = this.stack[newindex];
      this.index = newindex;
      this.eventBus.emit("redo", change);
    },
    doRevertAll() {
      this.index = this.stack.length;
      this.eventBus.emit("revertAll");
    },
  },
  mounted() {
    this.eventBus.on("change", this.addChange);
  },
  beforeUnmount() {
    this.eventBus.off("change");
  },
});
</script>
<style scoped lang="scss">
.flow-h > * + * {
  margin-left: var(--spacing-2);
}
</style>
