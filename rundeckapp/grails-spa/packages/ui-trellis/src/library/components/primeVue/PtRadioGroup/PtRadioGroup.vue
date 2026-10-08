<template>
  <div
    class="pt-radio-group"
    role="radiogroup"
    data-testid="pt-radio-group"
    :aria-labelledby="legend ? legendId : undefined"
    :aria-label="legend ? undefined : ariaLabel"
    :aria-invalid="invalid || undefined"
  >
    <span
      v-if="legend"
      :id="legendId"
      class="pt-radio-group__legend"
      data-testid="pt-radio-group-legend"
    >
      {{ legend }}
    </span>
    <div
      v-for="(option, index) in options"
      :key="`${baseId}-${index}`"
      class="pt-radio-group__item"
      data-testid="pt-radio-group-item"
    >
      <RadioButton
        :model-value="modelValue"
        :input-id="`${baseId}-${index}`"
        :name="name || baseId"
        :value="getValue(option)"
        :disabled="disabled || isOptionDisabled(option)"
        :invalid="invalid"
        :data-testid="`pt-radio-group-radio-${index}`"
        @update:model-value="onUpdateModelValue"
      />
      <label
        class="pt-radio-group__label"
        :for="`${baseId}-${index}`"
        data-testid="pt-radio-group-label"
      >
        {{ getLabel(option) }}
      </label>
    </div>
  </div>
</template>

<script lang="ts">
import { defineComponent, useId, type PropType } from "vue";
import RadioButton from "primevue/radiobutton";

/** A labelled group of radio buttons; the whole group is one tab stop. */
export default defineComponent({
  name: "PtRadioGroup",
  components: { RadioButton },
  props: {
    /** The selected option's value (v-model). */
    modelValue: {
      type: [String, Number, Boolean, Object] as PropType<any>,
      default: undefined,
    },
    /** Options to choose from; simple values or objects. */
    options: {
      type: Array as PropType<any[]>,
      required: true,
    },
    /** Property name or getter for an option's label. */
    optionLabel: {
      type: [String, Function] as PropType<string | ((option: any) => string)>,
      default: "label",
    },
    /** Property name or getter for an option's value. */
    optionValue: {
      type: [String, Function] as PropType<string | ((option: any) => any)>,
      default: "value",
    },
    /** Property name or getter for an option's disabled flag. */
    optionDisabled: {
      type: [String, Function] as PropType<string | ((option: any) => boolean)>,
      default: "disabled",
    },
    /** Text that names the group, shown above the options. */
    legend: {
      type: String,
      default: undefined,
    },
    /** Accessible name for the group when no legend is shown. */
    ariaLabel: {
      type: String,
      default: undefined,
    },
    /** Native name shared by the radios; generated when omitted. */
    name: {
      type: String,
      default: undefined,
    },
    disabled: {
      type: Boolean,
      default: false,
    },
    invalid: {
      type: Boolean,
      default: false,
    },
  },
  emits: ["update:modelValue", "change"],
  setup() {
    return { baseId: `pt-radio-group-${useId()}` };
  },
  computed: {
    legendId(): string {
      return `${this.baseId}-legend`;
    },
  },
  mounted() {
    if (!this.legend && !this.ariaLabel) {
      console.warn(
        "PtRadioGroup: provide a legend or ariaLabel so the radiogroup has an accessible name.",
      );
    }
  },
  methods: {
    resolve(option: any, field: string | ((option: any) => any)) {
      if (typeof field === "function") return field(option);
      return option !== null && typeof option === "object"
        ? option[field]
        : undefined;
    },
    getLabel(option: any): string {
      const label = this.resolve(option, this.optionLabel);
      return String(label ?? option);
    },
    getValue(option: any): any {
      const value = this.resolve(option, this.optionValue);
      return value === undefined ? option : value;
    },
    isOptionDisabled(option: any): boolean {
      return !!this.resolve(option, this.optionDisabled);
    },
    onUpdateModelValue(value: any) {
      this.$emit("update:modelValue", value);
      this.$emit("change", value);
    },
  },
});
</script>

<style scoped lang="scss">
.pt-radio-group {
  display: flex;
  flex-direction: column;
  gap: var(--sizes-3);
  font-family: Inter, var(--fonts-body2);
  font-size: 14px;
  line-height: 21px;
  color: var(--colors-gray-800);

  // Radio control per the Figma "Radio / Control" spec (16px control, 6px dot).
  --p-radiobutton-width: var(--sizes-6);
  --p-radiobutton-height: var(--sizes-6);
  --p-radiobutton-icon-size: var(--sizes-2\.5);
  --p-radiobutton-background: var(--colors-white);
  --p-radiobutton-border-color: var(--colors-gray-600);
  --p-radiobutton-hover-border-color: var(--colors-gray-600);
  --p-radiobutton-checked-background: var(--colors-blue-600);
  --p-radiobutton-checked-border-color: var(--colors-blue-600);
  --p-radiobutton-checked-hover-background: var(--colors-blue-600);
  --p-radiobutton-checked-hover-border-color: var(--colors-blue-600);
  --p-radiobutton-icon-checked-color: var(--colors-white);
  --p-radiobutton-icon-checked-hover-color: var(--colors-white);
  --p-radiobutton-focus-border-color: var(--colors-blue-500);
  --p-radiobutton-checked-focus-border-color: var(--colors-blue-500);
  --p-radiobutton-focus-ring-shadow: 0 0 0 2.8px var(--colors-blue-100);
  --p-radiobutton-invalid-border-color: var(--colors-red-500);
  --p-radiobutton-disabled-background: var(--colors-gray-400);
  --p-radiobutton-checked-disabled-border-color: var(--colors-gray-400);
  --p-radiobutton-icon-disabled-color: var(--colors-gray-600);

  &__item {
    display: flex;
    align-items: center;
    gap: var(--sizes-3);
    padding: var(--sizes-0\.5) 0;
  }

  &__label {
    margin: 0;
    font-weight: var(--fontWeights-regular);
    cursor: pointer;
  }
}
</style>
