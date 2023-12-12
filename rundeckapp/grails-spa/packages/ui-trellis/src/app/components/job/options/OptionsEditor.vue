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
        <ol>
          <li v-for="option in intOptions">
            {{ option }}
          </li>
          <!--          <g:render template="/scheduledExecution/optlistContent" model="${[options:options,edit:edit]}"/>-->
          <li v-if="createMode">
            <option-edit
              :error="error"
              :new-option="true"
              v-model="createOption"
              :file-upload-plugin-type="fileUploadPluginType"
              :features="features"
              :option-values-plugins="providers"
            />
          </li>
        </ol>
      </ul>
      <div
        id="optionDropFinal"
        class="dragdropfinal droppableitem"
        :data-abs-index="intOptions.length || 1"
        data-is-final="true"
        style="display: none"
      ></div>
      <!--      <g:embedJSON id="optDataList" data="${options.collect{[name:it.name,type:it.optionType,multivalued:it.multivalued, delimiter: it.delimiter]}}"/>-->

      <div
        class="empty note"
        :class="{ error: error }"
        id="optempty"
        v-if="intOptions.length < 1 && !createMode"
      >
        {{ $t("no.options.message") }}
      </div>

      <div style="margin: 10px 0" v-if="edit">
        <btn
          size="sm"
          class="ready"
          @click="optaddnew"
          :title="$t('add.new.option')"
        >
          <b class="glyphicon glyphicon-plus"></b>
          {{ $t("add.new.option") }}
        </btn>
      </div>
    </div>

    <div id="optnewitem"></div>
  </div>
</template>
<script lang="ts">
import pluginService from '@/library/modules/pluginService'
import { defineComponent } from "vue";
import UndoRedo from "../../util/UndoRedo.vue";
import OptionEdit from "./OptionEdit.vue";
import mitt, { Emitter, EventType } from "mitt";

const emitter = mitt();
const localEB: Emitter<Record<EventType, any>> = emitter;

export default defineComponent({
  name: "OptionsEditor",
  components: {
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
      intOptions: [],
      createOption: {},
      fileUploadPluginType: "",
      features: {},
      providers:[],
      providerLabels:{}
    };
  },
  methods: {
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
        realValuesUrl:null,
        optionValuesPluginType:'',
        remoteUrlAuthenticationType:'',
        configRemoteUrl:{}
      };
      this.createMode = true;
    },
  },
  async mounted() {
    this.intOptions = this.optionsData.options;
    this.fileUploadPluginType = this.optionsData.fileUploadPluginType;
    this.features = this.optionsData.features;
    pluginService
      .getPluginProvidersForService('OptionValues')
      .then(data => {
        if (data.service) {
          this.providers = data.descriptions;
          this.providerLabels = data.labels;
        }
      });
  },
});
</script>

<style scoped lang="scss"></style>
