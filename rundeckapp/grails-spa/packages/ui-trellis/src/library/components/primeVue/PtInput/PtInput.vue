<template>
  <div class="pt-input-wrapper">
    <label
      v-if="label"
      :for="inputId"
      class="text-heading--sm pt-form-label"
      data-testid="pt-input-label"
    >
      {{ label }}
    </label>

    <p
      v-if="helpText"
      class="text-body--sm pt-input__help"
      data-testid="pt-input-help"
    >
      {{ helpText }}
    </p>

    <Textarea
      v-if="multiline"
      :id="inputId"
      v-model="internalValue"
      data-testid="pt-input-textarea"
      class="pt-input__field"
      :rows="rows"
      :placeholder="placeholder"
      :disabled="disabled"
      :invalid="invalid"
      :name="name"
      :readonly="readonly"
      :maxlength="maxlength"
      :aria-label="ariaLabel"
      :aria-labelledby="ariaLabelledby"
      @focus="onFocus"
      @blur="onBlur"
      @input="onInput"
    />

    <IconField
      v-else-if="leftIcon || rightIcon"
      class="pt-input__field"
      data-testid="pt-input-icon-container"
    >
      <InputIcon v-if="leftIcon" :class="leftIcon" />
      <InputText
        :id="inputId"
        v-model="internalValue"
        :placeholder="placeholder"
        :disabled="disabled"
        :invalid="invalid"
        :name="name"
        :readonly="readonly"
        :maxlength="maxlength"
        :autocomplete="autocomplete"
        :type="type"
        :aria-label="ariaLabel"
        :aria-labelledby="ariaLabelledby"
        @focus="onFocus"
        @blur="onBlur"
        @input="onInput"
      />
      <InputIcon v-if="rightIcon" :class="rightIcon" />
    </IconField>

    <InputText
      v-else
      :id="inputId"
      v-model="internalValue"
      data-testid="pt-input-field"
      class="pt-input__field"
      :placeholder="placeholder"
      :disabled="disabled"
      :invalid="invalid"
      :name="name"
      :readonly="readonly"
      :maxlength="maxlength"
      :autocomplete="autocomplete"
      :type="type"
      :aria-label="ariaLabel"
      :aria-labelledby="ariaLabelledby"
      @focus="onFocus"
      @blur="onBlur"
      @input="onInput"
    />

    <p
      v-if="invalid && errorText"
      class="text-body--sm pt-input__error"
      data-testid="pt-input-error"
    >
      {{ errorText }}
    </p>
  </div>
</template>

<script lang="ts">
import { defineComponent, type PropType } from "vue";
import InputText from "primevue/inputtext";
import Textarea from "primevue/textarea";
import IconField from "primevue/iconfield";
import InputIcon from "primevue/inputicon";

export default defineComponent({
  name: "PtInput",
  components: {
    InputText,
    // eslint-disable-next-line vue/no-reserved-component-names
    Textarea,
    IconField,
    InputIcon,
  },
  props: {
    modelValue: {
      type: [String, Number] as PropType<string | number | null>,
      default: "",
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
    name: {
      type: String,
      default: undefined,
    },
    label: {
      type: String,
      default: undefined,
    },
    helpText: {
      type: String,
      default: undefined,
    },
    errorText: {
      type: String,
      default: undefined,
    },
    leftIcon: {
      type: String,
      default: undefined,
    },
    rightIcon: {
      type: String,
      default: undefined,
    },
    inputId: {
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
    readonly: {
      type: Boolean,
      default: false,
    },
    maxlength: {
      type: Number,
      default: undefined,
    },
    autocomplete: {
      type: String,
      default: undefined,
    },
    type: {
      type: String as PropType<string>,
      default: "text",
    },
    /** Renders a multi-line textarea instead of a single-line input; icons are ignored. */
    multiline: {
      type: Boolean,
      default: false,
    },
    /** Number of visible text lines when `multiline` is set. */
    rows: {
      type: Number,
      default: 3,
    },
  },
  emits: ["update:modelValue", "focus", "blur", "input"],
  computed: {
    internalValue: {
      get(): string | number | null {
        return this.modelValue;
      },
      set(value: string | number | null) {
        this.$emit("update:modelValue", value);
      },
    },
  },
  methods: {
    onFocus(event: FocusEvent) {
      this.$emit("focus", event);
    },
    onBlur(event: FocusEvent) {
      this.$emit("blur", event);
    },
    onInput(event: Event) {
      this.$emit("input", event);
    },
  },
});
</script>

<style lang="scss">
@import "../_form-inputs.scss";

.pt-input-wrapper {
  display: flex;
  flex-direction: column;
  width: 100%;
}

.pt-input__help {
  margin-top: 0;
  margin-bottom: var(--space-1);
}

.pt-input__error {
  margin-top: var(--space-1);
  margin-bottom: 0;
  color: var(--colors-red-500);
}

// IconField container styles
.p-iconfield {
  width: 100%;
  display: flex;
  align-items: center;
  position: relative;

  .p-inputtext {
    // When icons present, add padding for icon space (icon 14px + gap 10.5px + padding 10.5px = 35px)
    padding-left: calc(var(--sizes-6) + var(--sizes-4) + var(--sizes-4));
    padding-right: calc(var(--sizes-6) + var(--sizes-4) + var(--sizes-4));
  }

  // When only left icon
  &:has(.p-inputicon:first-child):not(:has(.p-inputicon:last-child)) {
    .p-inputtext {
      padding-right: var(--sizes-4);
    }
  }

  // When only right icon
  &:has(.p-inputicon:last-child):not(:has(.p-inputicon:first-child)) {
    .p-inputtext {
      padding-left: var(--sizes-4);
    }
  }
}

// InputIcon styles
.p-inputicon {
  position: absolute;
  top: 50%;
  transform: translateY(-25%);
  color: var(--colors-gray-500);
  width: var(--sizes-6);
  height: var(--sizes-6);
  font-size: 14px;
  z-index: 1;
  display: flex;
  align-items: center;
  justify-content: center;

  // Left icon positioning
  &:first-child {
    left: var(--sizes-4);
  }

  // Right icon positioning
  &:last-child:not(:first-child) {
    right: var(--sizes-4);
    left: auto;
  }

  // Only one icon (right position)
  &:only-child {
    right: var(--sizes-4);
    left: auto;
  }
}

// Disabled icon state
.p-iconfield:has(.p-inputtext:disabled) .p-inputicon {
  color: var(--colors-gray-300-original);
}

// InputText and Textarea styles (Figma input: 10.5px padding, 6px radius)
.p-inputtext,
.p-textarea {
  width: 100%;
  @include form-input-base;
  padding: var(--sizes-4);
  font-family: Inter, var(--fonts-body2);
  font-size: 14px;
  font-weight: var(--fontWeights-regular);
  line-height: normal;
  color: var(--colors-gray-800);
  background: var(--colors-white);

  @include form-input-placeholder;

  // Hover state
  &:hover:not(:focus):not(:disabled):not(.p-invalid) {
    @include form-input-hover;
  }

  // Focus state
  &:focus {
    @include form-input-focus;
  }

  // Invalid state
  &.p-invalid {
    @include form-input-invalid;
  }

  // Disabled state
  &:disabled {
    @include form-input-disabled;
    background: var(--colors-gray-50);
    color: var(--colors-gray-500);
    cursor: not-allowed;
  }
}

// The Figma textarea has a fixed size and no resize handle
.p-textarea {
  resize: none;
}
</style>
