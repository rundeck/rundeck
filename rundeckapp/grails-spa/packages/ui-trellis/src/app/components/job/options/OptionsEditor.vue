<template>
  <div>
    <undo-redo
      :event-bus="localEB"
      data-test="options_undo_redo"
      :revert-all-enabled="true"
    />

    <div class="optslist">
      <draggable
        v-model="intOptions"
        item-key="name"
        handle=".dragHandle"
        @update="dragUpdated"
      >
        <template #item="{ element, index }">
          <div
            :id="`optitem_${index}`"
            class="edit-option-item"
            :class="{ alternate: index % 2 === 1 }"
          >
            <option-edit
              v-if="editIndex === index"
              :ui-features="{ next: false }"
              :error="error"
              :new-option="false"
              :model-value="createOption"
              :file-upload-plugin-type="fileUploadPluginType"
              :features="features"
              :option-values-plugins="providers"
              :job-was-scheduled="optionsData.jobWasScheduled"
              @update:model-value="updateOption(index, $event)"
              @cancel="doCancel"
            />
            <option-item
              v-else
              :editable="!createOption"
              :can-move-down="index < intOptions.length - 1"
              :can-move-up="index > 0"
              :option="element"
              :features="features"
              @move-up="doMoveUp(index)"
              @move-down="doMoveDown(index)"
              @edit="doEdit(index)"
              @delete="doRemove(index)"
              @duplicate="doDuplicate(index)"
            />
          </div>
        </template>
        <template #footer>
          <div
            v-if="createMode"
            class="edit-option-item"
            :class="{ alternate: intOptions.length % 2 === 1 }"
          >
            <option-edit
              id="optitem_new"
              :ui-features="{ next: false }"
              :error="error"
              :new-option="true"
              :model-value="createOption"
              :file-upload-plugin-type="fileUploadPluginType"
              :features="features"
              :option-values-plugins="providers"
              :job-was-scheduled="optionsData.jobWasScheduled"
              @update:model-value="saveNewOption"
              @cancel="doCancel"
            />
          </div>
        </template>
      </draggable>

      <div v-if="intOptions.length < 1 && !createMode" class="empty note">
        <p>{{ $t("no.options.message") }}</p>
      </div>

      <div v-if="edit" id="optnewbutton" style="margin: 10px 0">
        <btn
          size="sm"
          class="ready"
          :title="$t('add.new.option')"
          :disabled="!!createOption"
          @click="optaddnew"
        >
          <b class="glyphicon glyphicon-plus"></b>
          {{ $t("add.an.option") }}
        </btn>
      </div>
    </div>
  </div>
</template>
<script lang="ts">
import { getRundeckContext } from "../../../../library";
import { cloneDeep } from "lodash";
import {
  JobOption,
  JobOptionEdit,
  JobOptionsData,
  OptionPrototype,
} from "../../../../library/types/jobs/JobEdit";
import { Operation, ChangeEvent } from "./model/ChangeEvents";
import OptionItem from "./OptionItem.vue";
import pluginService from "../../../../library/modules/pluginService";
import type {
  PluginProviderDescription,
  PluginProvidersResponse,
} from "../../../../library/modules/pluginService";
import { defineComponent, PropType } from "vue";
import UndoRedo from "../../util/UndoRedo.vue";
import OptionEdit from "./OptionEdit.vue";
import mitt, { Emitter } from "mitt";
import draggable from "vuedraggable";

type UndoRedoEvents = {
  undo: ChangeEvent;
  redo: ChangeEvent;
  revertAll: void;
  change: ChangeEvent;
};

type OperationData =
  | { operation: Operation.Insert; index: number; value: JobOptionEdit }
  | { operation: Operation.Remove; index: number }
  | { operation: Operation.Modify; index: number; value: JobOptionEdit }
  | { operation: Operation.Move; index: number; dest: number };

const emitter = mitt<UndoRedoEvents>();
const localEB: Emitter<UndoRedoEvents> = emitter;
const eventBus = getRundeckContext().eventBus;

