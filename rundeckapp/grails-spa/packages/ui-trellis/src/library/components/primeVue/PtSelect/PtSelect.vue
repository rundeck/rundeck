<template>
  <div class="pt-select-wrapper">
    <label
      v-if="label"
      :for="inputId"
      class="text-heading--sm pt-form-label"
    >
      {{ label }}
    </label>
    <Select
      v-model="internalValue"
      :options="options"
      :option-label="optionLabel"
      :option-value="optionValue"
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
      :auto-filter-focus="false"
      :select-on-focus="false"
      :focus-on-hover="false"
      :name="name"
      :input-id="inputId"
      :data-key="dataKey"
      :aria-label="ariaLabel"
      :aria-labelledby="ariaLabelledby"
      :loading="loading"
      :loading-icon="loadingIcon"
      :dropdown-icon="dropdownIcon"
      :append-to="appendTo"
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
    <p v-if="invalid && errorText" class="text-body--sm pt-select__error">
      {{ errorText }}
    </p>
  </div>
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
    optionLabel: {
      type: [String, Function] as PropType<string | ((option: any) => string)>,
      default: undefined,
    },
    optionValue: {
      type: [String, Function] as PropType<string | ((option: any) => any)>,
      default: undefined,
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
    errorText: {
      type: String,
      default: undefined,
    },
    label: {
      type: String,
      default: undefined,
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
    debounceMs: {
      type: Number,
      default: 0,
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
  data() {
    return {
      debounceTimer: null as ReturnType<typeof setTimeout> | null,
    };
  },
  computed: {
    internalValue: {
      get(): any {
        return this.modelValue;
      },
      set(value: any) {
        if (this.debounceMs > 0) {
          // Debounce the update
          if (this.debounceTimer) {
            clearTimeout(this.debounceTimer);
          }
          this.debounceTimer = setTimeout(() => {
            this.$emit("update:modelValue", value);
            this.debounceTimer = null;
          }, this.debounceMs);
        } else {
          // Emit immediately if no debounce
          this.$emit("update:modelValue", value);
        }
      },
    },
  },
  beforeUnmount() {
    if (this.debounceTimer) {
      clearTimeout(this.debounceTimer);
    }
  },
  methods: {
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
@import "../_form-inputs.scss";

.pt-select-wrapper {
  display: flex;
  flex-direction: column;
  width: 100%;
}

.pt-select__error {
  margin-top: var(--space-1);
  margin-bottom: 0;
  color: var(--colors-red-500);
}

.p-select {
  width: 100%;
  @include form-input-base;
  overflow: visible;
  position: relative;

  // Focus state - Blue border with shadow
  &:focus,
  &:focus-visible,
  &:not(.p-disabled):hover,
  &.p-inputwrapper-focus {
    @include form-input-focus;
  }

  // Invalid state - Red border
  &.p-invalid,
  &.p-select-invalid {
    @include form-input-invalid;
  }

  // Disabled state
  &.p-disabled {
    @include form-input-disabled;
  }

  // Input field styles - Default state
  .p-select-label {
    background: transparent;
    border: none;
    border-radius: 0;
    color: var(--colors-gray-800);
    font-size: 14px;
    font-weight: 400;
    line-height: normal;
    padding: 10px;
    transition: none;
    display: block;
    width: 100%;

    // Placeholder text
    &::placeholder {
      color: var(--colors-gray-500);
    }
  }

  // Dropdown trigger button (contains the dropdown icon)
  .p-select-dropdown {
    background: transparent;
    border: none;
    padding: 0;
    margin: 0;
    display: flex;
    align-items: center;
    justify-content: center;
    outline: none;
    box-shadow: none;

    &:focus,
    &:focus-visible,
    &:active,
    &:hover {
      background: transparent;
      border: none;
      outline: none;
      box-shadow: none;
    }
  }

  // Dropdown icon
  .p-select-dropdown-icon {
    color: var(--colors-gray-500);
    width: 14px;
    height: 14px;

    // Disabled icon color
    .p-select-label:disabled ~ & {
      color: var(--colors-gray-300-original);
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
      border: 1px solid var(--colors-gray-300-original);
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
      background: var(--colors-cardNumber);
      color: var(--colors-gray-800);
    }

    // Focus state
    &.p-focus:not(.p-disabled) {
      background: var(--colors-cardNumber);
      color: var(--colors-gray-800);
    }

    // Selected state
    &.p-select-option-selected {
      background: var(--colors-blue-50);
      color: var(--colors-blue-500);

      // Selected + Focus state
      &.p-focus {
        background: var(--colors-blue-50);
        color: var(--colors-blue-500);
      }

      // Ensure nested elements also get blue color when selected
      * {
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
