<template>
  <ToggleButton
    :model-value="modelValue"
    :on-label="onLabel"
    :off-label="offLabel"
    :on-icon="onIcon"
    :off-icon="offIcon"
    :size="size"
    :fluid="fluid"
    :disabled="disabled"
    :invalid="invalid"
    :aria-label="ariaLabel"
    :aria-labelledby="ariaLabelledby"
    @update:model-value="onUpdateModelValue"
  />
</template>

<script lang="ts">
import { defineComponent } from "vue";
import ToggleButton from "primevue/togglebutton";

export default defineComponent({
  name: "PtToggleButton",
  // eslint-disable-next-line vue/no-reserved-component-names
  components: { ToggleButton },
  props: {
    modelValue: {
      type: Boolean,
      default: false,
    },
    onLabel: {
      type: String,
      default: "On",
    },
    offLabel: {
      type: String,
      default: "Off",
    },
    onIcon: {
      type: String,
      default: undefined,
    },
    offIcon: {
      type: String,
      default: undefined,
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
  methods: {
    onUpdateModelValue(value: boolean) {
      this.$emit("update:modelValue", value);
    },
  },
});
</script>

<style lang="scss">
.p-togglebutton {
  background: var(--colors-primaryButtonOnLight);
  border-color: var(--colors-primaryButtonOnLight);
  color: var(--colors-white);

  &:hover {
    background: var(--colors-blue-600);
    border-color: var(--colors-blue-600);
  }

  &:focus-visible,
  &:focus {
    box-shadow: none;
    outline: 1px solid var(--colors-blue-600);
    outline-offset: 2px;
  }

  &:active {
    background: var(--colors-blue-700);
    border-color: var(--colors-blue-700);

    &:focus-visible,
    &:focus {
      outline: 1px solid var(--colors-blue-700);
    }
  }
}

.p-togglebutton-label {
  font-weight: var(--fontWeights-medium);
}
</style>
