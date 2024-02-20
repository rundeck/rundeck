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
      <ul class="options">
        <li
          v-for="(option, i) in intOptions"
          :key="i"
          class="edit-option-item"
          :class="{ alternate: i % 2 === 1 }"
        >
          <option-edit
            v-if="editIndex === i"
            :ui-features="{ next: false }"
            :error="error"
            :new-option="false"
            :model-value="createOption"
            :file-upload-plugin-type="fileUploadPluginType"
            :features="features"
            :option-values-plugins="providers"
            @update:modelValue="updateOption(i, $event)"
            @cancel="doCancel"
          />
          <option-item
            v-else
            :editable="!createOption"
            :opt-index="i"
            :can-move-down="i < intOptions.length - 1"
            :can-move-up="i > 0"
            :option="option"
            @moveUp="doMoveUp(i)"
            @moveDown="doMoveDown(i)"
            @edit="doEdit(i)"
            @delete="doRemove(i)"
            @duplicate="doDuplicate(i)"
          />
        </li>
        <li v-if="createMode">
          <option-edit
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
        </li>
      </ul>
      <div
        id="xoptionDropFinal"
        class="dragdropfinal droppableitem"
        :data-abs-index="intOptions.length || 1"
        data-is-final="true"
        style="display: none"
      ></div>

      <div class="empty note" v-if="intOptions.length < 1 && !createMode">
        {{ $t("no.options.message") }}
      </div>

      <div style="margin: 10px 0" v-if="edit">
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
import OptionItem from "./list/OptionItem.vue";
import pluginService from "@/library/modules/pluginService";
import { defineComponent, PropType } from "vue";
import UndoRedo from "../../util/UndoRedo.vue";
import OptionEdit from "./OptionEdit.vue";
import mitt, { Emitter, EventType } from "mitt";

const emitter = mitt();
const localEB: Emitter<Record<EventType, any>> = emitter;
const eventBus = getRundeckContext().eventBus;

enum Operation {
  Insert,
  Remove,
  Modify,
  Move,
}
interface ChangeEvent {
  index: number;
  value?: JobOption;
  orig?: JobOption;
  dest?: number;
  operation: Operation;
  undo: Operation;
}

export default defineComponent({
  name: "OptionsEditor",
  emits: ["changed"],
  components: {
    OptionItem,
    UndoRedo,
    OptionEdit,
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
    doRemove(i: number) {
      let orig = this.operationRemove(i);
      this.changeEvent({
        index: i,
        dest: -1,
        orig: orig,
        operation: Operation.Remove,
        undo: Operation.Insert,
      });
    },
    doMoveUp(i: number) {
      if (i > 0) {
        this.operationMove(i, i - 1);
        this.changeEvent({
          index: i,
          dest: i - 1,
          operation: Operation.Move,
          undo: Operation.Move,
        });
      }
    },
    doMoveDown(i: number) {
      if (i < this.intOptions.length - 1) {
        this.operationMove(i, i + 1);
        this.changeEvent({
          index: i,
          dest: i + 1,
          operation: Operation.Move,
          undo: Operation.Move,
        });
      }
    },
    updateOption(i: number, data: any) {
      let value = cloneDeep(data);
      let orig = this.operationModify(i, value);
      this.changeEvent({
        index: i,
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
      this.operationInsert(index, value);

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
      const temp = this.intOptions[dest];
      this.intOptions[dest] = this.intOptions[index];
      this.intOptions[index] = temp;
    },
    async operationInsert(index, value) {
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
    },
    doRedo(change: ChangeEvent) {
      this.operation(change.operation, change);
    },
    updateIndexes() {
      this.intOptions.forEach((opt: JobOption, i) => {
        opt.sortIndex = i + 1;
      });
    },
    changeEvent(event: ChangeEvent) {
      this.updateIndexes();
      this.localEB.emit("change", event);
      eventBus.emit("job-edit:edited", true);
      this.$emit("changed", this.intOptions);
    },
  },
  async mounted() {
    this.intOptions = clone(this.optionsData.options);
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
}
</style>
