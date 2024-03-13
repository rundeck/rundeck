<template>
  <span class="optview">
    <span
      class="optdetail opt-detail-ext flow-h"
      :title="editable ? $t('option.click.to.edit.title') : ''"
    >
      <template v-if="option.type == 'file'">
        <i class="glyphicon glyphicon-file"></i>
      </template>
      <span
        class="optdetail_name"
        :class="{ required: option.required }"
        :title="
          (option.description || '') +
          (option?.required ? $t('option.view.required.title') : '')
        "
        >{{ option.name }}</span
      >
      <span class="optdetail_default_value">
        <span
          :title="displayDefaultValue"
          :class="{ truncatedtext: displayDefaultValue.length > 20 }"
        >
          {{ displayDefaultValueTruncated }}
        </span>
        {{ option.multivalued ? "(+)" : "" }}
        <template v-if="option.secure && option.storagePath">
          <i class="glyphicon glyphicon-lock"></i>
        </template>
      </span>
      <span class="opt-desc">
        {{ truncatedDescription }}
      </span>
    </span>
    <template v-if="option.values && option.values.length > 0">
      <popover trigger="hover" placement="bottom">
        <span class="valuesSet" data-role="trigger">
          <span class="valueslist">
            {{ $tc("option.values.c", option.values.length) }}
          </span>
        </span>
        <template #popover>
          <div class="info note">
            {{ $t("option.view.allowedValues.label") }}
          </div>
          <span v-for="(val, i) in option.values">
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
    displayDefaultValue() {
      return this.option.secure
        ? ""
        : this.option.value
          ? this.option.value
          : "";
    },
    displayDefaultValueTruncated() {
      let val = this.displayDefaultValue;
      if (val.length > 20) {
        return val.substring(0, 20) + "...";
      } else {
        return val;
      }
    },
    truncatedDescription() {
      return this.option.description
        ? this.option.description.substring(0, 100)
        : "";
    },
  },
});
</script>

<style scoped lang="scss">
.flow > * + * {
  margin-top: var(--spacing-2);
}
.flow-h > * + * {
  margin-left: var(--spacing-2);
}
.enforceSet {
  margin-left: var(--spacing-4);
}
</style>
