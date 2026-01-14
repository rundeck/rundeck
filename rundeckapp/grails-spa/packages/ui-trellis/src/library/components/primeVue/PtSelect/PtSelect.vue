<template>
  <Select
    v-model="modelValue"
    :options="options"
    :placeholder="placeholder"
    :disabled="disabled"
    :invalid="invalid"
    :filter="filter"
    :filter-match-mode="filterMatchMode"
    :filter-fields="filterFields"
    :editable="editable"
    :highlight-on-select="highlightOnSelect"
    :show-clear="showClear"
    :reset-filter-on-hide="resetFilterOnHide"
    :scroll-height="scrollHeight"
    :auto-filter-focus="autoFilterFocus"
    :select-on-focus="selectOnFocus"
    :focus-on-hover="focusOnHover"
    :name="name"
    :input-id="inputId"
    :data-key="dataKey"
    :aria-label="ariaLabel"
    :aria-labelledby="ariaLabelledby"
    :loading="loading"
    :loading-icon="loadingIcon"
    :dropdown-icon="dropdownIcon"
    :append-to="appendTo"
    @update:model-value="onUpdateModelValue"
    @focus="onFocus"
    @blur="onBlur"
    @show="onShow"
    @hide="onHide"
    @filter="onFilter"
  >
    <template v-if="$slots.value" #value="slotProps">
      <slot name="value" v-bind="slotProps" />
    </template>
    <template v-if="$slots.header" #header="slotProps">
      <slot name="header" v-bind="slotProps" />
    </template>
    <template v-if="$slots.footer" #footer="slotProps">
      <slot name="footer" v-bind="slotProps" />
    </template>
    <template v-if="$slots.option" #option="slotProps">
      <slot name="option" v-bind="slotProps" />
    </template>
    <template v-if="$slots.optiongroup" #optiongroup="slotProps">
      <slot name="optiongroup" v-bind="slotProps" />
    </template>
    <template v-if="$slots.emptyfilter">
      <slot name="emptyfilter" />
    </template>
    <template v-if="$slots.empty">
      <slot name="empty" />
    </template>
    <template v-if="$slots.loadingicon" #loadingicon="slotProps">
      <slot name="loadingicon" v-bind="slotProps" />
    </template>
    <template v-if="$slots.dropdownicon" #dropdownicon="slotProps">
      <slot name="dropdownicon" v-bind="slotProps" />
    </template>
    <template v-if="$slots.clearicon" #clearicon="slotProps">
      <slot name="clearicon" v-bind="slotProps" />
    </template>
  </Select>
</template>

<script lang="ts">
import { defineComponent, type PropType } from "vue";
import Select from "primevue/select";

