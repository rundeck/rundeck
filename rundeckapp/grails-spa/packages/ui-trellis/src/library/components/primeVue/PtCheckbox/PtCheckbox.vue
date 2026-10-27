<template>
  <Checkbox
    v-model="value"
    class="pt-checkbox"
    binary
    :input-id="inputId"
    :name="name"
    :disabled="disabled"
    :readonly="readonly"
    :invalid="invalid"
    :indeterminate="indeterminate"
    :aria-label="ariaLabel"
    :aria-labelledby="ariaLabelledby"
    :pt="inputPassThrough"
    data-testid="pt-checkbox"
  >
    <template v-if="$slots.icon" #icon="slotProps">
      <slot name="icon" v-bind="slotProps" />
    </template>
  </Checkbox>
</template>

<script lang="ts">
import { defineComponent } from "vue";
import Checkbox from "primevue/checkbox";

/** Rundeck-styled binary checkbox control without item-level label or layout. */
export default defineComponent({
  name: "PtCheckbox",
  components: { Checkbox },
  props: {
    /** Whether the checkbox is checked. */
    modelValue: {
      type: Boolean,
      default: false,
    },
    /** Native input id used to associate an external label. */
    inputId: {
      type: String,
      default: undefined,
    },
    /** Native form field name. */
    name: {
      type: String,
      default: undefined,
    },
    disabled: {
      type: Boolean,
      default: false,
    },
    readonly: {
      type: Boolean,
      default: false,
    },
    invalid: {
      type: Boolean,
      default: false,
    },
    indeterminate: {
      type: Boolean,
      default: false,
    },
    ariaLabel: {
      type: String,
      default: undefined,
    },
    ariaLabelledby: {
      type: String,
      default: undefined,
    },
  },
  emits: ["update:modelValue"],
  computed: {
    value: {
      get(): boolean {
        return this.modelValue;
      },
      set(value: boolean) {
        this.$emit("update:modelValue", value);
      },
    },
    /** Applies a stable test selector to the hidden native checkbox. */
    inputPassThrough(): Record<string, unknown> {
      return {
        input: {
          "data-testid": "pt-checkbox-input",
        },
      };
    },
  },
});
</script>

<style scoped lang="scss">
.pt-checkbox {
  --p-checkbox-width: calc(var(--sizes-px) * 16);
  --p-checkbox-height: calc(var(--sizes-px) * 16);
  --p-checkbox-border-radius: var(--radii-sm);
  --p-checkbox-icon-size: calc(var(--sizes-px) * 14);
  --p-checkbox-background: var(--colors-white);
  --p-checkbox-border-color: var(--colors-gray-600);
  --p-checkbox-hover-background: var(--colors-gray-200);
  --p-checkbox-hover-border-color: var(--colors-gray-800);
  --p-checkbox-checked-background: var(--colors-blue-600);
  --p-checkbox-checked-border-color: var(--colors-blue-600);
  --p-checkbox-checked-hover-background: var(--colors-blue-700);
  --p-checkbox-checked-hover-border-color: var(--colors-blue-700);
  --p-checkbox-icon-checked-color: var(--colors-white);
  --p-checkbox-icon-checked-hover-color: var(--colors-white);
  --p-checkbox-focus-border-color: var(--colors-blue-500);
  --p-checkbox-checked-focus-border-color: var(--colors-blue-500);
  --p-checkbox-focus-ring-shadow: 0 0 0 2.8px var(--colors-blue-100);
  --p-checkbox-invalid-border-color: var(--colors-red-600);
  --p-checkbox-disabled-background: var(--colors-gray-200);
  --p-checkbox-checked-disabled-background: var(--colors-gray-400);
  --p-checkbox-checked-disabled-border-color: var(--colors-gray-400);
  --p-checkbox-icon-disabled-color: var(--colors-gray-600);
}
</style>
