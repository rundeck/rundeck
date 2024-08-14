<template>
  <div>
    <template v-if="prop.type === 'Boolean'">
      <div
        v-if="prop.defaultValue !== 'true'"
        class="col-xs-10 col-xs-offset-2"
      >
        <div class="checkbox">
          <input
            v-if="!renderReadOnly"
            :id="`${rkey}prop_` + pindex"
            v-model="currentValue"
            type="checkbox"
            :name="`${rkey}prop_` + pindex"
            value="true"
          />
          <label :for="`${rkey}prop_` + pindex">{{ prop.title }}</label>
        </div>
      </div>
      <label
        v-if="prop.defaultValue === 'true'"
        :class="
          'col-sm-2 control-label input-sm ' + (prop.required ? 'required' : '')
        "
        :for="`${rkey}prop_` + pindex"
        >{{ prop.title }}</label
      >
      <div v-if="prop.defaultValue === 'true'" class="col-xs-10">
        <label :for="`${rkey}prop_true_` + pindex" class="radio-inline">
          <input
            v-if="!renderReadOnly"
            :id="`${rkey}prop_true_` + pindex"
            v-model="currentValue"
            type="radio"
            :name="`${rkey}prop_` + pindex"
            value="true"
          />
          <input
            v-else
            :id="`${rkey}prop_true_` + pindex"
            v-model="currentValue"
            type="radio"
            :name="`${rkey}prop_` + pindex"
            value="true"
            :disabled="true"
          />
          <plugin-prop-val :prop="prop" :value="'true'" />
        </label>
        <label :for="`${rkey}prop_false_` + pindex" class="radio-inline">
          <input
            :id="`${rkey}prop_false_` + pindex"
            v-model="currentValue"
            type="radio"
            :name="`${rkey}prop_` + pindex"
            value="false"
          />
          <plugin-prop-val :prop="prop" :value="'false'" />
        </label>
      </div>
    </template>
    <template
      v-else-if="prop.options && prop.options['displayType'] === 'DYNAMIC_FORM'"
    >
      <input :id="rkey" type="hidden" :name="prop.name" />

      <dynamic-form-plugin-prop
        :id="rkey"
        v-model="currentValue"
        :fields="modelValue"
        :has-options="hasAllowedValues()"
        :options="parseAllowedValues()"
        :element="rkey"
        :name="prop.name"
      ></dynamic-form-plugin-prop>
    </template>
    <template v-else>
      <label
        :class="
          'col-sm-2 control-label input-sm ' + (prop.required ? 'required' : '')
        "
        :for="`${rkey}prop_` + pindex"
        >{{ prop.title }}</label
      >
      <div v-if="prop.type === 'Select'" class="col-sm-10">
        <select
          v-if="!renderReadOnly"
          :id="`${rkey}prop_` + pindex"
          v-model="currentValue"
          :name="`${rkey}prop_` + pindex"
          class="form-control input-sm"
        >
          <option v-if="!prop.required" value>--None Selected--</option>
          <option v-for="opt in prop.allowed" :key="opt" :value="opt">
            <plugin-prop-val :prop="prop" :value="opt" />
          </option>
        </select>
        <select
          v-else
          :id="`${rkey}prop_` + pindex"
          v-model="currentValue"
          :name="`${rkey}prop_` + pindex"
          class="form-control input-sm"
          :disabled="true"
        >
          <option v-if="!prop.required" value>--None Selected--</option>
          <option v-for="opt in prop.allowed" :key="opt" :value="opt">
            <plugin-prop-val :prop="prop" :value="opt" />
          </option>
        </select>
      </div>
      <template v-else-if="prop.type === 'FreeSelect'">
        <div class="col-sm-5">
          <input
            v-if="!renderReadOnly"
            :id="`${rkey}prop_` + pindex"
            v-model="currentValue"
            :name="`${rkey}prop_` + pindex"
            size="100"
            type="text"
            class="form-control input-sm"
          />
          <input
            v-else
            :id="`${rkey}prop_` + pindex"
            v-model="currentValue"
            :name="`${rkey}prop_` + pindex"
            size="100"
            type="text"
            class="form-control input-sm"
            :disabled="true"
          />
        </div>
        <div class="col-sm-5">
          <select
            v-if="!renderReadOnly"
            v-model="currentValue"
            class="form-control input-sm"
          >
            <option v-for="opt in prop.allowed" :key="opt" :value="opt">
              <plugin-prop-val :prop="prop" :value="opt" />
            </option>
          </select>
          <select
            v-else
            v-model="currentValue"
            class="form-control input-sm"
            :disabled="true"
          >
            <option v-for="opt in prop.allowed" :key="opt" :value="opt">
              <plugin-prop-val :prop="prop" :value="opt" />
            </option>
          </select>
        </div>
      </template>
      <template v-else-if="prop.type === 'Options'">
        <div class="col-sm-10">
          <div :class="{ longlist: prop.allowed && prop.allowed.length > 20 }">
            <div
              v-for="(opt, oindex) in prop.allowed"
              :key="opt"
              class="checkbox"
            >
              <input
                v-if="!renderReadOnly"
                :id="`${rkey}opt_` + pindex + '_' + oindex"
                v-model="currentValue"
                type="checkbox"
                :value="opt"
              />
              <input
                v-else
                :id="`${rkey}opt_` + pindex + '_' + oindex"
                v-model="currentValue"
                type="checkbox"
                :value="opt"
                :disabled="true"
              />
              <label :for="`${rkey}opt_` + pindex + '_' + oindex"
                ><plugin-prop-val :prop="prop" :value="opt"
              /></label>
            </div>
          </div>
        </div>
      </template>
      <div v-else :class="inputColSize(prop)">
        <input
          v-if="['Integer', 'Long'].indexOf(prop.type) >= 0 && !renderReadOnly"
          :id="`${rkey}prop_` + pindex"
          v-model.number="currentValue"
          :name="`${rkey}prop_` + pindex"
          size="100"
          type="number"
          class="form-control input-sm"
        />
        <template
          v-else-if="
            prop.options && prop.options['displayType'] === 'MULTI_LINE'
          "
        >
          <textarea
            v-if="!renderReadOnly"
            :id="`${rkey}prop_` + pindex"
            v-model="currentValue"
            :name="`${rkey}prop_` + pindex"
            rows="10"
            cols="100"
            class="form-control input-sm"
            :class="contextAutocomplete ? 'context_var_autocomplete' : ''"
          ></textarea>
          <textarea
            v-else
            :id="`${rkey}prop_` + pindex"
            v-model="currentValue"
            :name="`${rkey}prop_` + pindex"
            rows="10"
            cols="100"
            class="form-control input-sm"
            :disabled="true"
          ></textarea>
        </template>
        <template
          v-else-if="prop.options && prop.options['displayType'] === 'CODE'"
        >
          <ace-editor
            :id="`${rkey}prop_` + pindex"
            v-model="currentValue"
            :name="`${rkey}prop_` + pindex"
            :lang="prop.options['codeSyntaxMode']"
            :code-syntax-selectable="
              prop.options['codeSyntaxSelectable'] === 'true' && !renderReadOnly
            "
            height="200"
            width="100%"
            :read-only="renderReadOnly"
          />
        </template>
        <template
          v-else-if="prop.options && prop.options['displayType'] === 'PASSWORD'"
        >
          <div v-if="!renderReadOnly">
            <input
              :id="`${rkey}prop_` + pindex"
              v-model="currentValue"
              :name="`${rkey}prop_` + pindex"
              size="100"
              type="password"
              autocomplete="new-password"
              class="form-control input-sm"
            />
          </div>
          <div v-else>
            <input
              :id="`${rkey}prop_` + pindex"
              v-model="currentValue"
              :name="`${rkey}prop_` + pindex"
              size="100"
              type="password"
              autocomplete="new-password"
              class="form-control input-sm"
              :disabled="true"
            />
          </div>
        </template>
        <template
          v-else-if="
            prop.options && prop.options['displayType'] === 'STATIC_TEXT'
          "
        >
          <span v-if="prop.options['staticTextContentType'] === 'text/html'">{{
            prop.staticTextDefaultValue
          }}</span>
          <span
            v-if="prop.options['staticTextContentType'] === 'text/markdown'"
            >{{ prop.staticTextDefaultValue }}</span
          >
          <span v-else>{{ prop.staticTextDefaultValue }}</span>
        </template>
        <template
          v-else-if="
            prop.options && prop.options['displayType'] === 'RUNDECK_JOB'
          "
        >
          <input
            :id="`${rkey}prop_` + pindex"
            :name="`${rkey}prop_` + pindex"
            readonly
            size="100"
            class="form-control input-sm"
            :title="currentValue"
            :value="jobName"
          />
        </template>
        <input
          v-else-if="renderReadOnly"
          :id="`${rkey}prop_` + pindex"
          v-model="currentValue"
          :name="`${rkey}prop_` + pindex"
          size="100"
          type="text"
          class="form-control input-sm"
          :disabled="true"
        />
        <input
          v-else
          :id="`${rkey}prop_` + pindex"
          v-model="currentValue"
          :name="`${rkey}prop_` + pindex"
          size="100"
          type="text"
          class="form-control input-sm"
          :class="contextAutocomplete ? 'context_var_autocomplete' : ''"
        />

        <TextAutocomplete
          v-if="contextAutocomplete && !aceEditorEnabled"
          v-model="currentValue"
          :target="'#' + rkey + 'prop_' + pindex"
          :data="jobContext"
          item-key="name"
          autocomplete-key="$"
        >
          <template #item="{ items, select, highlight }">
            <li v-for="(item, index) in items" :key="item.name">
              <a role="button" @click="select(item.name)">
                <span v-html="highlight(item)"></span> - {{ item.description }}
              </a>
            </li>
          </template>
        </TextAutocomplete>
      </div>
      <div
        v-if="
          prop.options && prop.options['selectionAccessor'] === 'PLUGIN_TYPE'
        "
        class="col-sm-5"
      >
        <select v-model="currentValue" class="form-control input-sm">
          <option disabled value>-- Select Plugin Type --</option>
          <option
            v-for="opt in selectorDataForName"
            :key="opt.key"
            :value="opt.value"
          >
            {{ opt.key }}
          </option>
        </select>
      </div>
      <div
        v-if="
          prop.options && prop.options['selectionAccessor'] === 'RUNDECK_JOB'
        "
        class="col-sm-5"
      >
        <job-config-picker
          v-model="currentValue"
          :btn-class="`btn-primary`"
        ></job-config-picker>
      </div>
      <div
        v-if="
          prop.options && prop.options['selectionAccessor'] === 'STORAGE_PATH'
        "
        class="col-sm-5"
      >
        <div v-if="useRunnerSelector === true">
          <ui-socket
            section="plugin-runner-key-selector"
            location="nodes"
            :event-bus="eventBus"
            :socket-data="{
              storageFilter: prop.options['storage-file-meta-filter'],
              allowUpload: true,
              readOnly: renderReadOnly,
              value: currentValue,
              handleUpdate: (val) => (currentValue = val),
            }"
          >
            <key-storage-selector
              v-model="currentValue"
              :storage-filter="prop.options['storage-file-meta-filter']"
              :allow-upload="true"
              :value="keyPath"
              :read-only="renderReadOnly"
            />
          </ui-socket>
        </div>
        <div v-else>
          <key-storage-selector
            v-model="currentValue"
            :storage-filter="prop.options['storage-file-meta-filter']"
            :allow-upload="true"
            :value="keyPath"
            :read-only="renderReadOnly"
          />
        </div>
      </div>
      <slot
        v-else-if="prop.options && prop.options['selectionAccessor']"
        name="accessors"
        :prop="prop"
        :input-values="inputValues"
        :accessor="prop.options['selectionAccessor']"
      ></slot>
    </template>

    <div v-if="prop.desc" class="col-sm-10 col-sm-offset-2 help-block">
      <plugin-details
        :description="prop.desc"
        :extended-css="extendedCss"
        description-css="more-info"
        markdown-container-css="m-0 p-0"
        inline-description
        allow-html
      >
        <template #extraDescriptionText>
          <div class="help-block">{{ prop.desc }}</div>
        </template>
      </plugin-details>
    </div>
    <div
      v-if="validation && !validation.valid && validation.errors[prop.name]"
      class="col-sm-10 col-sm-offset-2 text-warning"
    >
      {{ validation.errors[prop.name] }}
    </div>
  </div>
