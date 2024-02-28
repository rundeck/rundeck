<template>
  <div>
    <undo-redo :event-bus="localEB" />

    <div class="optslist">
      <div v-if="intOptions.length > 0">
        <div class="optheader optctrlholder">
          <span class="optdetail">
            <span class="header">
              {{ $t("option.list.header.name.title") }}
            </span>
          </span>
          <span class="valuesSet">
            <span class="header">
              {{ $t("option.list.header.values.title") }}
            </span>
          </span>
          <span class="enforceSet">
            <span class="header">
              {{ $t("option.list.header.restrictions.title") }}
            </span>
          </span>
        </div>
      </div>
      <div class="clear"></div>
      <draggable
        v-model="intOptions"
        @update="dragUpdated"
        item-key="name"
        handle=".dragHandle"
      >
        <template #item="{ element, index }">
          <div
            class="edit-option-item"
            :class="{ alternate: index % 2 === 1 }"
            :id="`optitem_${index}`"
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
              @update:modelValue="updateOption(index, $event)"
              @cancel="doCancel"
            />
            <option-item
              v-else
              :editable="!createOption"
              :can-move-down="index < intOptions.length - 1"
              :can-move-up="index > 0"
              :option="element"
              @moveUp="doMoveUp(index)"
              @moveDown="doMoveDown(index)"
              @edit="doEdit(index)"
              @delete="doRemove(index)"
              @duplicate="doDuplicate(index)"
            />
          </div>
        </template>
        <template #footer>
          <template v-if="createMode">
            <option-edit
              id="optitem_new"
              :ui-features="{ next: false }"
              :error="error"
              :new-option="true"
              :model-value="createOption"
              :file-upload-plugin-type="fileUploadPluginType"
              :features="features"
              :option-values-plugins="providers"
              @update:modelValue="saveNewOption"
              @cancel="doCancel"
            />
          </template>
        </template>
      </draggable>

      <div class="empty note" v-if="intOptions.length < 1 && !createMode">
        {{ $t("no.options.message") }}
      </div>

      <div style="margin: 10px 0" v-if="edit" id="optnewbutton">
        <btn
          size="sm"
          class="ready"
          @click="optaddnew"
          :title="$t('add.new.option')"
          :disabled="!!createOption"
        >
          <b class="glyphicon glyphicon-plus"></b>
          {{ $t("add.an.option") }}
        </btn>
      </div>
    </div>
  </div>
</template>
<script lang="ts">
import { getRundeckContext } from "@/library";
import { cloneDeep, clone } from "lodash";
import {
  JobOption,
  JobOptionsData,
  OptionPrototype,
} from "../../../../library/types/jobs/JobEdit";
import { Operation, ChangeEvent } from "./model/ChangeEvents";
import OptionItem from "./OptionItem.vue";
import pluginService from "@/library/modules/pluginService";
import { defineComponent, PropType } from "vue";
import UndoRedo from "../../util/UndoRedo.vue";
import OptionEdit from "./OptionEdit.vue";
import mitt, { Emitter, EventType } from "mitt";
import draggable from "vuedraggable";

const emitter = mitt();
const localEB: Emitter<Record<EventType, any>> = emitter;
const eventBus = getRundeckContext().eventBus;

export default defineComponent({
  name: "OptionsEditor",
  emits: ["changed"],
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
    edit: {
      type: Boolean,
      default: true,
    },
  },
  data() {
    return {
      localEB,
      error: "",
      createMode: false,
      editIndex: -1,
      intOptions: [] as JobOption[],
      createOption: null,
      fileUploadPluginType: "",
      features: {},
      providers: [],
      providerLabels: {},
    };
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
      let orig = this.operation(Operation.Remove, { index });
      this.changeEvent({
        index,
        dest: -1,
        orig: orig,
        operation: Operation.Remove,
        undo: Operation.Insert,
      });
    },
    doMoveUp(index: number) {
      if (index > 0) {
        this.operation(Operation.Move, { index, dest: index - 1 });
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
        this.operation(Operation.Move, { index, dest: index + 1 });
        this.changeEvent({
          index: index,
          dest: index + 1,
          operation: Operation.Move,
          undo: Operation.Move,
        });
      }
    },
    dragUpdated(change) {
      this.updateIndexes();
      this.changeEvent({
        index: change.oldIndex,
        dest: change.newIndex,
        operation: Operation.Move,
        undo: Operation.Move,
      });
    },
    updateOption(index: number, data: any) {
      let value = cloneDeep(data);
      let orig = this.operation(Operation.Modify, { index, value });
      this.changeEvent({
        index: index,
        dest: -1,
        orig,
        value,
        operation: Operation.Modify,
        undo: Operation.Modify,
      });
      this.editIndex = -1;
      this.createOption = null;
    },
    saveNewOption(data: any) {
      this.createMode = false;
      let index = this.intOptions.length;
      let value = cloneDeep(data);
      this.operation(Operation.Insert, { index, value });

      this.changeEvent({
        index,
        dest: -1,
        value,
        operation: Operation.Insert,
        undo: Operation.Remove,
      });
      this.createOption = null;
    },
    operationRemove(index: number) {
      let oldval = this.intOptions[index];
      this.intOptions.splice(index, 1);
      return oldval;
    },
    operationModify(index: number, data: any) {
      let orig = this.intOptions[index];
      this.intOptions[index] = cloneDeep(data);
      return orig;
    },
    operationMove(index: number, dest: number) {
      const orig = this.intOptions[index];
      this.intOptions.splice(index, 1);
      this.intOptions.splice(dest, 0, orig);
    },
    operationInsert(index: number, value: any) {
      this.intOptions.splice(index, 0, cloneDeep(value));
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
      this.updateIndexes();
    },
    doCancel() {
      this.createMode = false;
      this.editIndex = -1;
      this.createOption = null;
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
  async mounted() {
    this.intOptions = cloneDeep(this.optionsData.options);
    this.updateIndexes();
    this.fileUploadPluginType = this.optionsData.fileUploadPluginType;
    this.features = this.optionsData.features;
    pluginService.getPluginProvidersForService("OptionValues").then((data) => {
      if (data.service) {
        this.providers = data.descriptions;
        this.providerLabels = data.labels;
      }
    });
    this.localEB.on("undo", this.doUndo);
    this.localEB.on("redo", this.doRedo);
  },
  beforeUnmount() {
    this.localEB.off("undo");
    this.localEB.off("redo");
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
    background-color: var(--background-color-accent-lvl2);
  }

  padding: 10px 4px;
}
</style>