export default defineComponent({
  name: "OptionsEditor",
  components: {
    OptionItem,
    UndoRedo,
    OptionEdit,
    draggable,
  },
  props: {
    optionsData: {
      type: Object as PropType<JobOptionsData>,
      required: true,
    },
    features: {
      type: Object as PropType<Record<string, boolean>>,
      required: false,
      default: () => ({}),
    },
    edit: {
      type: Boolean,
      default: true,
    },
  },
  emits: ["changed"],
  data() {
    return {
      localEB,
      error: "",
      createMode: false,
      editIndex: -1,
      origOptions: [] as JobOptionEdit[],
      intOptions: [] as JobOptionEdit[],
      createOption: null as JobOptionEdit | null,
      fileUploadPluginType: "" as string | undefined,
      providers: [] as PluginProviderDescription[],
      providerLabels: {} as Record<string, string>,
    };
  },
  async mounted() {
    this.origOptions = cloneDeep(this.optionsData.options);
    this.intOptions = cloneDeep(this.optionsData.options);
    this.updateIndexes();
    this.fileUploadPluginType = this.optionsData.fileUploadPluginType;
    pluginService
      .getPluginProvidersForService("OptionValues")
      .then((data: PluginProvidersResponse | undefined) => {
        if (data?.service) {
          this.providers = data.descriptions;
          this.providerLabels = data.labels ?? {};
        }
      });
    this.localEB.on("undo", this.doUndo);
    this.localEB.on("redo", this.doRedo);
    this.localEB.on("revertAll", this.doRevertAll);
  },
  beforeUnmount() {
    this.localEB.off("undo");
    this.localEB.off("redo");
    this.localEB.off("revertAll");
  },
  methods: {
    cloneDeep,
    optaddnew() {
      this.createOption = Object.assign({}, OptionPrototype);
      this.createMode = true;
    },
    doEdit(i: number) {
      if (this.createOption) {
        return;
      }
      this.createOption = cloneDeep(this.intOptions[i]);
      this.editIndex = i;
    },
    doDuplicate(i: number) {
      this.createOption = cloneDeep(this.intOptions[i]);
      this.createOption.name = this.createOption.name + "_copy";
      this.createMode = true;
    },
    doRemove(index: number) {
      const orig = this.applyOperation({
        operation: Operation.Remove,
        index,
      });
      this.changeEvent({
        index,
        dest: -1,
        orig: orig as JobOption,
        operation: Operation.Remove,
        undo: Operation.Insert,
      });
    },
    doMoveUp(index: number) {
      if (index > 0) {
        this.applyOperation({
          operation: Operation.Move,
          index,
          dest: index - 1,
        });
        this.changeEvent({
          index: index,
          dest: index - 1,
          operation: Operation.Move,
          undo: Operation.Move,
        });
      }
    },
    doMoveDown(index: number) {
      if (index < this.intOptions.length - 1) {
        this.applyOperation({
          operation: Operation.Move,
          index,
          dest: index + 1,
        });
        this.changeEvent({
          index: index,
          dest: index + 1,
          operation: Operation.Move,
          undo: Operation.Move,
        });
      }
    },
    dragUpdated(change: { oldIndex: number; newIndex: number }) {
      this.updateIndexes();
      this.changeEvent({
        index: change.oldIndex,
        dest: change.newIndex,
        operation: Operation.Move,
        undo: Operation.Move,
      });
    },
    updateOption(index: number, data: JobOptionEdit) {
      const value = cloneDeep(data);
      const orig = this.applyOperation({
        operation: Operation.Modify,
        index,
        value,
      });
      this.changeEvent({
        index: index,
        dest: -1,
        orig: orig as JobOption,
        value,
        operation: Operation.Modify,
        undo: Operation.Modify,
      });
      this.editIndex = -1;
      this.createOption = null;
    },
    saveNewOption(data: JobOptionEdit) {
      this.createMode = false;
      const index = this.intOptions.length;
      const value = cloneDeep(data);
      this.applyOperation({
        operation: Operation.Insert,
        index,
        value,
      });

      this.changeEvent({
        index,
        dest: -1,
        value,
        operation: Operation.Insert,
        undo: Operation.Remove,
      });
      this.createOption = null;
    },
    operationRemove(index: number): JobOptionEdit {
      const oldval = this.intOptions[index];
      this.intOptions.splice(index, 1);
      return oldval;
    },
    operationModify(index: number, data: JobOptionEdit): JobOptionEdit {
      const orig = this.intOptions[index];
      this.intOptions[index] = cloneDeep(data);
      return orig;
    },
    operationMove(index: number, dest: number): void {
      const orig = this.intOptions[index];
      this.intOptions.splice(index, 1);
      this.intOptions.splice(dest, 0, orig);
    },
    operationInsert(index: number, value: JobOptionEdit): void {
      this.intOptions.splice(index, 0, cloneDeep(value));
    },
    applyOperation(data: OperationData): JobOptionEdit | undefined {
      let result: JobOptionEdit | undefined;
      if (data.operation === Operation.Insert) {
        this.operationInsert(data.index, data.value);
      } else if (data.operation === Operation.Remove) {
        result = this.operationRemove(data.index);
      } else if (data.operation === Operation.Modify) {
        result = this.operationModify(data.index, data.value);
      } else if (data.operation === Operation.Move) {
        this.operationMove(data.index, data.dest);
      }
      this.updateIndexes();
      return result;
    },
    replayOperation(
      operation: Operation,
      index: number,
      dest: number,
      value?: JobOptionEdit,
    ) {
      if (operation === Operation.Insert) {
        return this.applyOperation({ operation, index, value: value! });
      }
      if (operation === Operation.Remove) {
        return this.applyOperation({ operation, index });
      }
      if (operation === Operation.Modify) {
        return this.applyOperation({ operation, index, value: value! });
      }
      return this.applyOperation({ operation, index, dest });
    },
    doCancel() {
      this.createMode = false;
      this.editIndex = -1;
      this.createOption = null;
    },
    doUndo(change: ChangeEvent) {
      this.replayOperation(
        change.undo,
        change.dest !== undefined && change.dest >= 0
          ? change.dest
          : change.index,
        change.index >= 0 ? change.index : (change.dest ?? change.index),
        change.orig || change.value,
      );
      this.wasChanged();
    },
    doRedo(change: ChangeEvent) {
      this.replayOperation(
        change.operation,
        change.index,
        change.dest ?? change.index,
        change.value,
      );
      this.wasChanged();
    },
    doRevertAll() {
      this.intOptions = cloneDeep(this.origOptions);
      this.wasChanged();
    },
    updateIndexes() {
      this.intOptions.forEach((opt: JobOption, i: number) => {
        opt.sortIndex = i + 1;
      });
    },
    changeEvent(event: ChangeEvent) {
      this.localEB.emit("change", event);
      this.wasChanged();
    },
    wasChanged() {
      eventBus.emit("job-edit:edited", true);
      this.$emit("changed", this.intOptions);
    },
  },
});
</script>

<style scoped lang="scss">
.edit-option-item {
  background-color: var(--background-color-lvl2);
  border-radius: 3px;
  margin-top: 10px;
  border: 1px solid var(--gray-input-outline);
  &.alternate {
    background-color: var(--background-color);
  }

  padding: 10px;
}

.note {
  margin-top: 10px;
}
</style>
