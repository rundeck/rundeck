<template>
  <div>
    <transition name="fade" appear>
      <div v-if="loading">
        <slot name="loading">
          <div class="loader">
            <i class="fas fa-spinner fa-spin loading-spinner text-muted" data-testid="loading-spinner" />
            {{ $t("loading.text") }}
          </div>
        </slot>
      </div>
      <div
        v-else
        class="list-container"
        :class="{ 'ea-mode': conditionalEnabled }"
        data-testid="list-container"
      >
        <undo-redo
          :class="{ right: conditionalEnabled }"
          data-test="options_undo_redo"
          :event-bus="localEB"
          :revert-all-enabled="revertAllEnabled"
        />

        <div class="w-full">
          <draggable
            v-if="internalData && internalData.length > 0"
            v-model="internalData"
            :item-key="itemKey"
            :handle="handle"
            :tag="draggableTag"
            :disabled="draggableDisabled"
            data-testid="draggable-container"
            @update="dragUpdated"
          >
            <template #item="{ element, index }">
              <div class="item-container" data-testid="item-container">
                <slot name="item" :item="{ element, index }" />
              </div>
            </template>
            <template #footer>
              <slot name="footer"></slot>
            </template>
          </draggable>
          <slot v-else name="empty" />
        </div>
        <slot v-if="mode === 'edit' && !loading">
          <btn
            id="addButton"
            size="sm"
            class="ready"
            data-testid="add-button"
            :disabled="addButtonDisabled"
            @click="handleButtonClick"
          >
            <i class="fas fa-plus" />
            {{ buttonLabel || $t("add.an.option") }}
          </btn>
        </slot>
        <slot name="extra" />
      </div>
    </transition>
  </div>
</template>

<script lang="ts">
import { defineComponent } from "vue";
import UndoRedo from "@/app/components/util/UndoRedo.vue";
import draggable from "vuedraggable";
import { getRundeckContext } from "@/library";
import mitt, { Emitter, EventType } from "mitt";
import {
  ChangeEvent,
  Operation,
} from "@/app/components/job/options/model/ChangeEvents";
import { cloneDeep } from "lodash";

const emitter = mitt();
const localEB: Emitter<Record<EventType, any>> = emitter;

export default defineComponent({
  name: "CommonUndoRedoDraggableList",
  components: { UndoRedo, draggable },
  props: {
    revertAllEnabled: {
      type: Boolean,
      default: false,
    },
    mode: {
      type: String,
      default: "edit",
    },
    buttonLabel: {
      type: String,
      default: "",
    },
    modelValue: {
      type: Object,
      default: () => {},
    },
    itemKey: {
      type: String,
      default: "name",
    },
    handle: {
      type: String,
      default: ".dragHandle",
    },
    draggableTag: {
      type: String,
      default: "div",
    },
    loading: {
      type: Boolean,
      default: false,
    },
    conditionalEnabled: {
      type: Boolean,
      default: false,
    },
    addButtonDisabled: {
      type: Boolean,
      default: false,
    },
    draggableDisabled: {
      type: Boolean,
      default: false,
    },
  },
  emits: ["addButtonClick", "update:modelValue"],
  data() {
    return {
      eventBus: getRundeckContext().eventBus,
      localEB,
      internalData: null,
      originalData: null,
    };
  },
  watch: {
    modelValue: {
      deep: true,
      handler(newVal) {
        this.internalData = newVal;
        // if by any chance component got mounted before data was available,
        // update it once modelValue gets populated
        if (this.originalData === null) {
          this.originalData = cloneDeep(newVal);
        }
      },
    },
  },
  async mounted() {
    if (this.modelValue) {
      this.originalData = cloneDeep(this.modelValue);
      this.internalData = cloneDeep(this.modelValue);
    }

    this.localEB.on("undo", this.doUndo);
    this.localEB.on("redo", this.doRedo);
    this.localEB.on("revertAll", this.doRevertAll);
  },

  methods: {
    handleButtonClick() {
      this.$emit("addButtonClick");
    },
    cloneDeep,
    dragUpdated(change) {
      this.changeEvent({
        index: change.oldIndex,
        dest: change.newIndex,
        operation: Operation.Move,
        undo: Operation.Move,
      });
    },
    operationRemove(index: number) {
      const oldval = this.internalData[index];
      this.internalData.splice(index, 1);
      return oldval;
    },
    operationModify(index: number, data: any) {
      const orig = this.internalData[index];
      this.internalData[index] = cloneDeep(data);
      return orig;
    },
    operationMove(index: number, dest: number) {
      const orig = this.internalData[index];
      this.internalData.splice(index, 1);
      this.internalData.splice(dest, 0, orig);
    },
    operationInsert(index: number, value: any) {
      this.internalData.splice(index, 0, cloneDeep(value));
    },
    operation(op: Operation, data: any) {
      if (op === Operation.Insert) {
        this.operationInsert(data.index, data.value);
      } else if (op === Operation.Remove) {
        this.operationRemove(data.index);
      } else if (op === Operation.Modify) {
        this.operationModify(data.index, data.value);
      } else if (op === Operation.Move) {
        this.operationMove(data.index, data.dest);
      }
    },
    doUndo(change: ChangeEvent) {
      this.operation(change.undo, {
        index: change.dest >= 0 ? change.dest : change.index,
        dest: change.index >= 0 ? change.index : change.dest,
        value: change.orig || change.value,
      });
      this.wasChanged();
    },
    doRedo(change: ChangeEvent) {
      this.operation(change.operation, change);
      this.wasChanged();
    },
    doRevertAll() {
      this.internalData = cloneDeep(this.originalData);
      this.wasChanged();
    },
    changeEvent(event: ChangeEvent) {
      this.localEB.emit("change", event);
      this.wasChanged();
    },
    wasChanged() {
      if (this.eventBus) {
        this.eventBus.emit("job-edit:edited", true);
      } else {
        this.localEB.emit("job-edit:edited", true);
      }

      this.$emit("update:modelValue", this.internalData);
    },
  },
});
</script>

<style scoped lang="scss">
.w-full {
  width: 100%;
}

.list-container {
  display: flex;
  flex-direction: column;
  flex-wrap: wrap;
  align-items: flex-start;
  gap: 10px;
  height: auto;

  .right {
    align-self: flex-end;
  }
}

.loader {
  align-items: center;
  display: flex;
  gap: 10px;
}

.fade-enter-active,
.fade-leave-active {
  transition: opacity 0.5s ease-in-out;
  will-change: opacity;
}
.fade-enter-from,
.fade-leave-to * {
  opacity: 0;
  height: 0;
  overflow: hidden;
}
</style>
