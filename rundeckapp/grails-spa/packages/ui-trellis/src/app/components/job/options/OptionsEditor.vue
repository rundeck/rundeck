<template>
  <div>
    <undo-redo :event-bus="localEB" />

    <div class="optslist">
      <div v-if="intOptions.length > 0">
        <div class="optheader optctrlholder">
          <span class="optdetail">
            <span class="header">Name</span>
          </span>
          <span class="valuesSet">
            <span class="header">Values</span>
          </span>
          <span class="enforceSet">
            <span class="header">Restriction</span>
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
            @delete="intOptions.splice(i, 1)"
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
      <!--      <g:embedJSON id="optDataList" data="${options.collect{[name:it.name,type:it.optionType,multivalued:it.multivalued, delimiter: it.delimiter]}}"/>-->

      <div class="empty note" v-if="intOptions.length < 1 && !createMode">
        {{ $t("no.options.message") }}
      </div>

      <div style="margin: 10px 0" v-if="edit">
        <btn
          size="sm"
          class="ready"
          @click="optaddnew"
          :title="$t('add.new.option')"
          :disabled="createOption"
        >
          <b class="glyphicon glyphicon-plus"></b>
          {{ $t("add.an.option") }}
        </btn>
      </div>
    </div>
  </div>
</template>
<script lang="ts">
import { cloneDeep } from "lodash";
import OptionItem from "./list/OptionItem.vue";
import pluginService from "@/library/modules/pluginService";
import { defineComponent } from "vue";
import UndoRedo from "../../util/UndoRedo.vue";
import OptionEdit from "./OptionEdit.vue";
import mitt, { Emitter, EventType } from "mitt";

const emitter = mitt();
const localEB: Emitter<Record<EventType, any>> = emitter;

export default defineComponent({
  name: "OptionsEditor",
  components: {
    OptionItem,
    UndoRedo,
    OptionEdit,
  },
  props: {
    optionsData: {
      type: Object,
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
      intOptions: [],
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
      this.createOption = {
        optionType: "text",
        required: false,
        hidden: false,
        multivalued: false,
        multivalueAllSelected: false,
        secureInput: false,
        secureExposed: false,
        inputType: "plain",
        realValuesUrl: null,
        optionValuesPluginType: "",
        remoteUrlAuthenticationType: "",
        configRemoteUrl: {},
        sortValues: false,
        regex: null,
      };
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
    doMoveUp(i: number) {
      if (i > 0) {
        const temp = this.intOptions[i - 1];
        this.intOptions[i - 1] = this.intOptions[i];
        this.intOptions[i] = temp;
      }
    },
    doMoveDown(i: number) {
      if (i < this.intOptions.length - 1) {
        const temp = this.intOptions[i + 1];
        this.intOptions[i + 1] = this.intOptions[i];
        this.intOptions[i] = temp;
      }
    },
    updateOption(i: number, data: any) {
      this.intOptions[i] = cloneDeep(data);
      this.editIndex = -1;
      this.createOption = null;
    },
    saveNewOption(data: any) {
      this.createMode = false;
      this.intOptions.push(cloneDeep(data));
      this.createOption = null;
    },
    doCancel() {
      this.createMode = false;
      this.editIndex = -1;
      this.createOption = null;
    },
  },
  async mounted() {
    this.intOptions = this.optionsData.options;
    this.fileUploadPluginType = this.optionsData.fileUploadPluginType;
    this.features = this.optionsData.features;
    pluginService.getPluginProvidersForService("OptionValues").then((data) => {
      if (data.service) {
        this.providers = data.descriptions;
        this.providerLabels = data.labels;
      }
    });
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