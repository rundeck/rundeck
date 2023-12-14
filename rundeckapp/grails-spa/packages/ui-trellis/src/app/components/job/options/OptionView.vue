<template>
  <span id="opt_${rkey}" class="optview">
    <span
      class="optdetail opt-detail-ext ${edit?'autohilite autoedit':''}"
      title="${edit?'Click to edit':''}"
    >
      <template v-if="option.optionType == 'file'">
        <i class="glyphicon glyphicon-file"></i>
      </template>
      <span
        class="${option?.required ? 'required' : ''}"
        title="${enc(attr:option?.description)}${option?.required ? ' (Required)' : ''}"
        >{{ option.name }}</span
      >
      <span class="">
        <span max="20" showtitle="true">
          {{option.secureInput && option.defaultValue?'****':option.defaultValue}}
        </span>{{ option.multivalued ? '(+)' : '' }}
        <template v-if="option.secureInput && option.defaultStoragePath">
          <i class="glyphicon glyphicon-lock"></i>
        </template>
      </span>
      <span class="opt-desc">
        <!-- TODO strip -->
        {{ option.description }}
      </span>
    </span>
    <template v-if="optionValuesArray.length>0">
      <popover trigger="hover" placement="bottom">
        <span class="valuesSet" data-role="trigger">
          <span class="valueslist">
            {{ $tc('option.values.c', optionValuesArray.length ) }}
          </span>
        </span>
        <template #popover>
          <div class="info note">Allowed Values</div>
          <span v-for="(val,i) in optionValuesArray">
            {{ 0 != i ? ', ' : '' }}
            <span class="valueItem">{{ val }}</span>
          </span>
        </template>

      </popover>
    </template>
    <template v-else-if="option.realValuesUrl">
      <span class="valuesSet">
        <span
          class="valuesUrl"
          :title="`Values loaded from Remote URL: ${option.realValuesUrl}`"
          >URL</span
        >
      </span>
    </template>

    <template v-if="option.enforced">
      <span class="enforceSet">
        <span class="enforced" title="Input must be one of the allowed values"
          >Strict</span
        >
      </span>
    </template>
    <template v-else-if="option.regex">
      <span class="enforceSet">
        <popover>
          <span class="regex" data-role="trigger">{{ option.regex }}</span>
          <div class="info note">Values must match the regular expression:</div>
          <code>{{ option.regex }}</code>
        </popover>
      </span>
    </template>
    <template v-else>
      <span class="enforceSet">
        <span class="any" title="No restrictions on input value">None</span>
      </span>
    </template>
  </span>
</template>

<script lang="ts">
import { defineComponent } from "vue";

export default defineComponent({
  name: "OptionView",
  props: {
    option: {
      type: Object,
      required: true,
      default: () => ({}),
    },
    edit: {
      type: Boolean,
      default: true,
    },
  },
  computed:{
    optionValuesArray(){
      return this.option.valuesList ? this.option.valuesList.split(',') : [];
    }
  }
});
</script>

<style scoped lang="scss"></style>
