<template>
  <span>
    <span v-if="prop.type === 'Boolean'" class="configpair">
      <template
        v-if="
          innerValue === 'true' ||
          (prop.defaultValue === 'true' && innerValue === 'false')
        "
      >
        <span :title="prop.desc">{{ prop.title }}: </span>
        <span
          v-if="innerValue === 'true'"
          :class="[
            (prop.options && prop.options['booleanTrueDisplayValueClass']) ||
              'text-success',
            'copiable-text',
          ]"
          @click="copyText(innerValue)"
        >
          <plugin-prop-val :prop="prop" :value="innerValue" />
          <i v-if="allowCopy" class="pi pi-copy copy-icon"></i>
        </span>
        <span
          v-else-if="innerValue === 'false'"
          :class="[
            (prop.options && prop.options['booleanFalseDisplayValueClass']) ||
              'text-success',
            'copiable-text',
          ]"
          @click="copyText(innerValue)"
        >
          <plugin-prop-val :prop="prop" :value="innerValue" />
          <i v-if="allowCopy" class="pi pi-copy copy-icon"></i>
        </span>
      </template>
    </span>
    <span v-else-if="prop.type === 'Integer'" class="configpair">
      <span :title="prop.desc" data-testid="integer-prop-title">
        {{ prop.title }}:
      </span>
      <span
        style="font-family: Courier, monospace"
        data-testid="integer-prop-value"
      >
        {{ innerValue }}
      </span>
    </span>
    <span
      v-else-if="['Options', 'Select', 'FreeSelect'].indexOf(prop.type) >= 0"
      class="configpair"
    >
      <span>
        <span :title="prop.desc">{{ prop.title }}:</span>
        <template v-if="prop.type !== 'Options'">
          <span class="text-success copiable-text" @click="copyText(innerValue)">
            <plugin-prop-val :prop="prop" :value="innerValue" />
            <i v-if="allowCopy" class="pi pi-copy copy-icon"></i>
          </span>
        </template>
        <template v-else>
          <span v-if="typeof value === 'string'">
            <span
                v-for="optval in innerValue.split(/, */)"
                :key="optval"
                class="text-success copiable-text"
                @click="copyText(optval)"
            >
              <i
                  v-if="!(prop.options && prop.options['valueDisplayType'])"
                  class="glyphicon glyphicon-ok-circle"
              ></i>
              <plugin-prop-val :prop="prop" :value="optval" />
              <i v-if="allowCopy" class="pi pi-copy copy-icon"></i>
            </span>
          </span>
          <span v-else-if="typeof value !== 'string' && innerValue.length > 0">
            <span
                v-for="optval in innerValue"
                :key="optval"
                class="text-success copiable-text"
                @click="copyText(optval)"
            >
              <i
                  v-if="!(prop.options && prop.options['valueDisplayType'])"
                  class="glyphicon glyphicon-ok-circle"
              ></i>
              <plugin-prop-val :prop="prop" :value="optval" />
              <i v-if="allowCopy" class="pi pi-copy copy-icon"></i>
            </span>
          </span>
        </template>
      </span>

    </span>
    <span v-else class="configpair">
      <template v-if="prop.options && prop.options['displayType'] === 'CODE'">
        <expandable>
          <template #label>
            <span :title="prop.desc">{{ prop.title }}:</span>
            <span class="text-info">
              {{ innerValue.split(/\r?\n/).length }} lines
            </span>
          </template>
          <ace-editor
            v-model="innerValue"
            :lang="prop.options['codeSyntaxMode']"
            height="200"
            width="100%"
            :read-only="true"
          />
        </expandable>
      </template>
      <template
        v-else-if="prop.options && prop.options['displayType'] === 'MULTI_LINE'"
      >
        <expandable>
          <template #label
            ><span :title="prop.desc">{{ prop.title }}:</span>
            <span class="text-info"
              >{{ `${innerValue}`.split(/\r?\n/).length }} lines</span
            ></template
          >
          <ace-editor
            v-model="innerValue"
            height="200"
            width="100%"
            :read-only="true"
          />
        </expandable>
      </template>
      <template
        v-else-if="
          prop.options && prop.options['displayType'] === 'DYNAMIC_FORM'
        "
      >
        <div class="customattributes"></div>

        <span
          v-for="(custom, index) in getCustomValues()"
          :key="`customValueForConfigPair${index}`"
          class="configpair"
          data-testid="configpair"
        >
          <span title="">{{ custom.label }}:</span>
          <span
            class="text-success copiable-text"
            @click="copyText(custom.value)"
          >
            {{ custom.value }}
            <i v-if="allowCopy" class="pi pi-copy copy-icon"></i>
          </span>
        </span>
      </template>
      <span class="" v-else>
        <span :title="prop.desc">{{ prop.title }}:</span>
        <span
          v-if="prop.options && prop.options['displayType'] === 'PASSWORD'"
          class="text-success copiable-text"
          @click="copyText(innerValue)"
          :title="allowCopy ? 'Click to copy password' : ''"
        >
          &bull;&bull;&bull;&bull;&bull;&bull;&bull;&bull;&bull;&bull;&bull;&bull;
          <i v-if="allowCopy" class="pi pi-copy copy-icon"></i>
        </span>
        <span
          v-else
          class="text-success copiable-text"
          @click="copyText(innerValue)"
        >
          {{ innerValue }}
          <i v-if="allowCopy" class="pi pi-copy copy-icon"></i>
        </span>
      </span>
    </span>
  </span>
</template>
<script lang="ts">
import { defineComponent } from "vue";
import Expandable from "../utils/Expandable.vue";
import AceEditor from "../utils/AceEditor.vue";
import PluginPropVal from "./pluginPropVal.vue";
import { CopyToClipboard } from "../../utilities/Clipboard";
export default defineComponent({
  components: {
    Expandable,
    AceEditor,
    PluginPropVal,
  },
  props: {
    value: {
      type: [Boolean, String],
      default: "",
      required: false,
    },
    prop: {
      type: Object,
      required: true,
    },
    allowCopy: {
      type: Boolean,
      default: false,
    },
  },
  data() {
    return {
      innerValue: this.value as string,
    };
  },
  methods: {
    getCustomValues(): any[] {
      if (this.innerValue !== "") {
        return JSON.parse(`${this.innerValue}`);
      }
      return [];
    },
    async copyText(text: string) {
      try {
        await CopyToClipboard(text);
      } catch (error) {
        console.error("Failed to copy text:", error);
      }
    },
  },
});
</script>
<style>
.customattributes {
  border-bottom: 1px solid #eeeeee;
  margin-top: 10px;
  margin-bottom: 10px;
}

.copiable-text {
  align-items: center;
  cursor: pointer;
  display: flex;
  gap: 2px;
  font-weight: 400 !important;
  transition: background-color 0.2s ease;
  position: relative;
}

.copiable-text:hover {
  background-color: var(--colors-cardHoverBackgroundOnLight);
  padding: 2px 4px;
  margin: -2px -4px;
}

.copy-icon {
  opacity: 0;
  font-size: 12px;
  color: var(--colors-gray-800);
  transition: opacity 0.2s ease;
  pointer-events: none;
}

.copiable-text:hover .copy-icon {
  opacity: 1;
}

.copiable-text:active {
  background-color: rgba(40, 167, 69, 0.2);
}
</style>
