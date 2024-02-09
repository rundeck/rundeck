<template>
  <span>
    <span v-if="prop.type === 'Boolean'" class="configpair">
      <template
        v-if="
          value === 'true' ||
          value === true ||
          (prop.defaultValue === 'true' &&
            (value === 'false' || value === false))
        "
      >
        <span :title="prop.desc">{{ prop.title }}: </span>
        <span
          v-if="value === 'true' || value === true"
          :class="
            (prop.options && prop.options['booleanTrueDisplayValueClass']) ||
            'text-success'
          "
        >
          <plugin-prop-val :prop="prop" :value="value" />
        </span>
        <span
          v-if="value === 'false' || value === false"
          :class="
            (prop.options && prop.options['booleanFalseDisplayValueClass']) ||
            'text-success'
          "
        >
          <plugin-prop-val :prop="prop" :value="value" />
        </span>
      </template>
    </span>
    <span v-else-if="prop.type === 'Integer'" class="configpair">
      <span :title="prop.desc">{{ prop.title }}:</span>
      <span style="font-family: Courier, monospace">{{ value }}</span>
    </span>
    <span
      v-else-if="['Options', 'Select', 'FreeSelect'].indexOf(prop.type) >= 0"
      class="configpair"
    >
      <span :title="prop.desc">{{ prop.title }}:</span>
      <template v-if="prop.type !== 'Options'">
        <span class="text-success"
          ><plugin-prop-val :prop="prop" :value="value"
        /></span>
      </template>
      <template v-else>
        <span v-if="typeof value === 'string'">
          <span
            v-for="optval in value.split(/, */)"
            :key="optval"
            class="text-success"
          >
            <i
              v-if="!(prop.options && prop.options['valueDisplayType'])"
              class="glyphicon glyphicon-ok-circle"
            ></i>
            <plugin-prop-val :prop="prop" :value="optval" />
          </span>
        </span>
        <span v-else-if="typeof value === 'string' && `${value}`.length > 0">
          <span v-for="optval in `${value}`" :key="optval" class="text-success">
            <i
              v-if="!(prop.options && prop.options['valueDisplayType'])"
              class="glyphicon glyphicon-ok-circle"
            ></i>
            <plugin-prop-val :prop="prop" :value="optval" />
          </span>
        </span>
      </template>
    </span>
    <span v-else class="configpair">
      <template v-if="prop.options && prop.options['displayType'] === 'CODE'">
        <expandable>
          <template #label
            ><span :title="prop.desc">{{ prop.title }}:</span>
            <span class="text-info"
              >{{ `${value}`.split(/\r?\n/).length }} lines</span
            ></template
          >
          <ace-editor
            v-model="value as string"
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
              >{{ `${value}`.split(/\r?\n/).length }} lines</span
            ></template
          >
          <ace-editor
            v-model="value as string"
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

        <span v-for="custom in getCustomValues()" class="configpair">
          <span title="">{{ custom.label }}:</span>
          <span class="text-success"> {{ custom.value }}</span>
        </span>
      </template>
      <span v-else>
        <span :title="prop.desc">{{ prop.title }}:</span>
        <span
          v-if="prop.options && prop.options['displayType'] === 'PASSWORD'"
          class="text-success"
          >&bull;&bull;&bull;&bull;&bull;&bull;&bull;&bull;&bull;&bull;&bull;&bull;</span
        >
        <span v-else class="text-success">{{ value }}</span>
      </span>
    </span>
  </span>
</template>
<script lang="ts">
import { defineComponent, PropType } from "vue";
import Expandable from "../utils/Expandable.vue";
import AceEditor from "../utils/AceEditor.vue";
import PluginPropVal from "./pluginPropVal.vue";
export default defineComponent({
  components: {
    Expandable,
    AceEditor,
    PluginPropVal,
  },
  props: {
    value: {
      type: [Boolean, String],
      required: false,
    },
    prop: {
      type: Object,
      required: true,
    },
  },
  methods: {
    getCustomValues(): any[] {
      if (this.value !== null) {
        const data = `${this.value}`;
        const json = JSON.parse(data);
        return json;
      }
      return [];
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
</style>