</template>
<script lang="ts">
import { defineComponent } from "vue";

import JobConfigPicker from "./JobConfigPicker.vue";
import KeyStorageSelector from "./KeyStorageSelector.vue";

import AceEditor from "../utils/AceEditor.vue";
import PluginPropVal from "./pluginPropVal.vue";
import { client } from "../../modules/rundeckClient";
import DynamicFormPluginProp from "./DynamicFormPluginProp.vue";
import TextAutocomplete from "../utils/TextAutocomplete.vue";
import type { PropType } from "vue";
import { getRundeckContext } from "../../rundeckService";
import { EventBus } from "@/library";
import UiSocket from "../utils/UiSocket.vue";
import PluginDetails from "@/library/components/plugins/PluginDetails.vue";
interface Prop {
  type: string;
  defaultValue: any;
  title: string;
  required: boolean;
  options: any;
  allowed: string;
  name: string;
  desc: string;
  staticTextDefaultValue: string;
}

export default defineComponent({
  components: {
    PluginDetails,
    DynamicFormPluginProp,
    AceEditor,
    JobConfigPicker,
    PluginPropVal,
    KeyStorageSelector,
    TextAutocomplete,
    UiSocket,
  },
  props: {
    prop: {
      type: Object as PropType<Prop>,
      required: true,
    },
    readOnly: {
      type: Boolean,
      default: false,
      required: false,
    },
    modelValue: {
      required: false,
      default: "",
    },
    useRunnerSelector: {
      type: Boolean,
      default: false,
      required: false,
    },
    inputValues: {
      type: Object,
      required: false,
    },
    validation: {
      type: Object as PropType<any>,
      required: false,
    },
    eventBus: {
      type: Object as PropType<typeof EventBus>,
      required: false,
    },
    rkey: {
      type: String,
      required: false,
      default: "",
    },
    pindex: {
      type: Number,
      required: false,
      default: 0,
    },
    descriptionCss: {
      type: String,
      default: "",
      required: false,
    },
    extendedCss: {
      type: String,
      default: "",
      required: false,
    },
    contextAutocomplete: {
      type: Boolean,
      default: false,
      required: false,
    },
    autocompleteCallback: {
      type: Function,
      required: false,
    },
    selectorData: {
      type: Object as PropType<any>,
      required: true,
    },
  },
  emits: ["update:modelValue", "pluginPropsMounted"],
  data() {
    return {
      jobName: "",
      keyPath: "",
      jobContext: [] as any,
      aceEditorEnabled: false,
      renderReadOnly: false,
    };
  },
  computed: {
    selectorDataForName(): any[] {
      return this.selectorData[this.prop.name];
    },
    currentValue: {
      get() {
        return this.modelValue;
      },
      set(val: any) {
        this.$emit("update:modelValue", val);
        this.setJobName(val);
      },
    },
  },
  watch: {
    currentValue: function (newval) {
      this.$emit("update:modelValue", newval);
      this.setJobName(newval);
    },
    value: function (newval) {
      this.currentValue = newval;
    },
    readOnly: function (newval) {
      this.renderReadOnly =
        newval ||
        (this.prop.options && this.prop.options["displayType"] === "READONLY");
    },
  },
  mounted() {
    this.$emit("pluginPropsMounted");
    this.setJobName(this.modelValue);
    if (getRundeckContext() && getRundeckContext().projectName) {
      this.keyPath = "keys/project/" + getRundeckContext().projectName + "/";
    }

    if (this.autocompleteCallback && this.contextAutocomplete) {
      const vars = this.autocompleteCallback(this.rkey + "prop_" + this.pindex);
      const jobContext = [] as any;
      vars.forEach((context: any) => {
        jobContext.push({
          name: context["value"],
          description: context["data"]["title"],
          category: context["data"]["category"],
        });
      });
      this.jobContext = jobContext;
    }

    if (this.prop.options && this.prop.options["displayType"] === "CODE") {
      this.aceEditorEnabled = true;
    }

    this.renderReadOnly =
      this.readOnly ||
      (this.prop.options && this.prop.options["displayType"] === "READONLY");
  },
  methods: {
    inputColSize(prop: any) {
      if (prop.options && prop.options["selectionAccessor"]) {
        return "col-sm-5";
      }
      return "col-sm-10";
    },
    setJobName(jobUuid: string) {
      if (
        jobUuid &&
        jobUuid.length > 0 &&
        this.prop.options &&
        this.prop.options["displayType"] === "RUNDECK_JOB"
      ) {
        client.jobInfoGet(jobUuid).then((response) => {
          if (response.name) {
            let output = "";
            if (response.group) output += response.group + "/";
            output += response.name + " (" + response.project + ")";
            this.jobName = output;
          }
        });
      }
    },
    parseAllowedValues() {
      return JSON.stringify(this.prop.allowed);
    },
    handleUpdate(val: any) {
      this.currentValue = val;
    },
    hasAllowedValues() {
      if (this.prop.allowed != null) {
        return "true";
      }
      return "false";
    },
  },
});
</script>
<style scoped lang="scss">
@import "~vue3-markdown/dist/style.css";

.longlist {
  max-height: 500px;
  overflow-y: auto;
}
</style>
