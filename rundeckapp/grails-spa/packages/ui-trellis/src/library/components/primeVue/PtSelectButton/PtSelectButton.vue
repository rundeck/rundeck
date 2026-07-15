<template>
  <SelectButton
    data-testid="pt-select-button"
    :model-value="modelValue"
    :options="options"
    :option-label="optionLabel"
    :option-value="optionValue"
    :option-disabled="optionDisabled"
    :multiple="multiple"
    :fluid="fluid"
    :disabled="disabled"
    :invalid="invalid"
    :data-key="dataKey"
    :allow-empty="allowEmpty"
    :name="name"
    :aria-labelledby="ariaLabelledby"
    @update:model-value="onUpdateModelValue"
  />
</template>

<script lang="ts">
import { defineComponent, type PropType } from "vue";
import SelectButton from "primevue/selectbutton";

export default defineComponent({
  name: "PtSelectButton",
  // eslint-disable-next-line vue/no-reserved-component-names
  components: { SelectButton },
  props: {
    modelValue: {
      type: [String, Number, Object, Array] as PropType<any>,
      default: undefined,
    },
    options: {
      type: Array as PropType<any[]>,
      required: true,
    },
    optionLabel: {
      type: [String, Function] as PropType<string | ((option: any) => string)>,
      default: undefined,
    },
    optionValue: {
      type: [String, Function] as PropType<string | ((option: any) => any)>,
      default: undefined,
    },
    optionDisabled: {
      type: [String, Function] as PropType<string | ((option: any) => boolean)>,
      default: undefined,
    },
    multiple: {
      type: Boolean,
      default: false,
    },
    size: {
      type: String,
      default: undefined,
      validator: (val: string) => val === undefined || ["small", "large"].includes(val),
    },
    fluid: {
      type: Boolean,
      default: false,
    },
    disabled: {
      type: Boolean,
      default: false,
    },
    invalid: {
      type: Boolean,
      default: false,
    },
    dataKey: {
      type: String,
      default: undefined,
    },
    allowEmpty: {
      type: Boolean,
      default: true,
    },
    name: {
      type: String,
      default: undefined,
    },
    ariaLabelledby: {
      type: String,
      default: undefined,
    },
  },
  emits: ["update:modelValue", "change"],
  methods: {
    onUpdateModelValue(value: any) {
      this.$emit("update:modelValue", value);
      this.$emit("change", value);
    },
  },
});
</script>

<style lang="scss">
.p-selectbutton .p-togglebutton {
  // Unselected state - based on Figma design
  background: var(--colors-white);
  border-color: var(--colors-gray-300-original);
  color: var(--colors-gray-600);
  padding: 10px 17px;

  &:hover {
    background: var(--colors-gray-100);
    border-color: var(--colors-gray-300-original);
  }

  &:active {
    background: var(--colors-gray-200);
    border-color: var(--colors-gray-300-original);
  }

  &-checked {
    background: var(--colors-primaryButtonOnLight);
    border-color: var(--colors-primaryButtonOnLight);
    color: var(--colors-white);

    &:hover {
      background: var(--colors-blue-600);
      border-color: var(--colors-blue-600);
    }


    &:active {
      background: var(--colors-blue-700);
      border-color: var(--colors-blue-700);

    }
  }

  &-label {
    font-weight: var(--fontWeights-bold);
    font-size: 14px;
    line-height: 17px;
  }
}
</style>