export default defineComponent({
  name: "PtSelect",
  // eslint-disable-next-line vue/no-reserved-component-names
  components: { Select },
  props: {
    modelValue: {
      type: [String, Number, Object, Array] as PropType<any>,
      default: undefined,
    },
    options: {
      type: Array as PropType<any[]>,
      default: () => [],
    },
    placeholder: {
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
    filter: {
      type: Boolean,
      default: false,
    },
    filterMatchMode: {
      type: String,
      default: "contains",
      validator: (val: string) =>
        ["contains", "startsWith", "endsWith", "equals", "notEquals", "lt", "lte", "gt", "gte"].includes(
          val,
        ),
    },
    filterFields: {
      type: Array as PropType<string[]>,
      default: undefined,
    },
    editable: {
      type: Boolean,
      default: false,
    },
    highlightOnSelect: {
      type: Boolean,
      default: true,
    },
    showClear: {
      type: Boolean,
      default: false,
    },
    resetFilterOnHide: {
      type: Boolean,
      default: false,
    },
    scrollHeight: {
      type: String,
      default: "200px",
    },
    autoFilterFocus: {
      type: Boolean,
      default: true,
    },
    selectOnFocus: {
      type: Boolean,
      default: false,
    },
    focusOnHover: {
      type: Boolean,
      default: true,
    },
    name: {
      type: String,
      default: undefined,
    },
    inputId: {
      type: String,
      default: undefined,
    },
    dataKey: {
      type: String,
      default: undefined,
    },
    ariaLabel: {
      type: String,
      default: undefined,
    },
    ariaLabelledby: {
      type: String,
      default: undefined,
    },
    loading: {
      type: Boolean,
      default: false,
    },
    loadingIcon: {
      type: String,
      default: undefined,
    },
    dropdownIcon: {
      type: String,
      default: undefined,
    },
    appendTo: {
      type: [String, Object] as PropType<string | HTMLElement>,
      default: "body",
    },
  },
  emits: [
    "update:modelValue",
    "focus",
    "blur",
    "show",
    "hide",
    "filter",
  ],
  methods: {
    onUpdateModelValue(value: any) {
      this.$emit("update:modelValue", value);
    },
    onFocus(event: FocusEvent) {
      this.$emit("focus", event);
    },
    onBlur(event: FocusEvent) {
      this.$emit("blur", event);
    },
    onShow() {
      this.$emit("show");
    },
    onHide() {
      this.$emit("hide");
    },
    onFilter(event: any) {
      this.$emit("filter", event);
    },
  },
});
</script>

<style lang="scss">
// Root element styles
.p-select {
  width: 100%;

  // Input field styles - Default state
  .p-select-label {
    background: var(--colors-white);
    border: 1px solid var(--colors-gray-300);
    border-radius: 6px;
    color: var(--colors-gray-800);
    font-size: 14px;
    font-weight: 400;
    line-height: normal;
    padding: 10px;
    transition: border-color 0.2s, box-shadow 0.2s;

    // Placeholder text
    &::placeholder {
      color: var(--colors-gray-500);
    }

    // Hover state
    &:hover:not(:disabled) {
      border-color: var(--colors-gray-300);
    }

    // Focus state - Blue border with shadow
    &:focus,
    &:focus-visible {
      border-color: var(--colors-blue-500);
      box-shadow: 0px 0px 0px 2.8px var(--colors-blue-100);
      outline: none;
    }

    // Invalid state - Red border
    &.p-invalid,
    &.p-select-invalid {
      border-color: var(--colors-red-500);
    }

    // Disabled state
    &:disabled {
      background: var(--colors-white);
      border-color: var(--colors-gray-200);
      color: var(--colors-gray-400);
      cursor: not-allowed;
      opacity: 0.6;
    }
  }

  // Dropdown icon
  .p-select-dropdown-icon {
    color: var(--colors-gray-500);
    width: 14px;
    height: 14px;

    // Disabled icon color
    .p-select-label:disabled ~ & {
      color: var(--colors-gray-300);
    }
  }

  // Clear icon
  .p-select-clear-icon {
    color: var(--colors-gray-500);
  }

  // Loading icon
  .p-select-loading-icon {
    color: var(--colors-gray-500);
  }
}

// Overlay (dropdown panel) styles
.p-select-overlay {
  background: var(--colors-white);
  border-radius: 6px;
  box-shadow: 0px 2px 12px 0px rgba(0, 0, 0, 0.1);
  margin-top: 4px;
  z-index: 1200;

  // Header (filter input container)
  .p-select-header {
    background: var(--colors-gray-50);
    border-bottom: 1px solid var(--colors-gray-200);
    border-radius: 6px 6px 0 0;
    padding: 10px 17px;

    // Filter input
    .p-select-filter {
      background: var(--colors-white);
      border: 1px solid var(--colors-gray-300);
      border-radius: 6px;
      padding: 10px;
      width: 100%;

      &:focus {
        border-color: var(--colors-blue-500);
        box-shadow: 0px 0px 0px 2.8px var(--colors-blue-100);
        outline: none;
      }
    }
  }

  // List container
  .p-select-list-container {
    padding: 10px 0;
  }

  // List
  .p-select-list {
    padding: 0;
  }

  // Option styles
  .p-select-option {
    color: var(--colors-gray-800);
    font-size: 14px;
    padding: 10px 17px;
    transition: background-color 0.2s, color 0.2s;

    // Hover state
    &:hover:not(.p-disabled):not(.p-select-option-selected) {
      background: var(--colors-gray-100);
      color: var(--colors-gray-800);
    }

    // Focus state
    &.p-focus:not(.p-disabled) {
      background: var(--colors-gray-100);
      color: var(--colors-gray-800);
    }

    // Selected state
    &.p-select-option-selected {
      background: var(--colors-gray-50);
      color: var(--colors-blue-500);

      // Selected + Focus state
      &.p-focus {
        background: var(--colors-gray-50);
        color: var(--colors-blue-500);
      }
    }

    // Disabled state
    &.p-disabled {
      color: var(--colors-gray-400);
      cursor: not-allowed;
      opacity: 0.6;
    }
  }

  // Option group label
  .p-select-option-group-label {
    background: var(--colors-gray-50);
    color: var(--colors-gray-800);
    font-weight: 600;
    padding: 10px 17px;
  }

  // Empty message
  .p-select-empty-message {
    color: var(--colors-gray-500);
    padding: 10px 17px;
  }
}

</style>
