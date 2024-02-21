<template>
  <span class="optview">
    <span
      class="optdetail opt-detail-ext"
      :title="editable ? $t('option.click.to.edit.title') : ''"
    >
      <template v-if="option.optionType == 'file'">
        <i class="glyphicon glyphicon-file"></i>
      </template>
      <span
        :class="{ required: option.required }"
        :title="
          (option.description || '') +
          (option?.required ? $t('option.view.required.title') : '')
        "
        >{{ option.name }}</span
      >
      <span class="">
        <span
          :title="displayDefaultValue"
          :class="{ truncatedtext: displayDefaultValue.length > 20 }"
        >
          {{ displayDefaultValueTruncated }}
        </span>
        {{ option.multivalued ? "(+)" : "" }}
        <template v-if="option.secureInput && option.defaultStoragePath">
          <i class="glyphicon glyphicon-lock"></i>
        </template>
      </span>
      <span class="opt-desc">
        {{ option.description ? option.description.substring(0, 100) : "" }}
      </span>
    </span>
    <template v-if="optionValuesArray.length > 0">
      <popover trigger="hover" placement="bottom">
        <span class="valuesSet" data-role="trigger">
          <span class="valueslist">
            {{ $tc("option.values.c", optionValuesArray.length) }}
          </span>
        </span>
        <template #popover>
          <div class="info note">
            {{ $t("option.view.allowedValues.label") }}
          </div>
          <span v-for="(val, i) in optionValuesArray">
            {{ 0 != i ? ", " : "" }}
            <span class="valueItem">{{ val }}</span>
          </span>
        </template>
      </popover>
    </template>
    <template v-else-if="option.valuesUrl">
      <span class="valuesSet">
        <span
          class="valuesUrl"
          :title="$t('option.view.valuesUrl.title', [option.valuesUrl])"
          >{{ $t("option.view.valuesUrl.placeholder") }}</span
        >
      </span>
    </template>

    <template v-if="option.enforced">
      <span class="enforceSet">
        <span class="enforced" :title="$t('option.view.enforced.title')">
          >{{ $t("option.view.enforced.placeholder") }}</span
        >
      </span>
    </template>
    <template v-else-if="option.regex">
      <span class="enforceSet">
        <popover>
          <span class="regex" data-role="trigger">{{ option.regex }}</span>
          <div class="info note">{{ $t("option.view.regex.info.note") }}</div>
          <code>{{ option.regex }}</code>
        </popover>
      </span>
    </template>
    <template v-else>
      <span class="enforceSet">
        <span class="any" :title="$t('option.view.notenforced.title')">{{
          $t("option.view.notenforced.placeholder")
        }}</span>
      </span>
    </template>
  </span>
</template>

<script lang="ts">
import { JobOption } from "@/library/types/jobs/JobEdit";
import { defineComponent, PropType } from "vue";

export default defineComponent({
  name: "OptionView",
  props: {
    option: {
      type: Object as PropType<JobOption>,
      required: true,
    },
    editable: {
      type: Boolean,
      default: true,
    },
  },
  computed: {
    optionValuesArray() {
      return this.option.valuesList ? this.option.valuesList.split(",") : [];
    },
    displayDefaultValue() {
      return this.option.secureInput && this.option.defaultValue
        ? "****"
        : this.option.defaultValue || "";
    },
    displayDefaultValueTruncated() {
      let val = this.displayDefaultValue;
      if (val.length > 20) {
        val = val.substring(0, 20) + "...";
      } else {
        return val;
      }
    },
  },
});
</script>

<style scoped lang="scss"></style>
